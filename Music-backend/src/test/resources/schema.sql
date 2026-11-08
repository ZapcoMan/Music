-- H2 测试数据库表结构初始化
-- 基于 MySQL 语法,使用 H2 兼容模式

-- 先清场，再建表与种数据。
--
-- 数据源 URL 带 DB_CLOSE_DELAY=-1，内存库的生命周期跟着 JVM 而不是跟着 Spring 上下文，
-- 所以只要有一个测试类的上下文配置与别人不同（缓存 key 变了），Spring 就会新建第二个上下文，
-- 而这份脚本会在那块还留着第一批数据的库上再跑一遍。
-- 建表语句是 CREATE TABLE IF NOT EXISTS，第二次跑静默跳过；
-- 但下面的 INSERT 是无条件的，于是：
--   · admin.name / consumer.username 有唯一约束（V12 同步过来的），
--     重复插入当场报 23505，上下文启动失败，几百条用例级联变 ERROR；
--   · 没有唯一约束的 singer / song 等表更隐蔽——它们安静地插入双份，
--     于是"查出 3 首"的断言变成 6 首。
-- 实际触发过一次：给指标装配测试加 @AutoConfigureObservability 之后，
-- 全量从 277 绿变成 242 个 error，报错停在 INSERT INTO `admin` 那一行。
--
-- 用 DROP ALL OBJECTS 而不是逐表 DROP：这是 H2 专用语句，
-- 目的是让每个上下文都从一块干净的库开始，新增表时不必记得回来补一行。
DROP ALL OBJECTS;

-- 管理员表
CREATE TABLE IF NOT EXISTS `admin` (
    `id` INT PRIMARY KEY AUTO_INCREMENT,
    `name` VARCHAR(255) NOT NULL,
    `password` VARCHAR(255) NOT NULL,
    CONSTRAINT `uk_admin_name` UNIQUE (`name`)
);

-- 歌手表
CREATE TABLE IF NOT EXISTS `singer` (
    `id` INT PRIMARY KEY AUTO_INCREMENT,
    `name` VARCHAR(255) NOT NULL,
    `sex` TINYINT,
    `pic` VARCHAR(255),
    `birth` DATE,
    `location` VARCHAR(255),
    `tags` VARCHAR(500),
    `introduction` TEXT
);

-- 歌曲表
CREATE TABLE IF NOT EXISTS `song` (
    `id` INT PRIMARY KEY AUTO_INCREMENT,
    `singer_id` INT,
    `name` VARCHAR(255) NOT NULL,
    `introduction` TEXT,
    `tags` VARCHAR(500),
    `pic` VARCHAR(255),
    `lyric` TEXT,
    `url` VARCHAR(255),
    `create_time` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    `update_time` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- 歌单表
CREATE TABLE IF NOT EXISTS `song_list` (
    `id` INT PRIMARY KEY AUTO_INCREMENT,
    `title` VARCHAR(255) NOT NULL,
    `pic` VARCHAR(255),
    `introduction` TEXT,
    `style` VARCHAR(100),
    `create_time` TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 歌单歌曲关联表
CREATE TABLE IF NOT EXISTS `list_song` (
    `id` INT PRIMARY KEY AUTO_INCREMENT,
    `song_id` INT NOT NULL,
    `song_list_id` INT NOT NULL,
    CONSTRAINT `uk_list_song_pair` UNIQUE (`song_list_id`, `song_id`)
);

-- 用户表
CREATE TABLE IF NOT EXISTS `consumer` (
    `id` INT PRIMARY KEY AUTO_INCREMENT,
    `username` VARCHAR(255) NOT NULL,
    `password` VARCHAR(255) NOT NULL,
    `sex` TINYINT,
    `phone_num` VARCHAR(50),
    `email` VARCHAR(255),
    `birth` DATE,
    `introduction` TEXT,
    `location` VARCHAR(255),
    `avator` VARCHAR(255),
    `create_time` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    `update_time` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT `uk_consumer_username` UNIQUE (`username`)
);

-- 收藏表
CREATE TABLE IF NOT EXISTS `collect` (
    `id` INT PRIMARY KEY AUTO_INCREMENT,
    `user_id` INT NOT NULL,
    `type` TINYINT NOT NULL,
    `song_id` INT,
    `song_list_id` INT,
    `create_time` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT `uk_collect_user_song` UNIQUE (`user_id`, `song_id`),
    CONSTRAINT `uk_collect_user_list` UNIQUE (`user_id`, `song_list_id`)
);

-- 评论表
CREATE TABLE IF NOT EXISTS `comment` (
    `id` INT PRIMARY KEY AUTO_INCREMENT,
    `user_id` INT NOT NULL,
    `song_id` INT,
    `song_list_id` INT,
    `content` TEXT,
    `create_time` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    `type` TINYINT NOT NULL,
    `up` INT DEFAULT 0
);

-- 评价表
CREATE TABLE IF NOT EXISTS `ranks` (
    `id` INT PRIMARY KEY AUTO_INCREMENT,
    `song_list_id` INT NOT NULL,
    `consumer_id` INT NOT NULL,
    `score` INT,
    CONSTRAINT `uk_ranks_list_consumer` UNIQUE (`song_list_id`, `consumer_id`)
);
-- 与真实 DDL 对齐的两处（V1__InitDateBase.sql 的 ranks 定义）：
--   · score 原本写成 NOT NULL，而真实库是 NULL DEFAULT NULL。
--     正因为测试库不允许空分，"空分行被计入平均分母"这个缺陷在测试里永远复现不了。
--   · 原本没有 UNIQUE(song_list_id, consumer_id)，真实库有。
--     缺它的话"重复评分应改分而不是插入第二行"这条路径根本撞不到唯一键，
--     upsert 与并发冲突分支都测不到。

-- user_ratings 已下线（V13）：它全仓无 PO / 无 Mapper / 无引用，与 ranks 语义重复，
-- 历史行迁入 ranks 后 DROP。测试库原先补建过这张表，现在跟着真实结构一起删掉——
-- 留着的话测试库比真库多一张表，任何"扫全库表清单"的用例都会在测试里通过、在线上不成立。

-- 标签字典表
CREATE TABLE IF NOT EXISTS `tag` (
    `id` INT PRIMARY KEY AUTO_INCREMENT,
    `name` VARCHAR(50) NOT NULL,
    `type` TINYINT DEFAULT 0,
    `create_time` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    `update_time` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- 歌曲标签关联表
CREATE TABLE IF NOT EXISTS `song_tag` (
    `id` INT PRIMARY KEY AUTO_INCREMENT,
    `song_id` INT NOT NULL,
    `tag_id` INT NOT NULL,
    `create_time` TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 歌手标签关联表
CREATE TABLE IF NOT EXISTS `singer_tag` (
    `id` INT PRIMARY KEY AUTO_INCREMENT,
    `singer_id` INT NOT NULL,
    `tag_id` INT NOT NULL,
    `create_time` TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 搜索历史表
CREATE TABLE IF NOT EXISTS `search_history` (
    `id` INT PRIMARY KEY AUTO_INCREMENT,
    `user_id` INT NOT NULL,
    `keywords` VARCHAR(255) NOT NULL,
    `search_type` TINYINT DEFAULT 0,
    `create_time` TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 操作审计日志表（V5 迁移新建，测试 schema 需同步，否则审计接线后的写入在 H2 必然失败）
CREATE TABLE IF NOT EXISTS `operation_log` (
    `id` INT PRIMARY KEY AUTO_INCREMENT,
    `user_id` INT,
    `username` VARCHAR(50),
    `user_type` VARCHAR(20),
    `operation_type` VARCHAR(20),
    `module` VARCHAR(50),
    `description` VARCHAR(255),
    `request_url` VARCHAR(255),
    `request_method` VARCHAR(10),
    `request_params` TEXT,
    `response_result` TEXT,
    `ip_address` VARCHAR(50),
    `duration` BIGINT,
    `status` VARCHAR(20),
    `error_msg` VARCHAR(500),
    `create_time` TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 插入测试数据
INSERT INTO `admin` (`name`, `password`) VALUES 
('admin', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy');

INSERT INTO `singer` (`name`, `sex`, `pic`, `birth`, `location`, `introduction`) VALUES 
('周杰伦', 1, '/img/singerPic/test1.jpg', '1979-01-18', '台湾', '华语流行音乐男歌手'),
('陈奕迅', 1, '/img/singerPic/test2.jpg', '1974-07-27', '香港', '华语流行音乐男歌手'),
('王菲', 0, '/img/singerPic/test3.jpg', '1969-08-08', '北京', '华语流行音乐女歌手');

INSERT INTO `song` (`singer_id`, `name`, `introduction`, `pic`, `url`) VALUES 
(1, '七里香', '经典歌曲', '/img/songPic/test1.jpg', '/song/test1.mp3'),
(1, '青花瓷', '经典歌曲', '/img/songPic/test2.jpg', '/song/test2.mp3'),
(2, '十年', '经典歌曲', '/img/songPic/test3.jpg', '/song/test3.mp3');

INSERT INTO `song_list` (`title`, `pic`, `introduction`, `style`) VALUES 
('华语经典', '/img/songListPic/test1.jpg', '精选华语经典歌曲', '华语'),
('流行金曲', '/img/songListPic/test2.jpg', '最新流行金曲', '流行');

INSERT INTO `consumer` (`username`, `password`, `sex`, `phone_num`, `email`, `avator`) VALUES 
('testuser', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 1, '13800138000', 'test@example.com', '/userImages/test.jpg');

-- 标签字典：id 显式指定，与真实库 tag 表的编号语义保持一致（0风格 1情感 3语言）
INSERT INTO `tag` (`id`, `name`, `type`) VALUES
(1, '流行', 0), (2, '摇滚', 0), (3, '民谣', 0),
(6, '抒情', 1), (7, '伤感', 1),
(16, '中文', 3), (17, '英文', 3);

INSERT INTO `song_tag` (`song_id`, `tag_id`) VALUES
(1, 1), (1, 6), (1, 16),      -- 七里香
(2, 1), (2, 3), (2, 16),      -- 青花瓷
(3, 1), (3, 7), (3, 16);      -- 十年

INSERT INTO `singer_tag` (`singer_id`, `tag_id`) VALUES
(1, 1), (1, 16),              -- 周杰伦
(2, 1), (2, 16),              -- 陈奕迅
(3, 1), (3, 16);              -- 王菲

-- 逗号串与中间表保持一致。真实库经 V6 已两侧对齐，
-- 测试库若留空会让 Base_Column_List 读出的 tags 与 song_tag 不一致，掩盖问题。
UPDATE `song` SET `tags` = '流行,抒情,中文' WHERE `id` = 1;
UPDATE `song` SET `tags` = '流行,民谣,中文' WHERE `id` = 2;
UPDATE `song` SET `tags` = '流行,伤感,中文' WHERE `id` = 3;
UPDATE `singer` SET `tags` = '流行,中文' WHERE `id` IN (1, 2, 3);

INSERT INTO `search_history` (`user_id`, `keywords`, `search_type`) VALUES
(1, '七里香', 0), (1, '青花瓷', 0), (1, '华语经典', 1);

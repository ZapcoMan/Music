-- H2 测试数据库表结构初始化
-- 基于 MySQL 语法,使用 H2 兼容模式

-- 管理员表
CREATE TABLE IF NOT EXISTS `admin` (
    `id` INT PRIMARY KEY AUTO_INCREMENT,
    `name` VARCHAR(255) NOT NULL,
    `password` VARCHAR(255) NOT NULL
);

-- 歌手表
CREATE TABLE IF NOT EXISTS `singer` (
    `id` INT PRIMARY KEY AUTO_INCREMENT,
    `name` VARCHAR(255) NOT NULL,
    `sex` TINYINT,
    `pic` VARCHAR(255),
    `birth` DATE,
    `location` VARCHAR(255),
    `introduction` TEXT
);

-- 歌曲表
CREATE TABLE IF NOT EXISTS `song` (
    `id` INT PRIMARY KEY AUTO_INCREMENT,
    `singer_id` INT,
    `name` VARCHAR(255) NOT NULL,
    `introduction` TEXT,
    `pic` VARCHAR(255),
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
    `song_list_id` INT NOT NULL
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
    `update_time` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- 收藏表
CREATE TABLE IF NOT EXISTS `collect` (
    `id` INT PRIMARY KEY AUTO_INCREMENT,
    `user_id` INT NOT NULL,
    `type` TINYINT NOT NULL,
    `song_id` INT,
    `song_list_id` INT,
    `create_time` TIMESTAMP DEFAULT CURRENT_TIMESTAMP
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

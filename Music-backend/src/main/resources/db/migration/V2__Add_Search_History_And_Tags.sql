-- 创建用户搜索历史表
CREATE TABLE IF NOT EXISTS `search_history` (
    `id` INT(11) NOT NULL AUTO_INCREMENT COMMENT '主键',
    `user_id` INT(11) NOT NULL COMMENT '用户ID',
    `keywords` VARCHAR(255) NOT NULL COMMENT '搜索关键词',
    `search_type` TINYINT(1) DEFAULT 0 COMMENT '搜索类型（0歌曲1歌手2歌单）',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '搜索时间',
    PRIMARY KEY (`id`) USING BTREE,
    INDEX `idx_user_id` (`user_id`) USING BTREE,
    INDEX `idx_create_time` (`create_time`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='用户搜索历史';

-- 为song表添加tags字段
-- 注意：不使用 ADD COLUMN IF NOT EXISTS。该写法是 MariaDB 扩展，MySQL 不支持，
-- 会让本迁移在 MySQL 上直接抛 ERROR 1064 并中断后续所有迁移。
-- 重复执行的安全性由 Flyway 的版本化机制保证，无需在此做存在性判断。
ALTER TABLE `song` ADD COLUMN `tags` VARCHAR(500) DEFAULT NULL COMMENT '歌曲标签（逗号分隔，如：流行,抒情,伤感）' AFTER `introduction`;

-- 为singer表添加tags字段（同上，MySQL 兼容写法）
ALTER TABLE `singer` ADD COLUMN `tags` VARCHAR(500) DEFAULT NULL COMMENT '歌手标签（逗号分隔，如：男歌手,流行,实力派）' AFTER `location`;

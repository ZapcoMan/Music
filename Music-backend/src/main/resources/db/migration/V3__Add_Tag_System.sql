-- =====================================================
-- 标签系统数据库迁移脚本
-- 解决模糊匹配不准确和性能低下的问题
-- =====================================================

-- 1. 创建标签表
CREATE TABLE IF NOT EXISTS `tag` (
    `id` INT(11) NOT NULL AUTO_INCREMENT COMMENT '标签ID',
    `name` VARCHAR(50) NOT NULL COMMENT '标签名称（如：流行、抒情、摇滚）',
    `type` TINYINT(1) DEFAULT 0 COMMENT '标签类型（0风格1情感2场景3语言4其他）',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`) USING BTREE,
    UNIQUE KEY `uk_name` (`name`) USING BTREE,
    KEY `idx_type` (`type`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='标签字典表';

-- 2. 创建歌曲-标签关联表（中间表）
CREATE TABLE IF NOT EXISTS `song_tag` (
    `id` INT(11) NOT NULL AUTO_INCREMENT COMMENT '主键',
    `song_id` INT(11) NOT NULL COMMENT '歌曲ID',
    `tag_id` INT(11) NOT NULL COMMENT '标签ID',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '关联时间',
    PRIMARY KEY (`id`) USING BTREE,
    UNIQUE KEY `uk_song_tag` (`song_id`, `tag_id`) USING BTREE,
    KEY `idx_tag_id` (`tag_id`) USING BTREE,
    KEY `idx_song_id` (`song_id`) USING BTREE,
    CONSTRAINT `fk_song_tag_song` FOREIGN KEY (`song_id`) REFERENCES `song` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_song_tag_tag` FOREIGN KEY (`tag_id`) REFERENCES `tag` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='歌曲标签关联表';

-- 3. 创建歌手-标签关联表（可选，用于歌手标签召回）
CREATE TABLE IF NOT EXISTS `singer_tag` (
    `id` INT(11) NOT NULL AUTO_INCREMENT COMMENT '主键',
    `singer_id` INT(11) NOT NULL COMMENT '歌手ID',
    `tag_id` INT(11) NOT NULL COMMENT '标签ID',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '关联时间',
    PRIMARY KEY (`id`) USING BTREE,
    UNIQUE KEY `uk_singer_tag` (`singer_id`, `tag_id`) USING BTREE,
    KEY `idx_tag_id` (`tag_id`) USING BTREE,
    KEY `idx_singer_id` (`singer_id`) USING BTREE,
    CONSTRAINT `fk_singer_tag_singer` FOREIGN KEY (`singer_id`) REFERENCES `singer` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_singer_tag_tag` FOREIGN KEY (`tag_id`) REFERENCES `tag` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='歌手标签关联表';

-- 4. 插入基础标签数据（示例）
INSERT INTO `tag` (`name`, `type`) VALUES
('流行', 0), ('摇滚', 0), ('民谣', 0), ('电子', 0), ('嘻哈', 0),
('抒情', 1), ('伤感', 1), ('快乐', 1), ('治愈', 1), ('激情', 1),
('运动', 2), ('睡眠', 2), ('驾驶', 2), ('派对', 2), ('学习', 2),
('中文', 3), ('英文', 3), ('日文', 3), ('韩文', 3), ('粤语', 3)
ON DUPLICATE KEY UPDATE `name`=`name`;

-- 5. 为现有歌曲迁移标签数据（从introduction字段提取）
-- 注意：这只是一个示例，实际应该根据业务逻辑手动或通过AI标注
INSERT INTO `song_tag` (`song_id`, `tag_id`)
SELECT s.id, t.id
FROM song s
JOIN tag t ON s.introduction LIKE CONCAT('%', t.name, '%')
WHERE t.type = 0  -- 只迁移风格标签
ON DUPLICATE KEY UPDATE `song_id`=`song_id`;

-- 6. 添加性能优化索引
ALTER TABLE `song_tag` ADD INDEX `idx_create_time` (`create_time`);
ALTER TABLE `singer_tag` ADD INDEX `idx_create_time` (`create_time`);

-- =====================================================
-- 业务表字符集 utf8 → utf8mb4
--
-- 摸底实测的非 utf8mb4（MySQL 8 中 utf8 即 UTF8MB3 别名）字符列分布：
--   admin 2、comment 1、consumer 7、search_history 1、
--   singer 5、song 6、song_list 4、tag 1，另 flyway_schema_history 5。
-- 建库时的 --character-set-server=utf8mb4 只作用于新表默认值，
-- 而这些表是 V1/V3 用显式 DEFAULT CHARSET=utf8 建的，所以一直没跟上。
--
-- UTF8MB3 存不下 emoji 与部分生僻字（4 字节字符），
-- 用户昵称、歌单标题、评论内容里出现就会被拒绝或截断。
--
-- 长度安全检查：utf8mb4 下单个 VARCHAR(255) 索引占 1020 字节，
-- InnoDB DYNAMIC 上限 3072 字节，现有索引均为单列 varchar(255) 以内
-- 或 INT 组合（uk_song_tag、uk_singer_tag），不超限。
--
-- flyway_schema_history 不动：那是框架自身的表，列里只有版本号与描述，
-- 为它做全表重建换来的收益是零，风险却由 Flyway 自己承担。
-- =====================================================

ALTER TABLE `admin`          CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
ALTER TABLE `consumer`       CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
ALTER TABLE `singer`         CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
ALTER TABLE `song`           CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
ALTER TABLE `song_list`      CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
ALTER TABLE `comment`        CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
ALTER TABLE `tag`            CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
ALTER TABLE `search_history` CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

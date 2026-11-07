-- =====================================================
-- 补齐剩余表的默认字符集
--
-- V9 转换的是"有非 utf8mb4 字符列"的 8 张表，实测后仍有 6 张表的
-- 表级默认排序规则停在 utf8_general_ci：collect、list_song、ranks、
-- song_tag、singer_tag、user_ratings。
-- 它们当前全是 INT 列，所以现在读不出问题 ——
-- 但只要有人给其中任何一张加一个 VARCHAR 列，就会静默落到 UTF8MB3，
-- 而这正是本轮要消除的那类隐患。
--
-- 与 V9 分开发布而不是改写 V9：V9 已应用到 flyway_schema_history，
-- 修改已应用的迁移文件不会让它重跑，只会让 checksum 与线上内容不一致。
-- =====================================================

ALTER TABLE `collect`     CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
ALTER TABLE `list_song`   CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
ALTER TABLE `ranks`       CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
ALTER TABLE `song_tag`    CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
ALTER TABLE `singer_tag`  CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
ALTER TABLE `user_ratings` CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

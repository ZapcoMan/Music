-- ============================================================
-- V12: 为账号与关联关系补唯一约束
--
-- 背景：
--   1) consumer.username 与 admin.name 在 V1 里只有主键，既无索引也无唯一约束。
--      注册走"先查后插"，读己之写之外没有任何兜底，并发注册可以产生两个同名账号；
--      随后 getByUsername 声明返回单对象，会撞出 TooManyResultsException，
--      表现是登录直接 500。日常登录同样是全表扫描。
--   2) collect 与 list_song 也只有主键，防重复完全依赖应用层的 count 检查
--      （CollectController#addCollect 的 existSongId / existSongListId）。
--      V11 里那句 INSERT IGNORE 在没有任何唯一键的 collect 上根本不会触发，
--      属无效防护；双击收藏或并发提交仍能写出重复行。
--
-- 与 V4 的关系：
--   V4 已建 idx_collect_user_song(user_id, song_id) 普通索引，本脚本用同列的唯一
--   索引取代它并删掉该普通索引——两者列完全相同，留着只是冗余。
--   V4 当时以"删除索引属不可逆变更"为由未清理；实际上索引可用 CREATE INDEX
--   随时重建、不触碰数据，所以这里执行。
--   list_song 的 uk_list_song_pair(song_list_id, song_id) 与 V4 的
--   idx_list_song_song_list(song_id, song_list_id) 最左列不同、各服务不同查询路径，
--   两者都保留。
--
-- 历史脏数据（刻意不自动清理）：
--   若现网已存在重名账号或重复收藏，本脚本会直接失败并报 1062。这是有意选择：
--   宁可迁移报错，也不在无人工确认的情况下删除用户数据（与 V8 的处理方向一致）。
--   先执行下面四句定位冲突，人工决定保留哪一条后重跑迁移：
--     SELECT username, COUNT(*) c FROM consumer GROUP BY username HAVING c > 1;
--     SELECT name, COUNT(*) c FROM admin GROUP BY name HAVING c > 1;
--     SELECT user_id, song_id, COUNT(*) c FROM collect
--       WHERE song_id IS NOT NULL GROUP BY user_id, song_id HAVING c > 1;
--     SELECT song_list_id, song_id, COUNT(*) c FROM list_song
--       GROUP BY song_list_id, song_id HAVING c > 1;
--
-- 与测试库的同步：src/test/resources/schema.sql 里的同名约束需一并维护，
-- 否则测试环境永远测不出 1062 分支。两处口径由 #42（收敛 schema 双源）统一。
-- ============================================================

-- ---------- 账号唯一 ----------
ALTER TABLE `consumer` ADD UNIQUE KEY `uk_consumer_username` (`username`);
ALTER TABLE `admin`    ADD UNIQUE KEY `uk_admin_name` (`name`);

-- ---------- 收藏唯一 ----------
-- 收藏行按类型分列存放：type=0 只写 song_id（song_list_id 留 NULL），
-- type=1 只写 song_list_id（song_id 留 NULL），见 CollectController#addCollect。
-- MySQL 的唯一索引不把 NULL 视为相等，因此下面两条各自只约束对应类型的那一侧，
-- 无需把 type 放进索引里。
ALTER TABLE `collect` ADD UNIQUE KEY `uk_collect_user_song` (`user_id`, `song_id`);
ALTER TABLE `collect` ADD UNIQUE KEY `uk_collect_user_list` (`user_id`, `song_list_id`);

DROP INDEX `idx_collect_user_song` ON `collect`;

-- ---------- 歌单曲目唯一 ----------
ALTER TABLE `list_song` ADD UNIQUE KEY `uk_list_song_pair` (`song_list_id`, `song_id`);

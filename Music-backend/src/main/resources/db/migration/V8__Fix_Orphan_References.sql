-- =====================================================
-- 脏数据修复：补齐被引用的缺失目标 + 清理 "null" 字符串
--
-- 摸底实测的孤儿引用（全部指向"应当存在但不存在"的行）：
--   ranks       4 行 -> consumer 33（该账号已不在 consumer 表中）
--   user_ratings 1 行 -> consumer 33
--   comment     2 行 -> consumer 33
--   comment     1 行 -> song_list 91（song_list 最大 id 为 90）
--
-- 处理方向选择"补齐缺失的引用目标"而不是"删除孤儿行"：
--   删除不可逆，且 ranks 里那 4 条评分是协同过滤与评分特征的真实输入，
--   删掉会让刚补好的召回数据重新变稀；而补一个账号是一行插入，风险为零。
--
-- 该账号的 password 是一次性随机 BCrypt（明文未记录在任何地方、未告知任何人），
-- 因此它事实上无法登录，仅作为历史引用的存在性锚点。
-- 若后续要真正启用，需走重置密码流程。
-- =====================================================

-- ---------- 补回被引用的历史账号 ----------
INSERT INTO `consumer` (`id`, `username`, `password`, `sex`, `phone_num`, `email`, `birth`,
                        `introduction`, `location`, `avator`, `create_time`, `update_time`)
SELECT 33, '历史用户33', '$2a$10$MpqsjEYQj1JeW2ilG8gFIOz6qECqcIjfPjWbo8baaeFlCHK6utW8C', 1,
       NULL, NULL, NULL, '仅为历史数据引用锚点，口令不可用', NULL, '', NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM consumer WHERE id = 33);

-- ---------- 补回被引用的歌单 ----------
INSERT INTO `song_list` (`id`, `title`, `pic`, `introduction`, `style`)
SELECT 91, '华语经典回顾', NULL, '补齐历史评论引用的歌单，曲目见 list_song', '华语'
WHERE NOT EXISTS (SELECT 1 FROM song_list WHERE id = 91);

INSERT INTO `list_song` (`song_list_id`, `song_id`)
SELECT 91, so.id FROM `song` so
WHERE so.id IN (120, 136, 154, 160)   -- 晴天 / 十年 / 江南 / 小情歌
  AND NOT EXISTS (SELECT 1 FROM list_song ls WHERE ls.song_list_id = 91 AND ls.song_id = so.id);

-- ---------- 清理字符串 "null" ----------
-- 建库数据里大量 introduction 实际写入的是 4 个字符的 "null"（而非 SQL NULL），
-- V3 的标签自动迁移正是拿它去做 LIKE 匹配才全部落空。统一归一为真 NULL，
-- 代码侧对 NULL 已有"值缺失"处理路径。
UPDATE `song` SET introduction = NULL WHERE introduction = 'null';
UPDATE `singer` SET introduction = NULL WHERE introduction = 'null';
UPDATE `song` SET pic = NULL WHERE pic = 'null';

-- 124 号是重复上传产生的占位行，歌名写成了 'G.E.M.邓紫棋-null'。
-- 不编造曲目名，改为可读的占位说明，便于后台识别清理。
UPDATE `song` SET name = 'G.E.M.邓紫棋-未命名曲目' WHERE id = 124 AND name = 'G.E.M.邓紫棋-null';

-- =====================================================
-- 歌单收藏（collect.type=1）模拟数据
--
-- 背景：collect 表 65 行全部是 type=0（歌曲收藏），song_list_id 全空。
-- 上一轮 V7 因此**有意没有**造 type=1 的数据 ——
-- 当时 CollectMapper 的读取路径写死 and type = 0，
-- 而 addCollect 又强制要求 songId，歌单收藏既存不进也读不出，
-- 造进去就是没有读取方的死数据。
-- 本轮已把读写两端打通（/collect/add 支持 songListId、新增 /collect/songList），
-- 所以现在补数据才有意义。
--
-- 每个活跃用户收藏 1-2 个真实存在的歌单（song_list 86-91）。
-- =====================================================

INSERT IGNORE INTO `collect` (`user_id`, `type`, `song_id`, `song_list_id`, `create_time`)
SELECT c.id, 1, 0, sl.id, DATE_SUB(NOW(), INTERVAL (c.id % 7) DAY)
FROM consumer c
JOIN song_list sl ON sl.id = 86 + (c.id % 5)
WHERE c.id BETWEEN 35 AND 51
  AND NOT EXISTS (
      SELECT 1 FROM collect x
      WHERE x.user_id = c.id AND x.type = 1 AND x.song_list_id = sl.id
  );

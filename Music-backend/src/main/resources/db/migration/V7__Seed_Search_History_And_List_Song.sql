-- =====================================================
-- 行为数据补齐（模拟数据）
--
-- 摸底实测的两处结构性缺口：
-- 1) search_history 0 行。search_history 这一路召回（权重 0.20）读
--    getRecentKeywords -> likeSongOfNameForRecall('%关键词%')，
--    表空则该路恒返回 0 首。
-- 2) song_list 90「华语民谣，因为懂得才有共鸣」在 list_song 中 0 首歌，
--    却有 14 次评分（ranks 中被评次数最多）；song_list 88「欧美流行」只有 1 首，
--    而库里实际有 That Girl / Love Story 等欧美歌。
--    协同过滤（权重 0.40，六路里最高）靠 list_song 把"相似用户听过的歌单"
--    转成候选歌曲，空歌单使这 14 次评分无法产生任何候选。
--
-- 关键词全部取自库内真实歌名，保证 LIKE 能命中；空歌单只补语义相符的歌曲。
-- collect 的 type=1（歌单收藏）不补：CollectMapper.xml 的读取路径写死 and type = 0，
-- 补进去没有任何读取方，属于死数据。
-- =====================================================

-- ---------- 补齐被评过分为空/过少的歌单 ----------
INSERT INTO `list_song` (`song_list_id`, `song_id`)
SELECT 90, so.id FROM `song` so
WHERE so.id IN (120, 126, 130, 147, 154, 158, 160)      -- 晴天/青花瓷/兰亭序/李白/江南/我好想你/小情歌
  AND NOT EXISTS (SELECT 1 FROM list_song ls WHERE ls.song_list_id = 90 AND ls.song_id = so.id);

INSERT INTO `list_song` (`song_list_id`, `song_id`)
SELECT 88, so.id FROM `song` so
WHERE so.id IN (170, 171)                                -- That Girl / Love Story
  AND NOT EXISTS (SELECT 1 FROM list_song ls WHERE ls.song_list_id = 88 AND ls.song_id = so.id);

-- ---------- 搜索历史（模拟）----------
-- search_type：0 歌曲，1 歌单。读取侧不按 type 过滤，仅为语义留痕。
INSERT INTO `search_history` (`user_id`, `keywords`, `search_type`, `create_time`) VALUES
-- user 35
(35, '晴天', 0, DATE_SUB(NOW(), INTERVAL 3 MINUTE)),
(35, '青花瓷', 0, DATE_SUB(NOW(), INTERVAL 26 MINUTE)),
(35, '十年', 0, DATE_SUB(NOW(), INTERVAL 2 HOUR)),
(35, '小情歌', 0, DATE_SUB(NOW(), INTERVAL 1 DAY)),
-- user 36
(36, '光年之外', 0, DATE_SUB(NOW(), INTERVAL 12 MINUTE)),
(36, '泡沫', 0, DATE_SUB(NOW(), INTERVAL 3 HOUR)),
-- user 39
(39, '江南', 0, DATE_SUB(NOW(), INTERVAL 8 MINUTE)),
(39, '曹操', 0, DATE_SUB(NOW(), INTERVAL 40 MINUTE)),
(39, '修炼爱情', 0, DATE_SUB(NOW(), INTERVAL 5 HOUR)),
-- user 40
(40, '七里香', 0, DATE_SUB(NOW(), INTERVAL 5 MINUTE)),
(40, '花海', 0, DATE_SUB(NOW(), INTERVAL 18 MINUTE)),
(40, '兰亭序', 0, DATE_SUB(NOW(), INTERVAL 46 MINUTE)),
(40, '叶惠美', 1, DATE_SUB(NOW(), INTERVAL 9 HOUR)),
-- user 42
(42, '富士山下', 0, DATE_SUB(NOW(), INTERVAL 15 MINUTE)),
(42, '单车', 0, DATE_SUB(NOW(), INTERVAL 1 HOUR)),
(42, '最佳损友', 0, DATE_SUB(NOW(), INTERVAL 4 HOUR)),
-- user 44
(44, '李白', 0, DATE_SUB(NOW(), INTERVAL 7 MINUTE)),
(44, '模特', 0, DATE_SUB(NOW(), INTERVAL 33 MINUTE)),
(44, '年少有为', 0, DATE_SUB(NOW(), INTERVAL 2 HOUR)),
-- user 45
(45, '一路向北', 0, DATE_SUB(NOW(), INTERVAL 2 MINUTE)),
(45, '反方向的钟', 0, DATE_SUB(NOW(), INTERVAL 21 MINUTE)),
(45, '华语伤感女声', 1, DATE_SUB(NOW(), INTERVAL 6 HOUR)),
-- user 46
(46, '红豆', 0, DATE_SUB(NOW(), INTERVAL 9 MINUTE)),
(46, '匆匆那年', 0, DATE_SUB(NOW(), INTERVAL 28 MINUTE)),
(46, '人间', 0, DATE_SUB(NOW(), INTERVAL 3 HOUR)),
-- user 47
(47, '突然好想你', 0, DATE_SUB(NOW(), INTERVAL 4 MINUTE)),
(47, '后来的我们', 0, DATE_SUB(NOW(), INTERVAL 55 MINUTE)),
-- user 48
(48, '不将就', 0, DATE_SUB(NOW(), INTERVAL 17 MINUTE)),
(48, '大城小爱', 0, DATE_SUB(NOW(), INTERVAL 70 MINUTE)),
-- user 49
(49, '步步', 0, DATE_SUB(NOW(), INTERVAL 6 MINUTE)),
(49, '无与伦比的美丽', 0, DATE_SUB(NOW(), INTERVAL 36 MINUTE)),
(49, '华语组合金曲', 1, DATE_SUB(NOW(), INTERVAL 11 HOUR)),
-- user 50
(50, 'Right Now', 0, DATE_SUB(NOW(), INTERVAL 13 MINUTE)),
(50, 'Love Story', 0, DATE_SUB(NOW(), INTERVAL 80 MINUTE)),
-- user 51
(51, 'That Girl', 0, DATE_SUB(NOW(), INTERVAL 23 MINUTE)),
(51, 'Wonderful U', 0, DATE_SUB(NOW(), INTERVAL 110 MINUTE));

-- ============================================================
-- V4: 为推荐与列表查询热路径补建缺失索引
--
-- 背景：V1 建表时 song / list_song / collect / comment 仅有主键；
--       ranks 的唯一索引为 (song_list_id, consumer_id)，其最左列与
--       协同过滤 "WHERE r1.consumer_id = ?" 的过滤方向不匹配。
--       结果是召回、特征统计与热门榜计算全部走全表扫描。
--
-- 说明：tag / song_tag / singer_tag 已在 V3 建好完整索引，此处不再涉及。
-- ============================================================

-- 收藏表：召回按用户取收藏集合；热门榜与特征统计按歌曲聚合收藏数
CREATE INDEX `idx_collect_user_song` ON `collect` (`user_id`, `song_id`);
CREATE INDEX `idx_collect_song` ON `collect` (`song_id`);

-- 歌曲表：按歌手召回其他歌曲（songOfSingerId）
CREATE INDEX `idx_song_singer_id` ON `song` (`singer_id`);

-- 评价表：协同过滤需先按当前用户过滤再按歌单维度自连接
CREATE INDEX `idx_ranks_consumer_list` ON `ranks` (`consumer_id`, `song_list_id`);

-- 歌单-歌曲关联表：歌曲平均分经 list_song 反查所属歌单的评分
-- (song_id, song_list_id) 对该路径构成覆盖索引，避免回表
CREATE INDEX `idx_list_song_song_list` ON `list_song` (`song_id`, `song_list_id`);
CREATE INDEX `idx_list_song_list_id` ON `list_song` (`song_list_id`);

-- 评论表：按歌曲/歌单与类型取评论；按用户清理评论
CREATE INDEX `idx_comment_song_type` ON `comment` (`song_id`, `type`);
CREATE INDEX `idx_comment_user_id` ON `comment` (`user_id`);

-- 搜索历史：getRecentKeywords 为 WHERE user_id = ? ORDER BY create_time DESC，
-- 联合索引可同时完成过滤与排序。V2 的 idx_user_id 由此变为冗余，
-- 但删除索引属于不可逆变更，此处仅新增、不删除，留待确认后再清理。
CREATE INDEX `idx_search_history_user_time` ON `search_history` (`user_id`, `create_time`);

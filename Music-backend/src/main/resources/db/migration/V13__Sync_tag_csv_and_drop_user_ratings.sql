-- =====================================================
-- 标签真值收口 + user_ratings 下线
--
-- 这两件事都属于"数据口径"，不是结构缺失，所以单独一个版本脚本，
-- 不与 V12 的约束混在一起：V12 撞键会直接让迁移失败并保留现场，
-- 而这个脚本里的每一步都应该是可重复执行的收敛动作。
--
-- 口径确认（2026-11-08，与管理端页面与召回链路实际读取的路径一致）：
--   · song_tag / singer_tag 中间表是唯一真值。写入侧已经收敛——
--     SongServiceImpl / SingerServiceImpl 与 PUT /songs/{id}/tags 都是
--     先规范化逗号串、再由 TagService.replaceXxxTags 重建中间表；
--     召回 TagBasedRecall 与管理端标签查询读的都是中间表。
--   · song.tags / singer.tags 逗号串降级为派生展示字段：列表页直接渲染它，
--     省一次 JOIN。它不可手工编辑，值必须等于中间表的 GROUP_CONCAT。
--
-- 漂移是怎么来的：V6 也回填过一次，但条件是 `WHERE tags IS NULL OR tags = ''`，
-- 只填空行、不覆盖旧值；而 PUT /songs/{id}/tags 的初版只重建中间表、不写逗号串
-- （TagController 的注释里承认过这一点）。于是存在两类行：中间表已更新、逗号串仍是旧值。
-- 本脚本按"中间表 → 逗号串"的方向覆盖，正好修掉这两类。
-- =====================================================

-- ---------- 一、以中间表为准重算逗号串 ----------
-- 只处理"中间表有行"的歌曲：没有关联行的歌曲无从推算，保留它现有的逗号串，
-- 宁可留着也不清空——清空等于把只存在于 CSV 里的历史标签删掉。
-- 子查询读的是 song_tag / tag，与被更新的 song 不是同一张表，不会触发 MySQL 1093。
-- 一个前提：GROUP_CONCAT 受 group_concat_max_len 限制（默认 1024 字节）。
-- 当前每首 2–4 个标签、标签名最长 50 字，远不到上限；真要给同一首歌挂上百个标签，
-- 这里会静默截断成半截字符串，那时要么调大该变量，要么让展示侧改读中间表。
UPDATE `song` s
SET s.tags = (
    SELECT GROUP_CONCAT(t.name ORDER BY t.id SEPARATOR ',')
    FROM `song_tag` st
    JOIN `tag` t ON t.id = st.tag_id
    WHERE st.song_id = s.id
)
WHERE EXISTS (
    SELECT 1 FROM `song_tag` st2 WHERE st2.song_id = s.id
);

UPDATE `singer` g
SET g.tags = (
    SELECT GROUP_CONCAT(t.name ORDER BY t.id SEPARATOR ',')
    FROM `singer_tag` gt
    JOIN `tag` t ON t.id = gt.tag_id
    WHERE gt.singer_id = g.id
)
WHERE EXISTS (
    SELECT 1 FROM `singer_tag` gt2 WHERE gt2.singer_id = g.id
);

-- ---------- 二、user_ratings 迁进 ranks 后下线 ----------
-- user_ratings 是 V1 建表时留下的另一套评分存储：全仓没有 PO、没有 Mapper、
-- 没有任何 SQL 或配置引用它，V8 只清理过它的孤儿引用（当时剩 1 行）。
-- 真正在用的评分表是 ranks（RankController 写入、平均分与协同过滤读它），
-- 两张表语义完全重复，留着只会让下一个人重新判断一次。
--
-- 迁移要注意的三处不对称，都写在下面这条 SQL 里：
--   · user_ratings.user_id / song_list_id 可空，ranks 两列都 NOT NULL
--     → 用 JOIN 天然过滤掉空键与已不存在的用户/歌单，不制造新的孤儿行。
--   · ranks 自带 UNIQUE(song_list_id, consumer_id)（V1 就有，不是 V12 加的）
--     → 用 INSERT IGNORE：同键已有评分时保留 ranks 里的现值，
--       不用一张十年没人写的表去覆盖线上正在生效的分数。
--   · user_ratings.timestamp 在 ranks 里没有对应列
--     → 这次迁移会丢掉这些历史评分的时间，只保分值本身。这是已知的取舍，
--       不是遗漏：为 1 行旧数据给 ranks 加一列不值得。
INSERT IGNORE INTO `ranks` (`song_list_id`, `consumer_id`, `score`)
SELECT ur.song_list_id, ur.user_id, ur.score
FROM `user_ratings` ur
JOIN `consumer` c ON c.id = ur.user_id
JOIN `song_list` sl ON sl.id = ur.song_list_id
WHERE ur.score IS NOT NULL;

-- IF EXISTS 而不是裸 DROP：MySQL 的 DDL 不参与事务，一旦这步因为"表已被手工删过"
-- 报 1051（Unknown table），Flyway 会把整个版本记为失败并停在半途——前面已迁进 ranks 的行不会回滚，
-- 修一次反而要人工判断执行到了哪一步。
DROP TABLE IF EXISTS `user_ratings`;

-- ---------- 三、留给运维的核对查询（本脚本不执行） ----------
-- 执行前后各跑一次，确认收敛结果：
--
-- 1) 还有多少歌曲的逗号串与中间表不一致（期望 0）：
--    SELECT s.id, s.tags, x.derived
--      FROM song s
--      LEFT JOIN (SELECT st.song_id, GROUP_CONCAT(t.name ORDER BY t.id SEPARATOR ',') AS derived
--                   FROM song_tag st JOIN tag t ON t.id = st.tag_id GROUP BY st.song_id) x
--        ON x.song_id = s.id
--     WHERE IFNULL(s.tags, '') <> IFNULL(x.derived, '')
--       AND x.derived IS NOT NULL;
--
-- 2) 只存在于 CSV、中间表没有关联的历史歌曲（本脚本刻意不动它们，数量应保持不变）：
--    SELECT id, name, tags FROM song WHERE tags IS NOT NULL AND tags <> ''
--      AND NOT EXISTS (SELECT 1 FROM song_tag st WHERE st.song_id = song.id);
--    这批歌在标签召回里是隐形的。要收口就得反向解析 CSV 并可能往 tag 字典插新标签，
--    那是另一次口径变更，需要单独决策，不在本脚本范围内。
--
-- 3) 迁移后核对评分总量：SELECT (SELECT COUNT(*) FROM ranks) AS now, 原 ranks 行数 + 迁入行数。

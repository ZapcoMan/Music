package com.shanzhu.music.service.ranking;

import com.shanzhu.music.config.RecommendationProperties;
import com.shanzhu.music.entity.dto.SongFeature;
import com.shanzhu.music.entity.po.Collect;
import com.shanzhu.music.entity.po.Rank;
import com.shanzhu.music.entity.po.Song;
import com.shanzhu.music.mapper.CollectMapper;
import com.shanzhu.music.mapper.RankMapper;
import com.shanzhu.music.mapper.SongMapper;
import com.shanzhu.music.service.recall.RecallFusionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 特征工程服务
 * 提取用户、歌曲、交叉特征
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FeatureEngineeringService {
    
    private final CollectMapper collectMapper;
    private final RankMapper rankMapper;
    private final SongMapper songMapper;
    private final RecommendationProperties properties;

    /**
     * 为召回的歌曲列表提取特征（带召回信息）
     *
     * @param userId              用户ID
     * @param recallSongsWithInfo 带召回信息的歌曲列表
     * @return 带特征的歌曲列表
     */
    public List<SongWithFeature> extractFeaturesWithRecallInfo(
            Integer userId, List<RecallFusionService.SongWithRecallInfo> recallSongsWithInfo) {
        log.debug("开始为 {} 首召回歌曲提取特征（带信息）", recallSongsWithInfo.size());
        
        // 1. 预计算用户统计特征
        UserStats userStats = calculateUserStats(userId);
        
        // 2. 一次批量装载候选统计与用户歌手分布。
        //    原先这两类数据都在下面的循环里逐首查询：30 个候选会产生 60 条
        //    歌曲统计 SQL，外加 60 条交叉特征 SQL —— 而后者查的其实是同一份
        //    用户收藏，只是入参换了首歌，重复度极高。
        List<Integer> candidateIds = recallSongsWithInfo.stream()
                .map(swri -> swri.getSong().getId())
                .filter(Objects::nonNull)
                .toList();
        RequestStats stats = loadRequestStats(userId, candidateIds);
        
        // 3. 为每首歌曲提取特征（纯内存计算，不再触库）
        List<SongWithFeature> songsWithFeatures = new ArrayList<>();
        
        for (RecallFusionService.SongWithRecallInfo swri : recallSongsWithInfo) {
            try {
                SongFeature feature = extractSingleSongFeatureWithRecallInfo(
                    userId, swri.getSong(), userStats, swri, stats);
                songsWithFeatures.add(new SongWithFeature(swri.getSong(), feature));
            } catch (Exception e) {
                log.error("提取歌曲特征失败，songId={}", swri.getSong().getId(), e);
            }
        }
        
        log.debug("特征提取完成，成功提取 {} 首歌曲特征", songsWithFeatures.size());
        return songsWithFeatures;
    }
    
    /**
     * 提取单首歌曲的特征（带召回信息）
     */
    private SongFeature extractSingleSongFeatureWithRecallInfo(
            Integer userId, Song song, UserStats userStats,
            RecallFusionService.SongWithRecallInfo recallInfo, RequestStats stats) {
        SongFeature feature = new SongFeature();
        
        // 用户特征
        feature.setUserId(userId);
        feature.setUserCollectCount(userStats.collectCount);
        feature.setUserAvgRating(userStats.avgRating);
        feature.setUserRatingCount(userStats.ratingCount);
        
        // 歌曲特征
        feature.setSongId(song.getId());
        feature.setSingerId(song.getSingerId());
        
        // 歌曲统计特征（取自请求级批量装载，不再触库）
        SongStats songStats = stats.songStatsOf(song.getId());
        feature.setSongCollectCount(songStats.collectCount);
        feature.setSongAvgRating(songStats.avgRating);
        feature.setSongRatingCount(songStats.ratingCount);
        
        // 歌曲年龄
        if (song.getCreateTime() != null) {
            LocalDate createDate = song.getCreateTime().toInstant()
                .atZone(ZoneId.systemDefault()).toLocalDate();
            long days = ChronoUnit.DAYS.between(createDate, LocalDate.now());
            feature.setSongAgeDays((int) days);
        } else {
            // 创建时间缺失时记为"年龄未知"，由打分侧按不扶持、不惩罚处理。
            // 旧实现填 0 会被当成当天发布的新歌而吃到扶持加分，与 boost 侧
            // 把 null 判为老歌的语义正好相反
            feature.setSongAgeDays(null);
        }
        
        // 交叉特征（按用户歌手分布查表，不再触库）
        CrossFeatures crossFeatures = stats.crossFeaturesOf(song.getSingerId());
        feature.setUserLikedSinger(crossFeatures.userLikedSinger);
        feature.setUserSingerCollectCount(crossFeatures.singerCollectCount);
        
        // 召回相关特征（使用真实值）
        feature.setRecallStrategyWeight(recallInfo.getRecallWeight());
        feature.setRecallRank(recallInfo.getRecallRank());
        
        log.debug("歌曲 {} 的召回策略: {}, 排名: {}, 权重: {}",
            song.getId(), recallInfo.getRecallStrategy(), 
            recallInfo.getRecallRank(), recallInfo.getRecallWeight());
        
        return feature;
    }
    
    /**
     * 计算用户统计特征
     */
    private UserStats calculateUserStats(Integer userId) {
        UserStats stats = new UserStats();
        
        // 用户收藏数量
        List<Collect> collects = collectMapper.collectOfUserId(userId);
        stats.collectCount = collects != null ? collects.size() : 0;
        
        // 用户评分均值与评分次数（1-10 分制）
        Map<String, Object> ratingStats = rankMapper.selectUserRatingStats(userId);
        stats.avgRating = readDouble(ratingStats, "avgRating");
        stats.ratingCount = readInt(ratingStats, "ratingCount");
        
        return stats;
    }
    
    /**
     * 批量装载本次推荐请求所需的全部统计快照。
     *
     * 30 个候选只需 3 条 SQL：候选收藏数、候选评分统计、用户收藏的歌手分布。
     * 改造前同一批数据要跑约 120 条 SQL（每首 2 条统计 + 每首 2 条交叉特征查询，
     * 而交叉特征反复查询同一份用户收藏）。
     */
    private RequestStats loadRequestStats(Integer userId, List<Integer> candidateIds) {
        RequestStats stats = new RequestStats(properties.getNeutralRating());

        if (!candidateIds.isEmpty()) {
            for (Map<String, Object> row : collectMapper.countCollectsBySongIds(candidateIds)) {
                stats.collectCountBySong.put(toInt(row.get("songId")), toInt(row.get("collectCount")));
            }
            for (Map<String, Object> row : rankMapper.selectRatingStatsBySongIds(candidateIds)) {
                Integer songId = toInt(row.get("songId"));
                stats.avgRatingBySong.put(songId, toDouble(row.get("avgRating")));
                stats.ratingCountBySong.put(songId, toInt(row.get("ratingCount")));
            }
        }

        List<Integer> collectedSongIds = collectMapper.selectSongIdsByUserId(userId);
        if (collectedSongIds != null && !collectedSongIds.isEmpty()) {
            List<Song> collectedSongs = songMapper.selectBatchByIds(collectedSongIds);
            if (collectedSongs != null) {
                for (Song collected : collectedSongs) {
                    if (collected != null && collected.getSingerId() != null) {
                        stats.singerCollectCount.merge(collected.getSingerId(), 1, Integer::sum);
                    }
                }
            }
        }

        return stats;
    }

    /**
     * 单次请求内的统计快照。生命周期仅一次 recommend 调用，因此不做成 Spring bean。
     */
    private static class RequestStats {
        final Map<Integer, Integer> collectCountBySong = new HashMap<>();
        final Map<Integer, Double> avgRatingBySong = new HashMap<>();
        final Map<Integer, Integer> ratingCountBySong = new HashMap<>();
        /** 用户收藏歌曲的歌手分布：singerId -> 收藏数量 */
        final Map<Integer, Integer> singerCollectCount = new HashMap<>();
        private final double neutralRating;

        RequestStats(double neutralRating) {
            this.neutralRating = neutralRating;
        }

        SongStats songStatsOf(Integer songId) {
            SongStats songStats = new SongStats();
            songStats.collectCount = collectCountBySong.getOrDefault(songId, 0);
            songStats.avgRating = avgRatingBySong.getOrDefault(songId, neutralRating);
            songStats.ratingCount = ratingCountBySong.getOrDefault(songId, 0);
            return songStats;
        }

        CrossFeatures crossFeaturesOf(Integer singerId) {
            CrossFeatures features = new CrossFeatures();
            if (singerId != null) {
                features.singerCollectCount = singerCollectCount.getOrDefault(singerId, 0);
                features.userLikedSinger = features.singerCollectCount > 0;
            }
            return features;
        }
    }
    
    /**
     * 统计行里的整型取值
     */
    private int toInt(Object value) {
        return value instanceof Number ? ((Number) value).intValue() : 0;
    }

    /**
     * 统计行里的浮点取值，缺失时回落中性评分
     */
    private double toDouble(Object value) {
        return value instanceof Number ? ((Number) value).doubleValue() : properties.getNeutralRating();
    }

    /**
     * 从统计结果读取均值。SQL 侧已用 COALESCE 兜底，此处再防一次结果缺失，
     * 避免拆箱 NPE 让整首歌曲被上层 catch 静默丢弃
     */
    private double readDouble(Map<String, Object> stats, String key) {
        if (stats == null) {
            return properties.getNeutralRating();
        }
        Object value = stats.get(key);
        return value instanceof Number ? ((Number) value).doubleValue() : properties.getNeutralRating();
    }

    /**
     * 从统计结果读取计数
     */
    private int readInt(Map<String, Object> stats, String key) {
        if (stats == null) {
            return 0;
        }
        Object value = stats.get(key);
        return value instanceof Number ? ((Number) value).intValue() : 0;
    }
    
    // ========== 内部类 ==========
    
    /**
     * 用户统计特征
     */
    private static class UserStats {
        int collectCount = 0;
        double avgRating;
        int ratingCount = 0;
    }
    
    /**
     * 歌曲统计特征
     */
    private static class SongStats {
        int collectCount = 0;
        double avgRating;
        int ratingCount = 0;
    }
    
    /**
     * 交叉特征
     */
    private static class CrossFeatures {
        boolean userLikedSinger = false;
        int singerCollectCount = 0;
    }
    
    /**
     * 带特征的歌曲
     */
    @lombok.Data
    @lombok.AllArgsConstructor
    public static class SongWithFeature {
        private Song song;
        private SongFeature feature;
    }
}

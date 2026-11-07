package com.shanzhu.music.service.ranking;

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

    /**
     * ranks.score 为 1-10 分制。无评分时以中点作中性兜底，
     * 避免"没有评分历史"被误判成"偏好强度很低"
     */
    private static final double NEUTRAL_RATING = 5.0;

    /**
     * 为召回的歌曲列表提取特征
     *
     * @param userId       用户ID
     * @param recallSongs  召回的歌曲列表
     * @return 带特征的歌曲列表
     */
    public List<SongWithFeature> extractFeatures(Integer userId, List<Song> recallSongs) {
        log.debug("开始为 {} 首召回歌曲提取特征", recallSongs.size());
        
        // 1. 预计算用户统计特征（避免重复查询）
        UserStats userStats = calculateUserStats(userId);
        
        // 2. 为每首歌曲提取特征
        List<SongWithFeature> songsWithFeatures = new ArrayList<>();
        int rank = 1;
        
        for (Song song : recallSongs) {
            try {
                SongFeature feature = extractSingleSongFeature(userId, song, userStats, rank);
                songsWithFeatures.add(new SongWithFeature(song, feature));
                rank++;
            } catch (Exception e) {
                log.error("提取歌曲特征失败，songId={}", song.getId(), e);
            }
        }
        
        log.debug("特征提取完成，成功提取 {} 首歌曲特征", songsWithFeatures.size());
        return songsWithFeatures;
    }
    
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
        
        // 2. 为每首歌曲提取特征
        List<SongWithFeature> songsWithFeatures = new ArrayList<>();
        
        for (RecallFusionService.SongWithRecallInfo swri : recallSongsWithInfo) {
            try {
                SongFeature feature = extractSingleSongFeatureWithRecallInfo(
                    userId, swri.getSong(), userStats, swri);
                songsWithFeatures.add(new SongWithFeature(swri.getSong(), feature));
            } catch (Exception e) {
                log.error("提取歌曲特征失败，songId={}", swri.getSong().getId(), e);
            }
        }
        
        log.debug("特征提取完成，成功提取 {} 首歌曲特征", songsWithFeatures.size());
        return songsWithFeatures;
    }
    
    /**
     * 提取单首歌曲的特征
     */
    private SongFeature extractSingleSongFeature(Integer userId, Song song, 
                                                   UserStats userStats, int recallRank) {
        SongFeature feature = new SongFeature();
        
        // 用户特征
        feature.setUserId(userId);
        feature.setUserCollectCount(userStats.collectCount);
        feature.setUserAvgRating(userStats.avgRating);
        feature.setUserRatingCount(userStats.ratingCount);
        
        // 歌曲特征
        feature.setSongId(song.getId());
        feature.setSingerId(song.getSingerId());
        
        // 歌曲统计特征
        SongStats songStats = calculateSongStats(song.getId());
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
            feature.setSongAgeDays(0);
        }
        
        // 交叉特征
        CrossFeatures crossFeatures = calculateCrossFeatures(userId, song);
        feature.setUserLikedSinger(crossFeatures.userLikedSinger);
        feature.setUserSingerCollectCount(crossFeatures.singerCollectCount);
        
        // 召回相关特征（默认值）
        feature.setRecallStrategyWeight(0.3);
        feature.setRecallRank(recallRank);
        
        return feature;
    }
    
    /**
     * 提取单首歌曲的特征（带召回信息）
     */
    private SongFeature extractSingleSongFeatureWithRecallInfo(
            Integer userId, Song song, UserStats userStats,
            RecallFusionService.SongWithRecallInfo recallInfo) {
        SongFeature feature = new SongFeature();
        
        // 用户特征
        feature.setUserId(userId);
        feature.setUserCollectCount(userStats.collectCount);
        feature.setUserAvgRating(userStats.avgRating);
        feature.setUserRatingCount(userStats.ratingCount);
        
        // 歌曲特征
        feature.setSongId(song.getId());
        feature.setSingerId(song.getSingerId());
        
        // 歌曲统计特征
        SongStats songStats = calculateSongStats(song.getId());
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
            feature.setSongAgeDays(0);
        }
        
        // 交叉特征
        CrossFeatures crossFeatures = calculateCrossFeatures(userId, song);
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
     * 计算歌曲统计特征
     */
    private SongStats calculateSongStats(Integer songId) {
        SongStats stats = new SongStats();
        
        // 歌曲被收藏次数
        Integer collectCount = collectMapper.countCollectsBySongId(songId);
        stats.collectCount = collectCount != null ? collectCount : 0;
        
        // 歌曲评分均值与评分次数（1-10 分制，经歌单关联）
        Map<String, Object> ratingStats = rankMapper.selectSongRatingStats(songId);
        stats.avgRating = readDouble(ratingStats, "avgRating");
        stats.ratingCount = readInt(ratingStats, "ratingCount");
        
        return stats;
    }
    
    /**
     * 计算交叉特征（批量查询优化版）
     * 解决 N+1 查询问题：一次性批量查询所有收藏歌曲的信息
     */
    private CrossFeatures calculateCrossFeatures(Integer userId, Song song) {
        CrossFeatures features = new CrossFeatures();
        
        if (song.getSingerId() == null) {
            features.userLikedSinger = false;
            features.singerCollectCount = 0;
            return features;
        }
        
        // 查询用户收藏的歌曲ID列表
        List<Integer> collectedSongIds = collectMapper.selectSongIdsByUserId(userId);
        
        if (collectedSongIds == null || collectedSongIds.isEmpty()) {
            features.userLikedSinger = false;
            features.singerCollectCount = 0;
            return features;
        }
        
        // ✅ 批量查询：一次 IN (...) 查询所有收藏歌曲的歌手ID
        List<Song> collectedSongs = songMapper.selectBatchByIds(collectedSongIds);
        
        if (collectedSongs == null || collectedSongs.isEmpty()) {
            features.userLikedSinger = false;
            features.singerCollectCount = 0;
            return features;
        }
        
        // 统计用户收藏的同歌手歌曲数量
        int singerCollectCount = 0;
        for (Song collectedSong : collectedSongs) {
            if (collectedSong != null && 
                song.getSingerId().equals(collectedSong.getSingerId())) {
                singerCollectCount++;
            }
        }
        
        features.userLikedSinger = singerCollectCount > 0;
        features.singerCollectCount = singerCollectCount;
        
        return features;
    }

    /**
     * 从统计结果读取均值。SQL 侧已用 COALESCE 兜底，此处再防一次结果缺失，
     * 避免拆箱 NPE 让整首歌曲被上层 catch 静默丢弃
     */
    private double readDouble(Map<String, Object> stats, String key) {
        if (stats == null) {
            return NEUTRAL_RATING;
        }
        Object value = stats.get(key);
        return value instanceof Number ? ((Number) value).doubleValue() : NEUTRAL_RATING;
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
        double avgRating = NEUTRAL_RATING;
        int ratingCount = 0;
    }
    
    /**
     * 歌曲统计特征
     */
    private static class SongStats {
        int collectCount = 0;
        double avgRating = NEUTRAL_RATING;
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

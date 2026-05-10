package com.shanzhu.music.service.ranking;

import com.shanzhu.music.entity.dto.SongFeature;
import com.shanzhu.music.entity.po.Collect;
import com.shanzhu.music.entity.po.Rank;
import com.shanzhu.music.entity.po.Song;
import com.shanzhu.music.mapper.CollectMapper;
import com.shanzhu.music.mapper.RankMapper;
import com.shanzhu.music.mapper.SongMapper;
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
     * 提取单首歌曲的特征
     */
    private SongFeature extractSingleSongFeature(Integer userId, Song song, 
                                                   UserStats userStats, int recallRank) {
        SongFeature feature = new SongFeature();
        
        // 用户特征
        feature.setUserId(userId);
        feature.setUserCollectCount(userStats.collectCount);
        feature.setUserAvgRating(userStats.avgRating);
        
        // 歌曲特征
        feature.setSongId(song.getId());
        feature.setSingerId(song.getSingerId());
        
        // 歌曲统计特征
        SongStats songStats = calculateSongStats(song.getId());
        feature.setSongCollectCount(songStats.collectCount);
        feature.setSongAvgRating(songStats.avgRating);
        
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
        
        // 召回相关特征（默认值，实际应从召回结果中获取）
        feature.setRecallStrategyWeight(0.3);
        feature.setRecallRank(recallRank);
        
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
        
        // 用户平均评分（从ranks表查询）
        Double avgRating = rankMapper.selectAvgRatingByUserId(userId);
        stats.avgRating = avgRating != null ? avgRating : 4.0;
        
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
        
        // 歌曲平均评分（从ranks表关联查询）
        Double avgRating = rankMapper.selectAvgRatingBySongId(songId);
        stats.avgRating = avgRating != null ? avgRating : 4.0;
        
        return stats;
    }
    
    /**
     * 计算交叉特征
     */
    private CrossFeatures calculateCrossFeatures(Integer userId, Song song) {
        CrossFeatures features = new CrossFeatures();
        
        if (song.getSingerId() == null) {
            features.userLikedSinger = false;
            features.singerCollectCount = 0;
            return features;
        }
        
        // 查询用户收藏的歌曲
        List<Collect> userCollects = collectMapper.collectOfUserId(userId);
        
        if (userCollects == null || userCollects.isEmpty()) {
            features.userLikedSinger = false;
            features.singerCollectCount = 0;
            return features;
        }
        
        // 统计用户收藏的同歌手歌曲数量
        List<Integer> collectedSongIds = userCollects.stream()
            .map(Collect::getSongId)
            .collect(Collectors.toList());
        
        int singerCollectCount = 0;
        for (Integer collectedSongId : collectedSongIds) {
            Song collectedSong = songMapper.selectByPrimaryKey(collectedSongId);
            if (collectedSong != null && 
                song.getSingerId().equals(collectedSong.getSingerId())) {
                singerCollectCount++;
            }
        }
        
        features.userLikedSinger = singerCollectCount > 0;
        features.singerCollectCount = singerCollectCount;
        
        return features;
    }
    
    // ========== 内部类 ==========
    
    /**
     * 用户统计特征
     */
    private static class UserStats {
        int collectCount = 0;
        double avgRating = 4.0;
    }
    
    /**
     * 歌曲统计特征
     */
    private static class SongStats {
        int collectCount = 0;
        double avgRating = 4.0;
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

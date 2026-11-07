package com.shanzhu.music.service.ranking;

import com.shanzhu.music.config.RecommendationProperties;
import com.shanzhu.music.entity.dto.SongFeature;
import com.shanzhu.music.entity.po.Song;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * 精排服务
 *
 * 命名沿用 GBDT，但当前实现是"加权线性 + FM 二阶交叉 + sigmoid"，不含树模型与训练过程。
 * 权重、量纲上界、交叉系数、偏置统一由 RecommendationProperties 提供，调整参数无需重新编译
 * （真实 GBDT 需要离线训练与模型文件，路线见 docs 中的说明）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GBDTRankingService {

    private final NewSongBoostService newSongBoostService;
    private final RecommendationProperties properties;
    
    /**
     * 对带特征的歌曲进行排序
     *
     * @param songsWithFeatures 带特征的歌曲列表
     * @return 排序后的歌曲列表
     */
    public List<Song> rank(List<FeatureEngineeringService.SongWithFeature> songsWithFeatures) {
        log.debug("开始GBDT排序，候选歌曲数量: {}", songsWithFeatures.size());
        
        // 1. 为每首歌曲计算得分
        List<SongScore> songScores = new ArrayList<>();
        
        for (FeatureEngineeringService.SongWithFeature swf : songsWithFeatures) {
            // 新歌扶持已在 calculateScore 内、sigmoid 之前叠加
            double score = calculateScore(swf.getFeature());
            songScores.add(new SongScore(swf.getSong(), score));
        }
        
        // 3. 按得分降序排序
        songScores.sort((a, b) -> Double.compare(b.score, a.score));
        
        // 4. 提取排序后的歌曲列表
        List<Song> rankedSongs = new ArrayList<>();
        for (SongScore ss : songScores) {
            rankedSongs.add(ss.song);
        }
        
        log.debug("GBDT排序完成");
        return rankedSongs;
    }
    
    /**
     * 计算歌曲得分（模拟GBDT预测 + FM特征交叉）
     * 
     * 公式: score = linear_part + fm_cross_part + bias
     */
    private double calculateScore(SongFeature feature) {
        double ratingMax = properties.getRatingMax();

        // ========== 1. 线性部分 ==========
        double linearScore = 0.0;
        
        // 用户特征
        linearScore += properties.featureWeight("userCollectCount") * normalize(feature.getUserCollectCount(), 0, 100);
        linearScore += properties.featureWeight("userAvgRating") * normalize(feature.getUserAvgRating(), 0, ratingMax);
        linearScore += properties.featureWeight("userRatingCount") * normalize(feature.getUserRatingCount(), 0, 100);
        
        // 歌曲特征
        linearScore += properties.featureWeight("songCollectCount") * normalize(feature.getSongCollectCount(), 0, 1000);
        linearScore += properties.featureWeight("songAvgRating") * normalize(feature.getSongAvgRating(), 0, ratingMax);
        linearScore += properties.featureWeight("songRatingCount") * normalize(feature.getSongRatingCount(), 0, 1000);
        
        // 交叉特征
        linearScore += properties.featureWeight("userLikedSinger") * (Boolean.TRUE.equals(feature.getUserLikedSinger()) ? 1.0 : 0.0);
        linearScore += properties.featureWeight("userSingerCollectCount") * normalize(feature.getUserSingerCollectCount(), 0, 10);
        
        // 召回特征
        linearScore += properties.featureWeight("recallStrategyWeight") * normalize(feature.getRecallStrategyWeight(), 0, 1);
        linearScore += properties.featureWeight("recallRank") * normalize(feature.getRecallRank(), 1, 100);
        
        // ========== 2. FM二阶特征交叉部分 ==========
        double fmScore = calculateFMCross(feature);
        
        // ========== 3. 组合得分 ==========
        double newSongBonus = newSongBoostService.calculateBonus(feature.getSongAgeDays());
        double finalScore = linearScore
                + properties.getFmCoefficient() * fmScore
                + properties.getBias()
                + newSongBonus;
        
        // Sigmoid激活函数，将得分映射到(0, 1)区间
        return sigmoid(finalScore);
    }
    
    /**
     * 计算FM二阶特征交叉
     * 模拟 Factorization Machine 的特征交互
     * 
     * 核心思想：捕捉特征之间的非线性关系
     * 例如：用户喜欢歌手 × 歌曲热度 = 更强的推荐信号
     */
    private double calculateFMCross(SongFeature feature) {
        double crossScore = 0.0;
        
        // 交叉1: 用户收藏数 × 歌曲热度（活跃用户更可能喜欢热门歌曲）
        double userActivity = normalize(feature.getUserCollectCount(), 0, 100);
        double songPopularity = normalize(feature.getSongCollectCount(), 0, 1000);
        crossScore += properties.fmCrossWeight("activityXPopularity") * userActivity * songPopularity;
        
        // 交叉2: 用户是否喜欢歌手 × 同歌手收藏数（强化偏好）
        if (Boolean.TRUE.equals(feature.getUserLikedSinger())) {
            double singerPreference = normalize(feature.getUserSingerCollectCount(), 0, 10);
            crossScore += properties.fmCrossWeight("singerPreference") * singerPreference;
        }
        
        // 交叉3: 歌曲评分 × 召回策略权重（高质量歌曲在好策略中更重要）
        double songQuality = normalize(feature.getSongAvgRating(), 0, properties.getRatingMax());
        double strategyWeight = normalize(feature.getRecallStrategyWeight(), 0, 1);
        crossScore += properties.fmCrossWeight("qualityXStrategy") * songQuality * strategyWeight;
        
        // 交叉4: 新歌 × 用户活跃度（活跃用户更愿意尝试新歌）
        double isNewSong = feature.getSongAgeDays() != null
                && feature.getSongAgeDays() <= properties.getNewSong().getThresholdDays() ? 1.0 : 0.0;
        crossScore += properties.fmCrossWeight("newSongXActivity") * isNewSong * userActivity;
        
        return crossScore;
    }
    
    /**
     * 特征归一化 (Min-Max Normalization)
     */
    private double normalize(double value, double min, double max) {
        if (max == min) return 0.5;
        double normalized = (value - min) / (max - min);
        return Math.max(0.0, Math.min(1.0, normalized)); // 裁剪到[0, 1]
    }
    
    /**
     * Sigmoid激活函数
     */
    private double sigmoid(double x) {
        return 1.0 / (1.0 + Math.exp(-x));
    }
    
    /**
     * 歌曲得分
     */
    private static class SongScore {
        Song song;
        double score;
        
        SongScore(Song song, double score) {
            this.song = song;
            this.score = score;
        }
    }
}

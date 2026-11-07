package com.shanzhu.music.service.ranking;

import com.shanzhu.music.entity.dto.SongFeature;
import com.shanzhu.music.entity.po.Song;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * GBDT排序服务（简化版）
 * 使用加权线性模型模拟GBDT的打分逻辑
 * 
 * 实际生产中应该：
 * 1. 使用XGBoost/LightGBM训练GBDT模型
 * 2. 或使用TensorFlow/PyTorch实现DeepFM
 * 3. 定期离线训练，在线预测
 */
@Slf4j
@Service
public class GBDTRankingService {
    
    private final NewSongBoostService newSongBoostService;
    

    public GBDTRankingService(NewSongBoostService newSongBoostService) {
        this.newSongBoostService = newSongBoostService;
    }
    
    // 特征权重（可通过离线训练得到）
    private static final Map<String, Double> FEATURE_WEIGHTS = new HashMap<>();

    /**
     * ranks.score 为 1-10 分制。归一化上界必须与真实量纲一致，
     * 否则 score>=5 的样本会被裁剪饱和到 1.0，该特征彻底失去区分度
     */
    private static final double RATING_MAX = 10.0;
    
    static {
        // 用户特征权重
        FEATURE_WEIGHTS.put("userCollectCount", 0.05);
        FEATURE_WEIGHTS.put("userAvgRating", 0.1);
        FEATURE_WEIGHTS.put("userRatingCount", 0.03);   // 置信度：评分次数越少，均值越不可信
        
        // 歌曲特征权重
        FEATURE_WEIGHTS.put("songCollectCount", 0.25);  // 热度最重要
        FEATURE_WEIGHTS.put("songAvgRating", 0.2);
        FEATURE_WEIGHTS.put("songRatingCount", 0.05);   // 置信度
        // 不再对 songAgeDays 设负权重：新歌提权统一由 NewSongBoostService
        // 在 sigmoid 之前以加性增益实现，避免两处重复计分
        
        // 交叉特征权重
        FEATURE_WEIGHTS.put("userLikedSinger", 0.15);
        FEATURE_WEIGHTS.put("userSingerCollectCount", 0.1);
        
        // 召回特征权重
        FEATURE_WEIGHTS.put("recallStrategyWeight", 0.1);
        FEATURE_WEIGHTS.put("recallRank", -0.05);       // 召回排名越靠前越好
    }
    
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
        // ========== 1. 线性部分 ==========
        double linearScore = 0.0;
        
        // 用户特征
        linearScore += FEATURE_WEIGHTS.get("userCollectCount") * normalize(feature.getUserCollectCount(), 0, 100);
        linearScore += FEATURE_WEIGHTS.get("userAvgRating") * normalize(feature.getUserAvgRating(), 0, RATING_MAX);
        linearScore += FEATURE_WEIGHTS.get("userRatingCount") * normalize(feature.getUserRatingCount(), 0, 100);
        
        // 歌曲特征
        linearScore += FEATURE_WEIGHTS.get("songCollectCount") * normalize(feature.getSongCollectCount(), 0, 1000);
        linearScore += FEATURE_WEIGHTS.get("songAvgRating") * normalize(feature.getSongAvgRating(), 0, RATING_MAX);
        linearScore += FEATURE_WEIGHTS.get("songRatingCount") * normalize(feature.getSongRatingCount(), 0, 1000);
        
        // 交叉特征
        linearScore += FEATURE_WEIGHTS.get("userLikedSinger") * (feature.getUserLikedSinger() ? 1.0 : 0.0);
        linearScore += FEATURE_WEIGHTS.get("userSingerCollectCount") * normalize(feature.getUserSingerCollectCount(), 0, 10);
        
        // 召回特征
        linearScore += FEATURE_WEIGHTS.get("recallStrategyWeight") * normalize(feature.getRecallStrategyWeight(), 0, 1);
        linearScore += FEATURE_WEIGHTS.get("recallRank") * normalize(feature.getRecallRank(), 1, 100);
        
        // ========== 2. FM二阶特征交叉部分 ==========
        double fmScore = calculateFMCross(feature);
        
        // ========== 3. 组合得分 ==========
        double bias = 0.5;
        double newSongBonus = newSongBoostService.calculateBonus(feature.getSongAgeDays());
        double finalScore = linearScore + 0.3 * fmScore + bias + newSongBonus; // FM部分权重0.3
        
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
        crossScore += 0.15 * userActivity * songPopularity;
        
        // 交叉2: 用户是否喜欢歌手 × 同歌手收藏数（强化偏好）
        if (feature.getUserLikedSinger()) {
            double singerPreference = normalize(feature.getUserSingerCollectCount(), 0, 10);
            crossScore += 0.2 * singerPreference;
        }
        
        // 交叉3: 歌曲评分 × 召回策略权重（高质量歌曲在好策略中更重要）
        double songQuality = normalize(feature.getSongAvgRating(), 0, RATING_MAX);
        double strategyWeight = normalize(feature.getRecallStrategyWeight(), 0, 1);
        crossScore += 0.1 * songQuality * strategyWeight;
        
        // 交叉4: 新歌 × 用户活跃度（活跃用户更愿意尝试新歌）
        double isNewSong = feature.getSongAgeDays() != null && feature.getSongAgeDays() <= 30 ? 1.0 : 0.0;
        crossScore += 0.05 * isNewSong * userActivity;
        
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

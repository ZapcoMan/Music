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
    
    // 特征权重（可通过离线训练得到）
    private static final Map<String, Double> FEATURE_WEIGHTS = new HashMap<>();
    
    static {
        // 用户特征权重
        FEATURE_WEIGHTS.put("userCollectCount", 0.05);
        FEATURE_WEIGHTS.put("userAvgRating", 0.1);
        
        // 歌曲特征权重
        FEATURE_WEIGHTS.put("songCollectCount", 0.25);  // 热度最重要
        FEATURE_WEIGHTS.put("songAvgRating", 0.2);
        FEATURE_WEIGHTS.put("songAgeDays", -0.02);      // 新歌优先（负权重）
        
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
            double score = calculateScore(swf.getFeature());
            songScores.add(new SongScore(swf.getSong(), score));
        }
        
        // 2. 按得分降序排序
        songScores.sort((a, b) -> Double.compare(b.score, a.score));
        
        // 3. 提取排序后的歌曲列表
        List<Song> rankedSongs = new ArrayList<>();
        for (SongScore ss : songScores) {
            rankedSongs.add(ss.song);
        }
        
        log.debug("GBDT排序完成");
        return rankedSongs;
    }
    
    /**
     * 计算歌曲得分（模拟GBDT预测）
     * 
     * 公式: score = Σ(weight_i * feature_i) + bias
     */
    private double calculateScore(SongFeature feature) {
        double score = 0.0;
        
        // 用户特征
        score += FEATURE_WEIGHTS.get("userCollectCount") * normalize(feature.getUserCollectCount(), 0, 100);
        score += FEATURE_WEIGHTS.get("userAvgRating") * normalize(feature.getUserAvgRating(), 0, 5);
        
        // 歌曲特征
        score += FEATURE_WEIGHTS.get("songCollectCount") * normalize(feature.getSongCollectCount(), 0, 1000);
        score += FEATURE_WEIGHTS.get("songAvgRating") * normalize(feature.getSongAvgRating(), 0, 5);
        score += FEATURE_WEIGHTS.get("songAgeDays") * normalize(feature.getSongAgeDays(), 0, 3650);
        
        // 交叉特征
        score += FEATURE_WEIGHTS.get("userLikedSinger") * (feature.getUserLikedSinger() ? 1.0 : 0.0);
        score += FEATURE_WEIGHTS.get("userSingerCollectCount") * normalize(feature.getUserSingerCollectCount(), 0, 10);
        
        // 召回特征
        score += FEATURE_WEIGHTS.get("recallStrategyWeight") * normalize(feature.getRecallStrategyWeight(), 0, 1);
        score += FEATURE_WEIGHTS.get("recallRank") * normalize(feature.getRecallRank(), 1, 100);
        
        // 添加偏置项
        double bias = 0.5;
        score += bias;
        
        // Sigmoid激活函数，将得分映射到(0, 1)区间
        return sigmoid(score);
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

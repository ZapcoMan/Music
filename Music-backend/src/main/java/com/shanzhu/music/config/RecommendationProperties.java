package com.shanzhu.music.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * 推荐算法可调参数。
 *
 * 此前特征权重、召回权重、归一化上界、新歌分档全部写死在各服务的 static final 常量里，
 * 调一次参数就要重新编译，且很容易与文档记录的取值失配；统一收敛到这里后，
 * application.yml 是唯一出处，下面的默认值仅作为未配置时的兜底。
 */
@Data
@Component
@ConfigurationProperties(prefix = "recommendation")
public class RecommendationProperties {

    /**
     * 排序特征权重，键为特征名
     */
    private Map<String, Double> featureWeights = defaultFeatureWeights();

    /**
     * FM 二阶交叉项系数，键为交叉项名称
     */
    private Map<String, Double> fmCrossWeights = defaultFmCrossWeights();

    /**
     * 六路召回权重，键为策略名（RecallStrategy#getStrategyName）
     */
    private Map<String, Double> strategyWeights = defaultStrategyWeights();

    /**
     * ranks.score 的量纲上界。该列实际是 1-10 分制，
     * 归一化上界必须与真实量纲一致，否则 score>=5 的样本会全部饱和到 1.0
     */
    private double ratingMax = 10.0;

    /**
     * 无评分历史时的中性兜底评分（取量纲中点，避免被当成低偏好）
     */
    private double neutralRating = 5.0;

    /**
     * FM 部分在总分中的系数
     */
    private double fmCoefficient = 0.3;

    /**
     * 排序偏置
     */
    private double bias = 0.5;

    /**
     * 新歌扶持参数
     */
    private NewSong newSong = new NewSong();

    @Data
    public static class NewSong {
        /** 超过该天数不再扶持 */
        private int thresholdDays = 30;
        /** 强扶持区间上限 */
        private int strongDays = 7;
        /** 中扶持区间上限 */
        private int mediumDays = 14;
        private double strongBonus = 0.15;
        private double mediumBonus = 0.10;
        private double lightBonus = 0.05;
    }

    public double featureWeight(String name) {
        return featureWeights.getOrDefault(name, 0.0);
    }

    public double fmCrossWeight(String name) {
        return fmCrossWeights.getOrDefault(name, 0.0);
    }

    /**
     * 取策略权重，未配置时回落到策略自身声明的默认值
     */
    public double strategyWeight(String name, double fallback) {
        return strategyWeights.getOrDefault(name, fallback);
    }

    private static Map<String, Double> defaultFeatureWeights() {
        Map<String, Double> weights = new HashMap<>();
        weights.put("userCollectCount", 0.05);
        weights.put("userAvgRating", 0.10);
        weights.put("userRatingCount", 0.03);
        weights.put("songCollectCount", 0.25);
        weights.put("songAvgRating", 0.20);
        weights.put("songRatingCount", 0.05);
        weights.put("userLikedSinger", 0.15);
        weights.put("userSingerCollectCount", 0.10);
        weights.put("recallStrategyWeight", 0.10);
        weights.put("recallRank", -0.05);
        return weights;
    }

    private static Map<String, Double> defaultFmCrossWeights() {
        Map<String, Double> weights = new HashMap<>();
        weights.put("activityXPopularity", 0.15);
        weights.put("singerPreference", 0.20);
        weights.put("qualityXStrategy", 0.10);
        weights.put("newSongXActivity", 0.05);
        return weights;
    }

    private static Map<String, Double> defaultStrategyWeights() {
        Map<String, Double> weights = new HashMap<>();
        weights.put("collaborative_filtering", 0.40);
        weights.put("content_based", 0.25);
        weights.put("personalized", 0.20);
        weights.put("tag_based", 0.20);
        weights.put("search_history", 0.20);
        weights.put("popular", 0.15);
        return weights;
    }
}

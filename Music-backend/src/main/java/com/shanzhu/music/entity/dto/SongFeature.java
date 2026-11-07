package com.shanzhu.music.entity.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 歌曲推荐特征
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SongFeature {
    
    // ========== 用户特征 ==========
    /**
     * 用户ID
     */
    private Integer userId;
    
    /**
     * 用户收藏歌曲数量
     */
    private Integer userCollectCount;
    
    /**
     * 用户平均评分（1-10 分制）
     */
    private Double userAvgRating;

    /**
     * 用户评分次数：置信度信号，次数越少均值越不可信
     */
    private Integer userRatingCount;
    
    // ========== 歌曲特征 ==========
    /**
     * 歌曲ID
     */
    private Integer songId;
    
    /**
     * 歌手ID
     */
    private Integer singerId;
    
    /**
     * 歌曲被收藏次数（热度）
     */
    private Integer songCollectCount;
    
    /**
     * 歌曲平均评分（1-10 分制）
     */
    private Double songAvgRating;

    /**
     * 歌曲被评分次数：置信度信号
     */
    private Integer songRatingCount;
    
    /**
     * 歌曲发布时间距今天数
     */
    private Integer songAgeDays;
    
    // ========== 交叉特征 ==========
    /**
     * 用户是否收藏过该歌手的其他歌曲
     */
    private Boolean userLikedSinger;
    
    /**
     * 用户收藏的歌曲中同歌手的数量
     */
    private Integer userSingerCollectCount;
    
    /**
     * 召回策略权重
     */
    private Double recallStrategyWeight;
    
    /**
     * 召回排名位置
     */
    private Integer recallRank;
}

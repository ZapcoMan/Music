package com.shanzhu.music.service.recall;

import com.shanzhu.music.entity.dto.RecallResult;

/**
 * 召回策略接口
 */
public interface RecallStrategy {
    
    /**
     * 执行召回
     *
     * @param userId 用户ID
     * @return 召回结果
     */
    RecallResult recall(Integer userId);
    
    /**
     * 获取策略名称
     *
     * @return 策略名称
     */
    String getStrategyName();
    
    /**
     * 获取策略权重。
     * 该返回值只是策略自带的默认值，实际参与融合的权重会被
     * recommendation.strategy-weights 配置覆盖（见 RecallFusionService）。
     *
     * @return 权重值
     */
    Double getWeight();
}

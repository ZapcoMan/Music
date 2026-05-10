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
     * 获取策略权重
     *
     * @return 权重值
     */
    Double getWeight();
}

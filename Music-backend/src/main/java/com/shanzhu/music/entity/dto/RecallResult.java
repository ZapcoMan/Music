package com.shanzhu.music.entity.dto;

import com.shanzhu.music.entity.po.Song;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * 召回结果
 */
@Data
@NoArgsConstructor
public class RecallResult {

    /**
     * 召回策略名称
     */
    private String strategyName;

    /**
     * 召回的歌曲列表
     */
    private List<Song> songs;

    /**
     * 召回数量
     */
    private Integer count;

    /**
     * 权重（用于融合排序）
     */
    private Double weight;

    /**
     * 策略是否执行失败。
     * 为 true 时 songs 为空表示"该路不可用"，而不是"用户在这条路径上没有偏好"，
     * 二者必须可区分，否则 SQL 报错、表缺失、数据为空会呈现为同一种静默结果。
     */
    private boolean failed;

    /**
     * 失败原因摘要，供日志与降级信息使用
     */
    private String failureReason;

    /**
     * 正常结果。保持与既有调用方一致的四参形态。
     */
    public RecallResult(String strategyName, List<Song> songs, Integer count, Double weight) {
        this(strategyName, songs, count, weight, false, null);
    }

    public RecallResult(String strategyName, List<Song> songs, Integer count, Double weight,
                        boolean failed, String failureReason) {
        this.strategyName = strategyName;
        this.songs = songs;
        this.count = count;
        this.weight = weight;
        this.failed = failed;
        this.failureReason = failureReason;
    }

    /**
     * 策略执行异常时的结果，调用方需按 failed 区分处理
     */
    public static RecallResult failed(String strategyName, Double weight, String reason) {
        return new RecallResult(strategyName, new ArrayList<>(), 0, weight, true, reason);
    }
}

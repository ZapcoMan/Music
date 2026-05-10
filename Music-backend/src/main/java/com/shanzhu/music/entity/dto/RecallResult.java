package com.shanzhu.music.entity.dto;

import com.shanzhu.music.entity.po.Song;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 召回结果
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
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
}

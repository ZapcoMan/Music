package com.shanzhu.music.service.recall.impl;

import com.shanzhu.music.entity.dto.RecallResult;
import com.shanzhu.music.entity.po.Song;
import com.shanzhu.music.mapper.CollectMapper;
import com.shanzhu.music.mapper.SongMapper;
import com.shanzhu.music.service.recall.RecallStrategy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 热门召回策略
 * 基于全局收藏热度进行推荐
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PopularRecall implements RecallStrategy {
    
    private final SongMapper songMapper;
    private final CollectMapper collectMapper;
    
    // 策略权重
    private static final Double STRATEGY_WEIGHT = 0.15;
    // 最大召回数量
    private static final Integer MAX_RECALL_COUNT = 20;

    @Override
    public RecallResult recall(Integer userId) {
        log.debug("执行热门召回，userId={}", userId);
        
        List<Song> recalledSongs = new ArrayList<>();
        
        try {
            // 1. 获取热门歌曲（按真实收藏量排序）
            List<Song> hotSongs = songMapper.getHotSongs(MAX_RECALL_COUNT);
            
            if (hotSongs == null || hotSongs.isEmpty()) {
                return new RecallResult(getStrategyName(), recalledSongs, 0, STRATEGY_WEIGHT);
            }
            
            // 2. 获取用户已收藏的歌曲ID（用于排除）
            List<Song> userCollectedSongs = songMapper.getCollectByUserID(userId);
            Set<Integer> collectedSongIds = userCollectedSongs != null ? 
                userCollectedSongs.stream().map(Song::getId).collect(Collectors.toSet()) : 
                new HashSet<>();
            
            // 3. 排除用户已收藏的歌曲
            recalledSongs = hotSongs.stream()
                .filter(song -> !collectedSongIds.contains(song.getId()))
                .limit(MAX_RECALL_COUNT)
                .collect(Collectors.toList());
            
            log.debug("热门召回 {} 首歌曲", recalledSongs.size());
            
        } catch (Exception e) {
            log.error("热门召回失败，userId={}", userId, e);
        }
        
        return new RecallResult(getStrategyName(), recalledSongs, recalledSongs.size(), STRATEGY_WEIGHT);
    }

    @Override
    public String getStrategyName() {
        return "popular";
    }

    @Override
    public Double getWeight() {
        return STRATEGY_WEIGHT;
    }
}

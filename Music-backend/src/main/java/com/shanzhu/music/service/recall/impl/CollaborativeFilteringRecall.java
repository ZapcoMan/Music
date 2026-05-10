package com.shanzhu.music.service.recall.impl;

import com.shanzhu.music.entity.dto.RecallResult;
import com.shanzhu.music.entity.po.Song;
import com.shanzhu.music.mapper.SongListMapper;
import com.shanzhu.music.mapper.SongMapper;
import com.shanzhu.music.service.recall.RecallStrategy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * 协同过滤召回策略
 * 基于用户相似度进行推荐
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CollaborativeFilteringRecall implements RecallStrategy {
    
    private final SongListMapper songListMapper;
    private final SongMapper songMapper;
    
    // 策略权重
    private static final Double STRATEGY_WEIGHT = 0.4;
    // 最大召回数量
    private static final Integer MAX_RECALL_COUNT = 50;

    @Override
    public RecallResult recall(Integer userId) {
        log.debug("执行协同过滤召回，userId={}", userId);
        
        List<Song> recalledSongs = new ArrayList<>();
        
        try {
            // 1. 获取相似用户及其相似度分数
            List<Map<String, Object>> similarUsersWithScore = 
                songListMapper.findSimilarUsersWithScore(userId);
            
            if (similarUsersWithScore == null || similarUsersWithScore.isEmpty()) {
                log.debug("未找到相似用户");
                return new RecallResult(getStrategyName(), recalledSongs, 0, STRATEGY_WEIGHT);
            }
            
            // 2. 使用Map存储歌曲及其加权得分
            Map<Integer, Double> songWeightMap = new HashMap<>();
            Map<Integer, Song> songMap = new HashMap<>();
            
            // 3. 遍历每个相似用户，根据相似度加权累加
            for (Map<String, Object> userWithScore : similarUsersWithScore) {
                Integer similarUserId = ((Number) userWithScore.get("userId")).intValue();
                Double similarity = ((Number) userWithScore.get("similarity")).doubleValue();
                
                // 获取该相似用户收藏但当前用户未收藏的歌曲
                List<Song> songs = songMapper.getSongsByUserIdNotInOtherUser(userId, similarUserId);
                
                if (songs != null && !songs.isEmpty()) {
                    for (Song song : songs) {
                        double currentWeight = songWeightMap.getOrDefault(song.getId(), 0.0);
                        songWeightMap.put(song.getId(), currentWeight + similarity);
                        
                        if (!songMap.containsKey(song.getId())) {
                            songMap.put(song.getId(), song);
                        }
                    }
                }
            }
            
            // 4. 按加权得分降序排序
            List<Map.Entry<Integer, Double>> sortedSongs = new ArrayList<>(songWeightMap.entrySet());
            sortedSongs.sort((e1, e2) -> Double.compare(e2.getValue(), e1.getValue()));
            
            // 5. 构建召回结果（最多返回MAX_RECALL_COUNT首）
            int limit = Math.min(sortedSongs.size(), MAX_RECALL_COUNT);
            for (int i = 0; i < limit; i++) {
                Integer songId = sortedSongs.get(i).getKey();
                Song song = songMap.get(songId);
                if (song != null) {
                    recalledSongs.add(song);
                }
            }
            
            log.debug("协同过滤召回 {} 首歌曲", recalledSongs.size());
            
        } catch (Exception e) {
            log.error("协同过滤召回失败，userId={}", userId, e);
        }
        
        return new RecallResult(getStrategyName(), recalledSongs, recalledSongs.size(), STRATEGY_WEIGHT);
    }

    @Override
    public String getStrategyName() {
        return "collaborative_filtering";
    }

    @Override
    public Double getWeight() {
        return STRATEGY_WEIGHT;
    }
}

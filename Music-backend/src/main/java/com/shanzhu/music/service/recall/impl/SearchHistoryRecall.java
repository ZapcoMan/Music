package com.shanzhu.music.service.recall.impl;

import com.shanzhu.music.entity.dto.RecallResult;
import com.shanzhu.music.entity.po.Song;
import com.shanzhu.music.mapper.CollectMapper;
import com.shanzhu.music.mapper.SearchHistoryMapper;
import com.shanzhu.music.mapper.SongMapper;
import com.shanzhu.music.service.recall.RecallStrategy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 搜索历史召回策略
 * 基于用户最近搜索的关键词进行推荐
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SearchHistoryRecall implements RecallStrategy {
    
    private final SongMapper songMapper;
    private final CollectMapper collectMapper;
    private final SearchHistoryMapper searchHistoryMapper;
    
    // 策略权重
    private static final Double STRATEGY_WEIGHT = 0.20;
    // 最大召回数量
    private static final Integer MAX_RECALL_COUNT = 20;
    // 获取最近搜索关键词数量
    private static final Integer RECENT_KEYWORDS_COUNT = 10;

    @Override
    public RecallResult recall(Integer userId) {
        log.debug("执行搜索历史召回，userId={}", userId);
        
        List<Song> recalledSongs = new ArrayList<>();
        
        try {
            // 1. 获取用户最近的搜索关键词
            List<String> recentKeywords = searchHistoryMapper.getRecentKeywords(userId, RECENT_KEYWORDS_COUNT);
            
            if (recentKeywords == null || recentKeywords.isEmpty()) {
                log.debug("用户没有搜索历史");
                return new RecallResult(getStrategyName(), recalledSongs, 0, STRATEGY_WEIGHT);
            }
            
            log.debug("用户最近搜索关键词数量: {}", recentKeywords.size());
            
            // 2. 获取用户已收藏的歌曲ID（用于排除）
            List<Song> userCollectedSongs = songMapper.getCollectByUserID(userId);
            Set<Integer> collectedSongIds = userCollectedSongs != null ? 
                userCollectedSongs.stream().map(Song::getId).collect(Collectors.toSet()) : 
                new HashSet<>();
            
            // 3. 根据搜索关键词召回相关歌曲
            Map<Integer, Song> candidateSongs = new HashMap<>();
            Map<Integer, Integer> songRelevanceScore = new HashMap<>();
            
            for (String keyword : recentKeywords) {
                // 使用模糊查询匹配歌曲名称
                List<Song> matchedSongs = songMapper.likeSongOfNameForRecall("%" + keyword + "%");
                
                if (matchedSongs != null && !matchedSongs.isEmpty()) {
                    for (Song song : matchedSongs) {
                        // 排除用户已收藏的歌曲
                        if (!collectedSongIds.contains(song.getId())) {
                            candidateSongs.putIfAbsent(song.getId(), song);
                            
                            // 累加相关度得分（匹配的关键词次数）
                            int currentScore = songRelevanceScore.getOrDefault(song.getId(), 0);
                            songRelevanceScore.put(song.getId(), currentScore + 1);
                        }
                    }
                }
            }
            
            if (candidateSongs.isEmpty()) {
                log.debug("未找到与搜索历史相关的歌曲");
                return new RecallResult(getStrategyName(), recalledSongs, 0, STRATEGY_WEIGHT);
            }
            
            // 4. 按相关度得分降序排序
            List<Map.Entry<Integer, Integer>> sortedSongs = new ArrayList<>(songRelevanceScore.entrySet());
            sortedSongs.sort((e1, e2) -> e2.getValue().compareTo(e1.getValue()));
            
            // 5. 选取前MAX_RECALL_COUNT首
            int limit = Math.min(sortedSongs.size(), MAX_RECALL_COUNT);
            for (int i = 0; i < limit; i++) {
                Integer songId = sortedSongs.get(i).getKey();
                Song song = candidateSongs.get(songId);
                if (song != null) {
                    recalledSongs.add(song);
                }
            }
            
            log.debug("搜索历史召回 {} 首歌曲", recalledSongs.size());
            
        } catch (Exception e) {
            log.error("搜索历史召回失败，userId={}", userId, e);
            return RecallResult.failed(getStrategyName(), STRATEGY_WEIGHT, e.getMessage());
        }
        
        return new RecallResult(getStrategyName(), recalledSongs, recalledSongs.size(), STRATEGY_WEIGHT);
    }

    @Override
    public String getStrategyName() {
        return "search_history";
    }

    @Override
    public Double getWeight() {
        return STRATEGY_WEIGHT;
    }
}

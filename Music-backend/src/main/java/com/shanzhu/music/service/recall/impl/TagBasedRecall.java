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
 * 标签召回策略
 * 基于用户收藏歌曲的标签进行推荐
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TagBasedRecall implements RecallStrategy {
    
    private final SongMapper songMapper;
    private final CollectMapper collectMapper;
    
    // 策略权重
    private static final Double STRATEGY_WEIGHT = 0.20;
    // 最大召回数量
    private static final Integer MAX_RECALL_COUNT = 25;

    @Override
    public RecallResult recall(Integer userId) {
        log.debug("执行标签召回，userId={}", userId);
        
        List<Song> recalledSongs = new ArrayList<>();
        
        try {
            // 1. 获取用户收藏的歌曲
            List<Song> userCollectedSongs = songMapper.getCollectByUserID(userId);
            
            if (userCollectedSongs == null || userCollectedSongs.isEmpty()) {
                log.debug("用户没有收藏歌曲");
                return new RecallResult(getStrategyName(), recalledSongs, 0, STRATEGY_WEIGHT);
            }
            
            // 2. 提取用户喜欢的标签（从歌曲简介中）
            Map<String, Integer> tagFrequency = new HashMap<>();
            for (Song song : userCollectedSongs) {
                String introduction = song.getIntroduction();
                if (introduction != null && !introduction.isEmpty()) {
                    // 简单分词：按逗号、空格分隔
                    String[] tags = introduction.split("[,，\\s]+");
                    for (String tag : tags) {
                        tag = tag.trim();
                        if (!tag.isEmpty() && tag.length() > 1) { // 过滤单字符
                            tagFrequency.merge(tag, 1, Integer::sum);
                        }
                    }
                }
            }
            
            if (tagFrequency.isEmpty()) {
                log.debug("无法提取用户偏好标签");
                return new RecallResult(getStrategyName(), recalledSongs, 0, STRATEGY_WEIGHT);
            }
            
            log.debug("用户偏好标签数量: {}", tagFrequency.size());
            
            // 3. 选取出现频率最高的前5个标签
            List<Map.Entry<String, Integer>> sortedTags = new ArrayList<>(tagFrequency.entrySet());
            sortedTags.sort((e1, e2) -> e2.getValue().compareTo(e1.getValue()));
            
            int topTagCount = Math.min(sortedTags.size(), 5);
            Set<String> preferredTags = new HashSet<>();
            for (int i = 0; i < topTagCount; i++) {
                preferredTags.add(sortedTags.get(i).getKey());
            }
            
            log.debug("使用Top {} 标签: {}", topTagCount, preferredTags);
            
            // 4. 根据标签召回相关歌曲
            Set<Integer> collectedSongIds = userCollectedSongs.stream()
                .map(Song::getId)
                .collect(Collectors.toSet());
            
            Map<Integer, Song> candidateSongs = new HashMap<>();
            Map<Integer, Integer> songRelevanceScore = new HashMap<>();
            
            for (String tag : preferredTags) {
                List<Song> tagSongs = songMapper.getSongsBySimilarTags(tag, MAX_RECALL_COUNT);
                
                if (tagSongs != null && !tagSongs.isEmpty()) {
                    for (Song song : tagSongs) {
                        // 排除用户已收藏的歌曲
                        if (!collectedSongIds.contains(song.getId())) {
                            candidateSongs.putIfAbsent(song.getId(), song);
                            
                            // 累加相关度得分（匹配的标签数）
                            int currentScore = songRelevanceScore.getOrDefault(song.getId(), 0);
                            songRelevanceScore.put(song.getId(), currentScore + 1);
                        }
                    }
                }
            }
            
            // 5. 按相关度得分降序排序
            List<Map.Entry<Integer, Integer>> sortedSongs = new ArrayList<>(songRelevanceScore.entrySet());
            sortedSongs.sort((e1, e2) -> e2.getValue().compareTo(e1.getValue()));
            
            // 6. 选取前MAX_RECALL_COUNT首
            int limit = Math.min(sortedSongs.size(), MAX_RECALL_COUNT);
            for (int i = 0; i < limit; i++) {
                Integer songId = sortedSongs.get(i).getKey();
                Song song = candidateSongs.get(songId);
                if (song != null) {
                    recalledSongs.add(song);
                }
            }
            
            log.debug("标签召回 {} 首歌曲", recalledSongs.size());
            
        } catch (Exception e) {
            log.error("标签召回失败，userId={}", userId, e);
        }
        
        return new RecallResult(getStrategyName(), recalledSongs, recalledSongs.size(), STRATEGY_WEIGHT);
    }

    @Override
    public String getStrategyName() {
        return "tag_based";
    }

    @Override
    public Double getWeight() {
        return STRATEGY_WEIGHT;
    }
}

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
 * 内容标签召回策略
 * 基于用户收藏歌曲的歌手/风格进行推荐
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ContentBasedRecall implements RecallStrategy {
    
    private final CollectMapper collectMapper;
    private final SongMapper songMapper;
    
    // 策略权重
    private static final Double STRATEGY_WEIGHT = 0.25;
    // 最大召回数量
    private static final Integer MAX_RECALL_COUNT = 30;

    @Override
    public RecallResult recall(Integer userId) {
        log.debug("执行内容标签召回，userId={}", userId);
        
        List<Song> recalledSongs = new ArrayList<>();
        
        try {
            // 1. 获取用户收藏的歌曲
            List<Song> userCollectedSongs = songMapper.getCollectByUserID(userId);
            
            if (userCollectedSongs == null || userCollectedSongs.isEmpty()) {
                log.debug("用户没有收藏歌曲");
                return new RecallResult(getStrategyName(), recalledSongs, 0, STRATEGY_WEIGHT);
            }
            
            // 2. 提取用户喜欢的歌手ID
            Set<Integer> preferredSingerIds = userCollectedSongs.stream()
                .map(Song::getSingerId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
            
            if (preferredSingerIds.isEmpty()) {
                log.debug("无法提取用户偏好歌手");
                return new RecallResult(getStrategyName(), recalledSongs, 0, STRATEGY_WEIGHT);
            }
            
            log.debug("用户偏好歌手数量: {}", preferredSingerIds.size());
            
            // 3. 召回这些歌手的其他歌曲（排除用户已收藏的）
            Set<Integer> collectedSongIds = userCollectedSongs.stream()
                .map(Song::getId)
                .collect(Collectors.toSet());
            
            Map<Integer, Song> candidateSongs = new HashMap<>();
            
            for (Integer singerId : preferredSingerIds) {
                List<Song> singerSongs = songMapper.songOfSingerId(singerId);
                
                if (singerSongs != null && !singerSongs.isEmpty()) {
                    for (Song song : singerSongs) {
                        // 排除用户已收藏的歌曲
                        if (!collectedSongIds.contains(song.getId())) {
                            candidateSongs.putIfAbsent(song.getId(), song);
                        }
                    }
                }
            }
            
            // 4. 随机打乱并选取前MAX_RECALL_COUNT首
            List<Song> songList = new ArrayList<>(candidateSongs.values());
            Collections.shuffle(songList);
            
            int limit = Math.min(songList.size(), MAX_RECALL_COUNT);
            recalledSongs = songList.subList(0, limit);
            
            log.debug("内容标签召回 {} 首歌曲", recalledSongs.size());
            
        } catch (Exception e) {
            log.error("内容标签召回失败，userId={}", userId, e);
        }
        
        return new RecallResult(getStrategyName(), recalledSongs, recalledSongs.size(), STRATEGY_WEIGHT);
    }

    @Override
    public String getStrategyName() {
        return "content_based";
    }

    @Override
    public Double getWeight() {
        return STRATEGY_WEIGHT;
    }
}

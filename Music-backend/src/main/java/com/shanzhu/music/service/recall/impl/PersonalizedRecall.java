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
 * 个性化召回策略
 * 基于用户最近的收藏行为进行推荐
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PersonalizedRecall implements RecallStrategy {
    
    private final SongMapper songMapper;
    
    // 策略权重
    private static final Double STRATEGY_WEIGHT = 0.2;
    // 最大召回数量
    private static final Integer MAX_RECALL_COUNT = 25;

    @Override
    public RecallResult recall(Integer userId) {
        log.debug("执行个性化召回，userId={}", userId);
        
        List<Song> recalledSongs = new ArrayList<>();
        
        try {
            // 1. 获取用户最近收藏的歌曲（取最后10首作为兴趣代表）
            List<Song> userCollectedSongs = songMapper.getCollectByUserID(userId);
            
            if (userCollectedSongs == null || userCollectedSongs.isEmpty()) {
                log.debug("用户没有收藏歌曲");
                return new RecallResult(getStrategyName(), recalledSongs, 0, STRATEGY_WEIGHT);
            }
            
            // 2. 选取最近的歌曲作为种子（简化：取前5首）
            int seedCount = Math.min(userCollectedSongs.size(), 5);
            List<Song> seedSongs = userCollectedSongs.subList(0, seedCount);
            
            Set<Integer> collectedSongIds = userCollectedSongs.stream()
                .map(Song::getId)
                .collect(Collectors.toSet());
            
            // 3. 基于种子歌曲的特征进行扩展推荐
            Map<Integer, Song> candidateSongs = new HashMap<>();
            
            for (Song seedSong : seedSongs) {
                // 同歌手的其他歌曲
                if (seedSong.getSingerId() != null) {
                    List<Song> sameSingerSongs = songMapper.songOfSingerId(seedSong.getSingerId());
                    if (sameSingerSongs != null) {
                        for (Song song : sameSingerSongs) {
                            if (!collectedSongIds.contains(song.getId())) {
                                candidateSongs.putIfAbsent(song.getId(), song);
                            }
                        }
                    }
                }
            }
            
            // 4. 按相关性排序并选取前MAX_RECALL_COUNT首
            List<Song> songList = new ArrayList<>(candidateSongs.values());
            Collections.shuffle(songList); // 简化版：随机打乱
            
            int limit = Math.min(songList.size(), MAX_RECALL_COUNT);
            recalledSongs = songList.subList(0, limit);
            
            log.debug("个性化召回 {} 首歌曲", recalledSongs.size());
            
        } catch (Exception e) {
            log.error("个性化召回失败，userId={}", userId, e);
        }
        
        return new RecallResult(getStrategyName(), recalledSongs, recalledSongs.size(), STRATEGY_WEIGHT);
    }

    @Override
    public String getStrategyName() {
        return "personalized";
    }

    @Override
    public Double getWeight() {
        return STRATEGY_WEIGHT;
    }
}

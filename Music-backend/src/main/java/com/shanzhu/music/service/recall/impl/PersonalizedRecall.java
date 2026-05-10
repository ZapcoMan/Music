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
            
            // 3. 基于种子歌曲的特征进行扩展推荐，并计算相关度得分
            Map<Integer, Song> candidateSongs = new HashMap<>();
            Map<Integer, Integer> songRelevanceScore = new HashMap<>();
            
            for (Song seedSong : seedSongs) {
                // 同歌手的其他歌曲
                if (seedSong.getSingerId() != null) {
                    List<Song> sameSingerSongs = songMapper.songOfSingerId(seedSong.getSingerId());
                    if (sameSingerSongs != null) {
                        for (Song song : sameSingerSongs) {
                            if (!collectedSongIds.contains(song.getId())) {
                                candidateSongs.putIfAbsent(song.getId(), song);
                                
                                // 累加相关度得分（该歌手作为种子歌曲的次数）
                                int currentScore = songRelevanceScore.getOrDefault(song.getId(), 0);
                                songRelevanceScore.put(song.getId(), currentScore + 1);
                            }
                        }
                    }
                }
            }
            
            // 4. 按相关度得分降序排序（得分越高说明与用户最近喜好越相关）
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

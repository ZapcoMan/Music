package com.shanzhu.music.service.recall.impl;

import com.shanzhu.music.entity.dto.RecallResult;
import com.shanzhu.music.entity.po.Song;
import com.shanzhu.music.entity.vo.TaggedSongVo;
import com.shanzhu.music.entity.po.Tag;
import com.shanzhu.music.mapper.CollectMapper;
import com.shanzhu.music.mapper.SongMapper;
import com.shanzhu.music.mapper.TagMapper;
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
    private final TagMapper tagMapper;  // ✅ 新增标签Mapper
    
    // 策略权重
    private static final Double STRATEGY_WEIGHT = 0.20;
    // 最大召回数量
    private static final Integer MAX_RECALL_COUNT = 25;

    @Override
    public RecallResult recall(Integer userId) {
        log.debug("执行标签召回（结构化标签），userId={}", userId);
        
        List<Song> recalledSongs = new ArrayList<>();
        
        try {
            // 1. 获取用户收藏的歌曲
            List<Song> userCollectedSongs = songMapper.getCollectByUserID(userId);
            
            if (userCollectedSongs == null || userCollectedSongs.isEmpty()) {
                log.debug("用户没有收藏歌曲");
                return new RecallResult(getStrategyName(), recalledSongs, 0, STRATEGY_WEIGHT);
            }
            
            // 2. ✅ 提取用户喜欢的标签ID（从数据库精确查询）
            Map<Integer, Integer> tagFrequency = new HashMap<>();
            for (Song song : userCollectedSongs) {
                List<Tag> tags = tagMapper.selectTagsBySongId(song.getId());
                if (tags != null && !tags.isEmpty()) {
                    for (Tag tag : tags) {
                        tagFrequency.merge(tag.getId(), 1, Integer::sum);
                    }
                }
            }
            
            if (tagFrequency.isEmpty()) {
                log.debug("无法提取用户偏好标签");
                return new RecallResult(getStrategyName(), recalledSongs, 0, STRATEGY_WEIGHT);
            }
            
            log.debug("用户偏好标签数量: {}", tagFrequency.size());
            
            // 3. 选取出现频率最高的前5个标签ID
            List<Map.Entry<Integer, Integer>> sortedTags = new ArrayList<>(tagFrequency.entrySet());
            sortedTags.sort((e1, e2) -> e2.getValue().compareTo(e1.getValue()));
            
            int topTagCount = Math.min(sortedTags.size(), 5);
            List<Integer> preferredTagIds = new ArrayList<>();
            for (int i = 0; i < topTagCount; i++) {
                preferredTagIds.add(sortedTags.get(i).getKey());
            }
            
            log.debug("使用Top {} 标签ID: {}", topTagCount, preferredTagIds);
            
            // 4. ✅ 批量查询：一次 JOIN 查询所有相关歌曲（高性能）
            Set<Integer> collectedSongIds = userCollectedSongs.stream()
                .map(Song::getId)
                .collect(Collectors.toSet());
            
            // 根据多个标签ID查询相关歌曲，并按匹配标签数排序
            List<TaggedSongVo> candidateSongs = tagMapper.selectSongsByTagIds(preferredTagIds, MAX_RECALL_COUNT * 2);
            
            // 5. 排除用户已收藏的歌曲
            // TaggedSongVo 是 Song 的子类，直接作为召回结果携带 matchCount，
            // 便于后续解释"这首歌为什么被推荐"
            for (TaggedSongVo candidate : candidateSongs) {
                if (collectedSongIds.contains(candidate.getId())) {
                    continue;
                }
                recalledSongs.add(candidate);
                if (recalledSongs.size() >= MAX_RECALL_COUNT) {
                    break;
                }
            }
            
            log.debug("标签召回 {} 首歌曲", recalledSongs.size());
            
        } catch (Exception e) {
            log.error("标签召回失败，userId={}", userId, e);
            return RecallResult.failed(getStrategyName(), STRATEGY_WEIGHT, e.getMessage());
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

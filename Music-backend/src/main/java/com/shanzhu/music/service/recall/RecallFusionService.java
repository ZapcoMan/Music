package com.shanzhu.music.service.recall;

import com.shanzhu.music.entity.dto.RecallResult;
import com.shanzhu.music.entity.po.Consumer;
import com.shanzhu.music.entity.po.Song;
import com.shanzhu.music.mapper.CollectMapper;
import com.shanzhu.music.mapper.ConsumerMapper;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 多路召回融合服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RecallFusionService {
    
    private final List<RecallStrategy> recallStrategies;
    private final CollectMapper collectMapper;
    private final ConsumerMapper consumerMapper;
    
    /**
     * 执行多路召回并融合
     *
     * @param userId 用户ID
     * @param topN   最终返回的推荐数量
     * @return 融合后的推荐歌曲列表
     */
    public List<Song> fuseRecall(Integer userId, int topN) {
        log.info("开始多路召回融合，userId={}, topN={}", userId, topN);
        
        long startTime = System.currentTimeMillis();
        
        // 1. 检查是否是冷启动用户（新用户没有收藏记录）
        List<Song> userCollects = collectMapper.selectByUserId(userId);
        if (userCollects == null || userCollects.isEmpty()) {
            log.info("冷启动用户，使用热门召回");
            return handleColdStart(userId, topN);
        }
        
        // 2. 并行执行各路召回策略
        List<RecallResult> recallResults = new ArrayList<>();
        for (RecallStrategy strategy : recallStrategies) {
            try {
                RecallResult result = strategy.recall(userId);
                recallResults.add(result);
                log.debug("策略 {} 召回 {} 首歌曲，权重 {}", 
                    result.getStrategyName(), result.getCount(), result.getWeight());
            } catch (Exception e) {
                log.error("召回策略 {} 执行失败", strategy.getStrategyName(), e);
            }
        }
        
        // 3. 融合排序（加权得分）
        List<Song> fusedSongs = fusionRanking(recallResults, topN);
        
        long endTime = System.currentTimeMillis();
        log.info("多路召回融合完成，耗时 {}ms，最终推荐 {} 首歌曲", 
            endTime - startTime, fusedSongs.size());
        
        return fusedSongs;
    }
    
    /**
     * 执行多路召回并融合（带召回信息）
     * 用于特征工程提取
     *
     * @param userId 用户ID
     * @param topN   最终返回的推荐数量
     * @return 带召回信息的歌曲列表
     */
    public List<SongWithRecallInfo> fuseRecallWithInfo(Integer userId, int topN) {
        log.info("开始多路召回融合（带信息），userId={}, topN={}", userId, topN);
        
        // 1. 检查是否是冷启动用户
        List<Song> userCollects = collectMapper.selectByUserId(userId);
        if (userCollects == null || userCollects.isEmpty()) {
            log.info("冷启动用户，使用热门召回");
            List<Song> coldStartSongs = handleColdStart(userId, topN);
            return coldStartSongs.stream()
                .map(song -> new SongWithRecallInfo(song, "popular", 1, 0.15))
                .collect(Collectors.toList());
        }
        
        // 2. 执行各路召回策略
        List<RecallResult> recallResults = new ArrayList<>();
        for (RecallStrategy strategy : recallStrategies) {
            try {
                recallResults.add(strategy.recall(userId));
            } catch (Exception e) {
                log.error("召回策略 {} 执行异常", strategy.getStrategyName(), e);
                recallResults.add(RecallResult.failed(
                    strategy.getStrategyName(), strategy.getWeight(), e.getMessage()));
            }
        }

        // 显式汇总不可用的策略，避免"SQL 报错""表不存在""确实无数据"三种情况
        // 对外呈现为同一种静默的空结果
        List<String> degraded = recallResults.stream()
            .filter(RecallResult::isFailed)
            .map(r -> r.getStrategyName() + "[" + r.getFailureReason() + "]")
            .toList();
        if (!degraded.isEmpty()) {
            log.warn("用户 {} 召回降级：{}/{} 路不可用 -> {}",
                userId, degraded.size(), recallResults.size(), degraded);
        }
        
        // 3. 融合排序并保留召回信息
        return fusionRankingWithInfo(recallResults, topN);
    }
    
    /**
     * 冷启动处理 - 新用户推荐热门
     */
    private List<Song> handleColdStart(Integer userId, int topN) {
        // 直接用热门召回
        for (RecallStrategy strategy : recallStrategies) {
            if ("popular".equals(strategy.getStrategyName())) {
                RecallResult result = strategy.recall(userId);
                if (result.getSongs() != null && !result.getSongs().isEmpty()) {
                    return result.getSongs().stream().limit(topN).collect(Collectors.toList());
                }
            }
        }
        return new ArrayList<>();
    }
    
    /**
     * 融合排序算法
     * 基于各策略的权重对歌曲进行加权排序
     *
     * @param recallResults 各路召回结果
     * @param topN          最终返回数量
     * @return 排序后的歌曲列表
     */
    private List<Song> fusionRanking(List<RecallResult> recallResults, int topN) {
        // 使用Map存储歌曲的最终得分
        Map<Integer, Double> songScoreMap = new HashMap<>();
        Map<Integer, Song> songMap = new HashMap<>();
        // 记录每首歌曲的最佳召回策略和排名（用于特征工程）
        Map<Integer, String> songBestStrategyMap = new HashMap<>();
        Map<Integer, Integer> songBestRankMap = new HashMap<>();
        Map<Integer, Double> songBestWeightMap = new HashMap<>();
        
        // 遍历每路召回结果
        for (RecallResult result : recallResults) {
            if (result.getSongs() == null || result.getSongs().isEmpty()) {
                continue;
            }
            
            double strategyWeight = result.getWeight() != null ? result.getWeight() : 1.0;
            int rank = 1;
            
            // 根据在召回列表中的位置计算位置权重
            for (Song song : result.getSongs()) {
                // 位置衰减因子：排名越靠前，权重越高
                // 使用换底公式: log2(x) = ln(x) / ln(2)
                double positionWeight = 1.0 / (Math.log(rank + 1) / Math.log(2));
                
                // 综合得分 = 策略权重 × 位置权重
                double score = strategyWeight * positionWeight;
                
                // 累加得分（同一歌曲可能被多路召回）
                double currentScore = songScoreMap.getOrDefault(song.getId(), 0.0);
                songScoreMap.put(song.getId(), currentScore + score);
                
                // 保存歌曲对象
                songMap.putIfAbsent(song.getId(), song);
                
                // 记录最佳召回策略（得分最高的那一路）
                if (!songBestStrategyMap.containsKey(song.getId()) || score > songScoreMap.get(song.getId()) / 2) {
                    songBestStrategyMap.put(song.getId(), result.getStrategyName());
                    songBestRankMap.put(song.getId(), rank);
                    songBestWeightMap.put(song.getId(), strategyWeight);
                }
                
                rank++;
            }
        }
        
        // 按综合得分降序排序
        List<Map.Entry<Integer, Double>> sortedSongs = new ArrayList<>(songScoreMap.entrySet());
        sortedSongs.sort((e1, e2) -> Double.compare(e2.getValue(), e1.getValue()));
        
        // 选取前topN首
        List<Song> result = new ArrayList<>();
        int limit = Math.min(sortedSongs.size(), topN);
        
        for (int i = 0; i < limit; i++) {
            Integer songId = sortedSongs.get(i).getKey();
            Song song = songMap.get(songId);
            if (song != null) {
                result.add(song);
            }
        }
        
        log.debug("融合排序完成，候选歌曲 {} 首，最终返回 {} 首", 
            songScoreMap.size(), result.size());
        
        return result;
    }
    
    /**
     * 融合排序算法（保留召回信息）
     */
    private List<SongWithRecallInfo> fusionRankingWithInfo(List<RecallResult> recallResults, int topN) {
        Map<Integer, Double> songScoreMap = new HashMap<>();
        Map<Integer, Song> songMap = new HashMap<>();
        Map<Integer, String> songBestStrategyMap = new HashMap<>();
        Map<Integer, Integer> songBestRankMap = new HashMap<>();
        Map<Integer, Double> songBestWeightMap = new HashMap<>();
        
        for (RecallResult result : recallResults) {
            if (result.getSongs() == null || result.getSongs().isEmpty()) {
                continue;
            }
            
            double strategyWeight = result.getWeight() != null ? result.getWeight() : 1.0;
            int rank = 1;
            
            for (Song song : result.getSongs()) {
                double positionWeight = 1.0 / (Math.log(rank + 1) / Math.log(2));
                double score = strategyWeight * positionWeight;
                
                double currentScore = songScoreMap.getOrDefault(song.getId(), 0.0);
                songScoreMap.put(song.getId(), currentScore + score);
                songMap.putIfAbsent(song.getId(), song);
                
                // 记录最佳召回策略
                if (!songBestStrategyMap.containsKey(song.getId())) {
                    songBestStrategyMap.put(song.getId(), result.getStrategyName());
                    songBestRankMap.put(song.getId(), rank);
                    songBestWeightMap.put(song.getId(), strategyWeight);
                }
                
                rank++;
            }
        }
        
        // 按综合得分降序排序
        List<Map.Entry<Integer, Double>> sortedSongs = new ArrayList<>(songScoreMap.entrySet());
        sortedSongs.sort((e1, e2) -> Double.compare(e2.getValue(), e1.getValue()));
        
        // 构建带召回信息的歌曲列表
        List<SongWithRecallInfo> result = new ArrayList<>();
        int limit = Math.min(sortedSongs.size(), topN);
        
        for (int i = 0; i < limit; i++) {
            Integer songId = sortedSongs.get(i).getKey();
            Song song = songMap.get(songId);
            if (song != null) {
                String strategy = songBestStrategyMap.getOrDefault(songId, "unknown");
                Integer rank = songBestRankMap.getOrDefault(songId, 999);
                Double weight = songBestWeightMap.getOrDefault(songId, 0.1);
                result.add(new SongWithRecallInfo(song, strategy, rank, weight));
            }
        }
        
        return result;
    }
    
    /**
     * 带召回信息的歌曲（用于特征工程）
     */
    @Data
    @AllArgsConstructor
    public static class SongWithRecallInfo {
        private Song song;
        private String recallStrategy;  // 召回策略名称
        private Integer recallRank;      // 召回排名
        private Double recallWeight;     // 召回权重
    }
}

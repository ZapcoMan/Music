package com.shanzhu.music.service.recall;

import com.shanzhu.music.config.RecommendationProperties;
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
    private final RecommendationProperties properties;

    /**
     * 冷启动兜底策略名
     */
    private static final String POPULAR_STRATEGY = "popular";

    /**
     * 固定策略遍历顺序：先按权重降序，权重相同再按名称字典序。
     * recallStrategies 由 Spring 注入为 List，其顺序依赖 bean 注册次序，
     * 而融合产出的 strategy/rank 会作为特征进入打分，因此顺序必须确定。
     * 排序使用配置覆盖后的权重，与融合实际采用的权重保持一致。
     */
    private List<RecallStrategy> orderedStrategies() {
        return recallStrategies.stream()
            .sorted(Comparator.comparingDouble(this::effectiveWeight).reversed()
                    .thenComparing(RecallStrategy::getStrategyName))
            .toList();
    }

    /**
     * 生效权重 = 配置值，未配置时回落到策略自带默认值
     */
    private double effectiveWeight(RecallStrategy strategy) {
        return properties.strategyWeight(strategy.getStrategyName(), strategy.getWeight());
    }

    /**
     * 按策略名取生效权重，避免冷启动分支把数值写死
     */
    private double weightOf(List<RecallStrategy> orderedStrategies, String strategyName) {
        return orderedStrategies.stream()
            .filter(s -> strategyName.equals(s.getStrategyName()))
            .mapToDouble(this::effectiveWeight)
            .findFirst()
            .orElse(0.0);
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
        
        List<RecallStrategy> orderedStrategies = orderedStrategies();

        // 1. 检查是否是冷启动用户
        List<Song> userCollects = collectMapper.selectByUserId(userId);
        if (userCollects == null || userCollects.isEmpty()) {
            log.info("冷启动用户，使用热门召回");
            List<Song> coldStartSongs = handleColdStart(orderedStrategies, userId, topN);
            double popularWeight = weightOf(orderedStrategies, POPULAR_STRATEGY);
            List<SongWithRecallInfo> coldStart = new ArrayList<>();
            int coldStartRank = 1;
            for (Song song : coldStartSongs) {
                coldStart.add(new SongWithRecallInfo(song, POPULAR_STRATEGY, coldStartRank++, popularWeight));
            }
            return coldStart;
        }
        
        // 2. 执行各路召回策略
        List<RecallResult> recallResults = new ArrayList<>();
        for (RecallStrategy strategy : orderedStrategies) {
            try {
                RecallResult result = strategy.recall(userId);
                // 用配置权重覆盖策略自带默认值，使融合得分与召回特征同源
                result.setWeight(effectiveWeight(strategy));
                recallResults.add(result);
            } catch (Exception e) {
                // 这一路已降级为空结果、不再影响整体推荐，所以 ERROR 只留可定位的一行摘要；
                // 完整调用栈降到 debug，免得单测与下游偶发抖动把整屏刷成 stack trace。
                log.error("召回策略 {} 执行异常，本路降级：{}: {}",
                    strategy.getStrategyName(), e.getClass().getSimpleName(), e.getMessage());
                log.debug("召回策略 {} 异常详情", strategy.getStrategyName(), e);
                recallResults.add(RecallResult.failed(
                    strategy.getStrategyName(), effectiveWeight(strategy), e.toString()));
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
     * 冷启动处理 - 无收藏用户退化为热门召回
     *
     * 这里不走多路融合的 try/catch，热门召回自己异常（SQL 报错、表缺失等）
     * 也必须就地降级为空结果，否则一路失败会把整个推荐流程抛穿到调用方。
     */
    private List<Song> handleColdStart(List<RecallStrategy> orderedStrategies, Integer userId, int topN) {
        for (RecallStrategy strategy : orderedStrategies) {
            if (POPULAR_STRATEGY.equals(strategy.getStrategyName())) {
                try {
                    RecallResult result = strategy.recall(userId);
                    if (result.getSongs() != null && !result.getSongs().isEmpty()) {
                        return result.getSongs().stream().limit(topN).collect(Collectors.toList());
                    }
                } catch (Exception e) {
                    // 与多路召回那条 catch 同一口径：就地降级为空结果，ERROR 只留一行摘要，完整栈看 debug
                    log.error("冷启动用户 {} 的热门召回执行异常，返回空推荐：{}: {}",
                        userId, e.getClass().getSimpleName(), e.getMessage());
                    log.debug("冷启动用户 {} 的热门召回异常详情", userId, e);
                }
                break;
            }
        }
        log.warn("冷启动用户返回空推荐：热门策略 {} 未注册或无可用结果", POPULAR_STRATEGY);
        return new ArrayList<>();
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
        // 已选"最佳策略"对应的归一化位置，用于跨策略比较
        Map<Integer, Double> songBestPositionMap = new HashMap<>();
        
        for (RecallResult result : recallResults) {
            if (result.getSongs() == null || result.getSongs().isEmpty()) {
                continue;
            }
            
            double strategyWeight = result.getWeight() != null ? result.getWeight() : 1.0;
            // 各路召回规模不同（50/30/25/20），原始名次跨策略不可比，
            // 需按 rank / 本路召回总数 归一化后才能判断"最佳策略"
            int recallSize = Math.max(
                result.getCount() != null ? result.getCount() : result.getSongs().size(), 1);
            int rank = 1;
            
            for (Song song : result.getSongs()) {
                double positionWeight = 1.0 / (Math.log(rank + 1) / Math.log(2));
                double score = strategyWeight * positionWeight;
                
                double currentScore = songScoreMap.getOrDefault(song.getId(), 0.0);
                songScoreMap.put(song.getId(), currentScore + score);
                songMap.putIfAbsent(song.getId(), song);
                
                // 最佳策略 = 归一化位置最靠前的一路；
                // 并列时保留先遍历到的，而遍历顺序已按权重降序固定
                double normalizedPosition = (double) rank / recallSize;
                Double bestPosition = songBestPositionMap.get(song.getId());
                if (bestPosition == null || normalizedPosition < bestPosition) {
                    songBestPositionMap.put(song.getId(), normalizedPosition);
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

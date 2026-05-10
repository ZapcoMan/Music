package com.shanzhu.music.service.ranking;

import com.shanzhu.music.entity.po.Song;
import com.shanzhu.music.service.recall.RecallFusionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 完整排序服务
 * 流程：多路召回 → 特征工程 → GBDT排序 → 返回TopN
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RankingService {
    
    private final RecallFusionService recallFusionService;
    private final FeatureEngineeringService featureEngineeringService;
    private final GBDTRankingService gbdtRankingService;
    
    /**
     * 完整的推荐流程
     *
     * @param userId 用户ID
     * @param topN   最终返回数量
     * @return 精排后的推荐歌曲列表
     */
    public List<Song> recommend(Integer userId, int topN) {
        log.info("========== 开始完整推荐流程 ==========");
        log.info("用户ID: {}, 目标返回数量: {}", userId, topN);
        
        long startTime = System.currentTimeMillis();
        
        // 第1步：多路召回（召回层）
        log.info("【第1步】执行多路召回");
        List<RecallFusionService.SongWithRecallInfo> recallSongsWithInfo = 
            recallFusionService.fuseRecallWithInfo(userId, topN * 3); // 召回更多候选
        log.info("召回完成，候选歌曲数量: {}", recallSongsWithInfo.size());
        
        if (recallSongsWithInfo.isEmpty()) {
            log.warn("召回结果为空，返回空列表");
            return List.of();
        }
        
        // 第2步：特征工程
        log.info("【第2步】提取特征");
        List<FeatureEngineeringService.SongWithFeature> songsWithFeatures = 
            featureEngineeringService.extractFeaturesWithRecallInfo(userId, recallSongsWithInfo);
        log.info("特征提取完成，有效特征数量: {}", songsWithFeatures.size());
        
        if (songsWithFeatures.isEmpty()) {
            log.warn("特征提取失败，返回空列表");
            return List.of();
        }
        
        // 第3步：GBDT排序（排序层）
        log.info("【第3步】GBDT精排");
        List<Song> rankedSongs = gbdtRankingService.rank(songsWithFeatures);
        log.info("排序完成");
        
        // 第4步：截取TopN
        List<Song> finalResult = rankedSongs.stream()
            .limit(topN)
            .toList();
        
        long endTime = System.currentTimeMillis();
        log.info("========== 推荐流程完成 ==========");
        log.info("总耗时: {}ms, 最终推荐数量: {}", endTime - startTime, finalResult.size());
        
        return finalResult;
    }
}

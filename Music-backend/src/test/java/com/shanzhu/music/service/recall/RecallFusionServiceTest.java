package com.shanzhu.music.service.recall;

import com.shanzhu.music.config.RecommendationProperties;
import com.shanzhu.music.entity.dto.RecallResult;
import com.shanzhu.music.entity.po.Song;
import com.shanzhu.music.mapper.CollectMapper;
import com.shanzhu.music.mapper.ConsumerMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 多路召回融合的语义回归。
 *
 * 这个类此前只有 32.3% 行覆盖、38 个分支里 29 个从未执行——而它是所有推荐的聚合入口，
 * 出错影响的是整个推荐结果而不是某一路。它又是纯内存计算，不需要数据库和 Redis，
 * 是最该有单元测试却最久没有的一块。
 */
class RecallFusionServiceTest {

    private CollectMapper collectMapper;
    private RecommendationProperties properties;

    @BeforeEach
    void setUp() {
        collectMapper = mock(CollectMapper.class);
        properties = mock(RecommendationProperties.class);
        // 让"生效权重"等于策略自带权重，期望值才可以用已知数字推算而不依赖配置内容
        when(properties.strategyWeight(any(), anyDouble()))
                .thenAnswer(invocation -> invocation.getArgument(1));
    }

    private Song song(int id) {
        Song s = new Song();
        s.setId(id);
        return s;
    }

    private RecallStrategy stub(String name, double weight, List<Song> songs) {
        RecallStrategy strategy = mock(RecallStrategy.class);
        when(strategy.getStrategyName()).thenReturn(name);
        when(strategy.getWeight()).thenReturn(weight);
        when(strategy.recall(any())).thenReturn(new RecallResult(name, songs, songs.size(), weight));
        return strategy;
    }

    private RecallFusionService service(List<RecallStrategy> strategies) {
        return new RecallFusionService(strategies, collectMapper, mock(ConsumerMapper.class), properties);
    }

    private void userHasCollects(int userId) {
        when(collectMapper.selectByUserId(userId)).thenReturn(Arrays.asList(song(999)));
    }

    @Test
    @DisplayName("被多路命中的歌曲排在只被一路命中的前面")
    void songHitByMoreStrategiesRanksHigher() {
        userHasCollects(7);
        Song both = song(1);
        Song single = song(2);

        List<RecallFusionService.SongWithRecallInfo> result = service(Arrays.asList(
                stub("collaborative_filtering", 0.40, Arrays.asList(both, single)),
                stub("content_based", 0.25, Arrays.asList(single, both))
        )).fuseRecallWithInfo(7, 5);

        assertEquals(2, result.size());
        assertEquals(1, result.get(0).getSong().getId(),
                "两路都命中的歌曲应当排在只被一路命中的前面");
    }

    /**
     * 冷启动分支此前从未执行过。它决定"新用户打开首页看到什么"，
     * 静默返回空列表和退化成热门，是两种完全不同的产品行为。
     */
    @Test
    @DisplayName("无收藏用户退化为热门召回而不是空结果")
    void coldStartFallsBackToPopular() {
        when(collectMapper.selectByUserId(8)).thenReturn(Collections.emptyList());

        List<RecallFusionService.SongWithRecallInfo> result = service(Arrays.asList(
                stub("collaborative_filtering", 0.40, Arrays.asList(song(100))),
                stub("popular", 0.15, Arrays.asList(song(11), song(12)))
        )).fuseRecallWithInfo(8, 1);

        assertEquals(1, result.size(), "冷启动应只取热门结果且受 topN 限制");
        assertEquals(11, result.get(0).getSong().getId());
        assertEquals("popular", result.get(0).getRecallStrategy(),
                "冷启动的归因策略必须是 popular，它还要作为特征进入精排");
    }

    /**
     * 冷启动时若热门策略本身没注册，必须明确返回空而不是抛异常或悄悄给出别人的结果。
     */
    @Test
    @DisplayName("冷启动缺少热门策略时返回空结果而非异常")
    void coldStartWithoutPopularStrategyReturnsEmpty() {
        when(collectMapper.selectByUserId(9)).thenReturn(null);

        assertTrue(service(Arrays.asList(stub("tag_based", 0.2, Arrays.asList(song(1)))))
                .fuseRecallWithInfo(9, 5).isEmpty());
    }

    @Test
    @DisplayName("单路召回抛异常不应让整体推荐失败")
    void failingStrategyDegradesInsteadOfBreakingAll() {
        userHasCollects(10);
        RecallStrategy broken = mock(RecallStrategy.class);
        when(broken.getStrategyName()).thenReturn("search_history");
        when(broken.getWeight()).thenReturn(0.2);
        when(broken.recall(any())).thenThrow(new RuntimeException("SQL 报错"));

        List<RecallFusionService.SongWithRecallInfo> result = service(Arrays.asList(
                broken, stub("popular", 0.15, Arrays.asList(song(21)))
        )).fuseRecallWithInfo(10, 5);

        assertFalse(result.isEmpty(), "一路失败不该清空整个推荐结果");
        assertEquals(21, result.get(0).getSong().getId());
    }

    /**
     * recallStrategies 由 Spring 注入为 List，其顺序依赖 bean 注册次序；
     * 而融合产出的 strategy 与 rank 会作为特征进入打分。源码里专门固定了遍历顺序，
     * 这条用例就是钉住该约定：注入顺序相反时，结果序列必须逐位相同。
     */
    @Test
    @DisplayName("策略注入顺序不影响融合结果（确定性可复现）")
    void fusionIsDeterministicRegardlessOfInjectionOrder() {
        userHasCollects(11);
        List<RecallStrategy> forward = Arrays.asList(
                stub("popular", 0.15, Arrays.asList(song(1), song(2), song(3))),
                stub("content_based", 0.25, Arrays.asList(song(3), song(1))),
                stub("collaborative_filtering", 0.40, Arrays.asList(song(2), song(3))));
        List<RecallStrategy> reversed = new ArrayList<>(forward);
        Collections.reverse(reversed);

        List<Integer> a = service(forward).fuseRecallWithInfo(11, 3).stream()
                .map(x -> x.getSong().getId()).collect(Collectors.toList());
        List<Integer> b = service(reversed).fuseRecallWithInfo(11, 3).stream()
                .map(x -> x.getSong().getId()).collect(Collectors.toList());

        assertEquals(a, b, "同一组策略换注入顺序后，推荐序列必须完全一致");
        assertFalse(a.isEmpty());
    }

    @Test
    @DisplayName("topN 限制生效且不越界")
    void topNLimitsResultSize() {
        userHasCollects(12);
        List<RecallFusionService.SongWithRecallInfo> result = service(Arrays.asList(
                stub("popular", 0.15, Arrays.asList(song(1), song(2), song(3), song(4), song(5)))
        )).fuseRecallWithInfo(12, 2);

        assertEquals(2, result.size());
    }
}

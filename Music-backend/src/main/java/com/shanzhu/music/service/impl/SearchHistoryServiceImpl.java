package com.shanzhu.music.service.impl;

import com.shanzhu.music.entity.po.SearchHistory;
import com.shanzhu.music.exception.BusinessException;
import com.shanzhu.music.mapper.SearchHistoryMapper;
import com.shanzhu.music.service.SearchHistoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.shanzhu.music.service.ranking.RecommendationCache;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 搜索历史 服务实现
 *
 * 这里原先每个方法都包一层 catch(Exception) 再返回 false / List.of()，
 * 把"SQL 报错""表不存在""确实没有历史"三种情况压成同一种静默空结果：
 * 用户看到的是"我从没搜过东西"，而真实原因可能是数据库连不上。
 * 现在故障一律向上抛，由 GlobalExceptionHandler 统一映射成明确的失败响应。
 *
 * 推荐链路不受影响：召回策略各自在 RecallFusionService 里被 try/catch 兜住并汇总成
 * 降级清单，search_history 这一路挂掉只会让它的权重被标注不可用，不会拖垮整个推荐。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SearchHistoryServiceImpl implements SearchHistoryService {

    /**
     * limit 缺省值与上限：与 Controller 的 @RequestParam 默认值保持同一口径，
     * 上限是为了让"传个 99999"不能把整张历史表拉进响应。
     */
    private static final int DEFAULT_HISTORY_LIMIT = 20;
    private static final int DEFAULT_KEYWORD_LIMIT = 10;
    private static final int MAX_LIMIT = 100;

    /**
     * cleanOldHistory 的缺省保留天数
     */
    private static final int DEFAULT_RETENTION_DAYS = 30;

    private final SearchHistoryMapper searchHistoryMapper;
    private final RecommendationCache recommendationCache;

    @Override
    public Boolean recordSearch(Integer userId, String keywords, Byte searchType) {
        // 身份来自令牌（Controller 用 @RequestAttribute 注入），这里不再兜 userId 为空的情况：
        // 没有令牌根本走不到这个方法
        String trimmed = keywords == null ? "" : keywords.trim();
        if (trimmed.isEmpty()) {
            // BusinessException 而不是 IllegalArgumentException：后者映射成 HTTP 400，
            // 前端 axios 会走 reject，反而看不到这句可读的提示
            throw new BusinessException("搜索关键词不能为空");
        }

        SearchHistory searchHistory = new SearchHistory();
        searchHistory.setUserId(userId);
        searchHistory.setKeywords(trimmed);
        searchHistory.setSearchType(searchType != null ? searchType : 0);

        Integer result = searchHistoryMapper.insert(searchHistory);
        // 搜索历史本身是一路召回的输入，写入后需让推荐缓存失效
        recommendationCache.evict(userId);
        log.debug("记录搜索历史成功，userId={}, keywords={}", userId, trimmed);
        return result != null && result > 0;
    }

    @Override
    public List<SearchHistory> getRecentHistory(Integer userId, Integer limit) {
        return searchHistoryMapper.getRecentHistory(userId, normalizeLimit(limit, DEFAULT_HISTORY_LIMIT));
    }

    @Override
    public List<String> getRecentKeywords(Integer userId, Integer limit) {
        return searchHistoryMapper.getRecentKeywords(userId, normalizeLimit(limit, DEFAULT_KEYWORD_LIMIT));
    }

    @Override
    public Boolean clearHistory(Integer userId) {
        Integer deleted = searchHistoryMapper.deleteByUserId(userId);
        log.info("清空搜索历史，userId={}, deletedCount={}", userId, deleted);
        // 删掉 0 行不是失败——本来就没有历史。真正的失败（SQL 出错）已经抛出去了，
        // 不再用"以 Mapper 实际返回为准"这种恒真表达式假装区分
        return true;
    }

    @Override
    public Boolean cleanOldHistory(Integer userId, Integer days) {
        int retention = days == null || days <= 0 ? DEFAULT_RETENTION_DAYS : days;
        Integer deleted = searchHistoryMapper.deleteOldHistory(userId, retention);
        log.info("清理过期搜索历史，userId={}, days={}, deletedCount={}", userId, retention, deleted);
        return true;
    }

    /**
     * 把非法 limit（null / 非正数）归一到缺省值，并夹到上限，避免一次请求拉全表。
     */
    private static int normalizeLimit(Integer limit, int defaultValue) {
        if (limit == null || limit <= 0) {
            return defaultValue;
        }
        return Math.min(limit, MAX_LIMIT);
    }
}

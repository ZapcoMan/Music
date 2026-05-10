package com.shanzhu.music.service.impl;

import com.shanzhu.music.entity.po.SearchHistory;
import com.shanzhu.music.mapper.SearchHistoryMapper;
import com.shanzhu.music.service.SearchHistoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 搜索历史 服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SearchHistoryServiceImpl implements SearchHistoryService {

    private final SearchHistoryMapper searchHistoryMapper;

    @Override
    public Boolean recordSearch(Integer userId, String keywords, Byte searchType) {
        try {
            if (userId == null || keywords == null || keywords.trim().isEmpty()) {
                return false;
            }

            SearchHistory searchHistory = new SearchHistory();
            searchHistory.setUserId(userId);
            searchHistory.setKeywords(keywords.trim());
            searchHistory.setSearchType(searchType != null ? searchType : 0);

            Integer result = searchHistoryMapper.insert(searchHistory);
            log.debug("记录搜索历史成功，userId={}, keywords={}", userId, keywords);
            return result > 0;
        } catch (Exception e) {
            log.error("记录搜索历史失败，userId={}, keywords={}", userId, keywords, e);
            return false;
        }
    }

    @Override
    public List<SearchHistory> getRecentHistory(Integer userId, Integer limit) {
        try {
            if (userId == null) {
                return List.of();
            }

            if (limit == null || limit <= 0) {
                limit = 20; // 默认返回20条
            }

            return searchHistoryMapper.getRecentHistory(userId, limit);
        } catch (Exception e) {
            log.error("获取搜索历史失败，userId={}", userId, e);
            return List.of();
        }
    }

    @Override
    public List<String> getRecentKeywords(Integer userId, Integer limit) {
        try {
            if (userId == null) {
                return List.of();
            }

            if (limit == null || limit <= 0) {
                limit = 10; // 默认返回10条
            }

            return searchHistoryMapper.getRecentKeywords(userId, limit);
        } catch (Exception e) {
            log.error("获取搜索关键词失败，userId={}", userId, e);
            return List.of();
        }
    }

    @Override
    public Boolean clearHistory(Integer userId) {
        try {
            if (userId == null) {
                return false;
            }

            Integer result = searchHistoryMapper.deleteByUserId(userId);
            log.info("清空搜索历史成功，userId={}, deletedCount={}", userId, result);
            return true;
        } catch (Exception e) {
            log.error("清空搜索历史失败，userId={}", userId, e);
            return false;
        }
    }

    @Override
    public Boolean cleanOldHistory(Integer userId, Integer days) {
        try {
            if (userId == null) {
                return false;
            }

            if (days == null || days <= 0) {
                days = 30; // 默认清理30天前的历史
            }

            Integer result = searchHistoryMapper.deleteOldHistory(userId, days);
            log.info("清理过期搜索历史成功，userId={}, days={}, deletedCount={}", userId, days, result);
            return true;
        } catch (Exception e) {
            log.error("清理过期搜索历史失败，userId={}, days={}", userId, days, e);
            return false;
        }
    }
}

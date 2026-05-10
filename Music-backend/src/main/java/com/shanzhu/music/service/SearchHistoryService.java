package com.shanzhu.music.service;

import com.shanzhu.music.entity.po.SearchHistory;

import java.util.List;

/**
 * 搜索历史 服务层
 */
public interface SearchHistoryService {

    /**
     * 记录搜索历史
     *
     * @param userId 用户ID
     * @param keywords 搜索关键词
     * @param searchType 搜索类型（0歌曲1歌手2歌单）
     * @return 结果
     */
    Boolean recordSearch(Integer userId, String keywords, Byte searchType);

    /**
     * 获取用户最近的搜索历史
     *
     * @param userId 用户ID
     * @param limit 限制数量
     * @return 搜索历史列表
     */
    List<SearchHistory> getRecentHistory(Integer userId, Integer limit);

    /**
     * 获取用户最近搜索的关键词
     *
     * @param userId 用户ID
     * @param limit 限制数量
     * @return 关键词列表
     */
    List<String> getRecentKeywords(Integer userId, Integer limit);

    /**
     * 清空用户的搜索历史
     *
     * @param userId 用户ID
     * @return 结果
     */
    Boolean clearHistory(Integer userId);

    /**
     * 清理过期搜索历史（超过指定天数）
     *
     * @param userId 用户ID
     * @param days 天数
     * @return 结果
     */
    Boolean cleanOldHistory(Integer userId, Integer days);
}

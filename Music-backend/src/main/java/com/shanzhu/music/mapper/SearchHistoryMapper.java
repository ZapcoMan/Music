package com.shanzhu.music.mapper;

import com.shanzhu.music.entity.po.SearchHistory;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 搜索历史 Mapper
 */
@Mapper
public interface SearchHistoryMapper {

    /**
     * 插入搜索历史记录
     *
     * @param searchHistory 搜索历史
     * @return 结果
     */
    Integer insert(SearchHistory searchHistory);

    /**
     * 根据用户ID查询最近的搜索历史
     *
     * @param userId 用户ID
     * @param limit 限制数量
     * @return 搜索历史列表
     */
    List<SearchHistory> getRecentHistory(@Param("userId") Integer userId, @Param("limit") Integer limit);

    /**
     * 删除用户的搜索历史
     *
     * @param userId 用户ID
     * @return 结果
     */
    Integer deleteByUserId(Integer userId);

    /**
     * 删除指定时间的搜索历史
     *
     * @param userId 用户ID
     * @param days 天数
     * @return 结果
     */
    Integer deleteOldHistory(@Param("userId") Integer userId, @Param("days") Integer days);

    /**
     * 获取用户最近搜索的关键词列表
     *
     * @param userId 用户ID
     * @param limit 限制数量
     * @return 关键词列表
     */
    List<String> getRecentKeywords(@Param("userId") Integer userId, @Param("limit") Integer limit);

    /**
     * 统计用户搜索某关键词的次数
     *
     * @param userId 用户ID
     * @param keyword 关键词
     * @return 次数
     */
    Integer countKeywordFrequency(@Param("userId") Integer userId, @Param("keyword") String keyword);
}

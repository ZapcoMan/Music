package com.shanzhu.music.controller;

import com.shanzhu.music.annotation.RequireUserType;
import com.shanzhu.music.common.R;
import com.shanzhu.music.entity.po.SearchHistory;
import com.shanzhu.music.service.SearchHistoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 搜索历史 控制层
 */
@RestController
@RequestMapping("/search-history")
@RequireUserType("consumer")
@RequiredArgsConstructor
public class SearchHistoryController {

    private final SearchHistoryService searchHistoryService;

    /**
     * 记录搜索历史
     *
     * @param userId 当前登录用户，取自令牌
     * @param keywords 搜索关键词
     * @param searchType 搜索类型（0歌曲1歌手2歌单）
     * @return 结果
     */
    @PostMapping("/record")
    @RequireUserType("consumer")
    public R<Boolean> recordSearch(
            @RequestAttribute("userId") Integer userId,
            @RequestParam String keywords,
            @RequestParam(required = false, defaultValue = "0") Byte searchType
    ) {
        Boolean result = searchHistoryService.recordSearch(userId, keywords, searchType);
        if (result) {
            return R.ok("记录成功");
        }
        return R.error("记录失败");
    }

    /**
     * 获取用户最近的搜索历史
     *
     * @param userId 用户ID
     * @param limit 限制数量，默认20
     * @return 搜索历史列表
     */
    @GetMapping("/recent")
    public R<List<SearchHistory>> getRecentHistory(
            @RequestAttribute("userId") Integer userId,
            @RequestParam(required = false, defaultValue = "20") Integer limit
    ) {
        List<SearchHistory> historyList = searchHistoryService.getRecentHistory(userId, limit);
        return R.ok(historyList);
    }

    /**
     * 获取用户最近搜索的关键词
     *
     * @param userId 用户ID
     * @param limit 限制数量，默认10
     * @return 关键词列表
     */
    @GetMapping("/keywords")
    public R<List<String>> getRecentKeywords(
            @RequestAttribute("userId") Integer userId,
            @RequestParam(required = false, defaultValue = "10") Integer limit
    ) {
        List<String> keywords = searchHistoryService.getRecentKeywords(userId, limit);
        return R.ok(keywords);
    }

    /**
     * 清空用户的搜索历史
     *
     * @param userId 用户ID
     * @return 结果
     */
    @DeleteMapping("/clear")
    @RequireUserType("consumer")
    public R<Boolean> clearHistory(@RequestAttribute("userId") Integer userId) {
        Boolean result = searchHistoryService.clearHistory(userId);
        if (result) {
            return R.ok("清空成功");
        }
        return R.error("清空失败");
    }

    /**
     * 清理过期搜索历史
     *
     * @param userId 用户ID
     * @param days 天数，默认30天
     * @return 结果
     */
    @DeleteMapping("/clean-old")
    @RequireUserType("consumer")
    public R<Boolean> cleanOldHistory(
            @RequestAttribute("userId") Integer userId,
            @RequestParam(required = false, defaultValue = "30") Integer days
    ) {
        Boolean result = searchHistoryService.cleanOldHistory(userId, days);
        if (result) {
            return R.ok("清理成功");
        }
        return R.error("清理失败");
    }
}

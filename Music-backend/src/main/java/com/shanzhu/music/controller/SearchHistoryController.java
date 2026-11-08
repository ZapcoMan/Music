package com.shanzhu.music.controller;

import com.shanzhu.music.annotation.RequireUserType;
import com.shanzhu.music.common.R;
import com.shanzhu.music.entity.po.SearchHistory;
import com.shanzhu.music.service.OperationLogService;
import com.shanzhu.music.service.SearchHistoryService;
import com.shanzhu.music.utils.IpUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 搜索历史 控制层
 *
 * 角色域只有 consumer：搜索历史按 consumer 表的 id 归属，管理端从不写它，
 * 也不该由 admin 令牌代读（admin 的 id 在 consumer 表里不存在）。
 * 既有鉴权矩阵用例断言 admin 令牌打这些端点返回 403，不要把这里改成 {consumer, admin}。
 */
@RestController
@RequestMapping("/api/v1/search-history")
@RequireUserType("consumer")
@RequiredArgsConstructor
public class SearchHistoryController {

    private final SearchHistoryService searchHistoryService;
    private final OperationLogService operationLogService;
    private final IpUtils ipUtils;

    /**
     * 记录搜索历史
     *
     * @param userId 当前登录用户，取自令牌
     * @param keywords 搜索关键词
     * @param searchType 搜索类型（0歌曲1歌手2歌单）
     * @return 结果
     */
    @PostMapping
    @RequireUserType("consumer")
    public R<Boolean> recordSearch(
            @RequestAttribute("userId") Integer userId,
            @RequestParam String keywords,
            @RequestParam(required = false, defaultValue = "0") Byte searchType
    ) {
        Boolean result = searchHistoryService.recordSearch(userId, keywords, searchType);
        // 这里不另写 operation_log：search_history 这一行本身就是"谁在什么时候搜了什么"，
        // 再抄一份进审计表只会让每次搜索多一次写，且两处迟早对不上。
        if (result) {
            return R.ok("记录成功");
        }
        return R.error("记录失败");
    }

    /**
     * 获取用户最近的搜索历史
     *
     * 集合根路径即"我的搜索历史"：搜索历史是用户私有数据，身份只取令牌。
     *
     * @param userId 用户ID
     * @param limit 限制数量，默认20
     * @return 搜索历史列表
     */
    @GetMapping
    @RequireUserType("consumer")
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
     * 根路径的 DELETE 即"删掉我这个人的全部搜索历史"：集合根已经带上了 owner（令牌），
     * 再叫 /clear 只是把同一个动作说两遍。
     *
     * @param userId 用户ID
     * @return 结果
     */
    @DeleteMapping
    @RequireUserType("consumer")
    public R<Boolean> clearHistory(
            @RequestAttribute("userId") Integer userId,
            @RequestAttribute("username") String username,
            @RequestAttribute("userType") String userType,
            HttpServletRequest httpRequest
    ) {
        Boolean result = searchHistoryService.clearHistory(userId);
        // 清空是不可逆的整表删除，删完 search_history 里什么都不剩，
        // 只能靠审计回答"这个人是什么时候把自己的搜索记录清掉的"
        if (result) {
            operationLogService.logDelete(userId, username, userType, "互动模块",
                    "清空本人全部搜索历史", ipUtils.getIpAddress(httpRequest));
            return R.ok("清空成功");
        }
        return R.error("清空失败");
    }

    /**
     * 清理过期搜索历史
     *
     * 用 /expired 这个集合名做 DELETE 的目标，意思是"删掉集合里符合条件的那批行"，
     * 与根路径的"整集清空"区分开；原 /clean-old 是动词短语，读起来不像资源。
     *
     * @param userId 用户ID
     * @param days 天数，默认30天
     * @return 结果
     */
    @DeleteMapping("/expired")
    @RequireUserType("consumer")
    public R<Boolean> cleanOldHistory(
            @RequestAttribute("userId") Integer userId,
            @RequestAttribute("username") String username,
            @RequestAttribute("userType") String userType,
            @RequestParam(required = false, defaultValue = "30") Integer days,
            HttpServletRequest httpRequest
    ) {
        Boolean result = searchHistoryService.cleanOldHistory(userId, days);
        // days 是这次实际生效的保留窗口，不记下来就无法判断是不是被传了个 0 全清掉了
        if (result) {
            operationLogService.logDelete(userId, username, userType, "互动模块",
                    "清理保留期 " + days + " 天以外的搜索历史", ipUtils.getIpAddress(httpRequest));
            return R.ok("清理成功");
        }
        return R.error("清理失败");
    }
}

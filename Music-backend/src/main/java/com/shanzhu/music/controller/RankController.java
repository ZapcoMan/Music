package com.shanzhu.music.controller;

import com.shanzhu.music.annotation.RequireUserType;
import com.shanzhu.music.common.R;
import com.shanzhu.music.entity.dto.RankRequest;
import com.shanzhu.music.entity.po.Rank;
import com.shanzhu.music.service.OperationLogService;
import com.shanzhu.music.service.RankService;
import com.shanzhu.music.utils.IpUtils;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RestController;

/**
 * 评价 控制层
 *
 * 评分记录本身是 /api/v1/ranks 下的资源；而"某张歌单的平均分"读的是歌单的衍生属性，
 * 归属在歌单一侧，写成 /api/v1/song-lists/{songListId}/rank。
 * 两类路径不在同一个前缀下，故不声明类级 @RequestMapping，逐方法写绝对路径。
 * 角色域仍留在类级兜底，方法级注解优先级更高。
 */
@RestController
@RequireUserType({"consumer", "admin"})
@RequiredArgsConstructor
public class RankController {

    private final RankService rankService;
    private final OperationLogService operationLogService;
    private final IpUtils ipUtils;

    /**
     * 新增评价
     * <p>
     * 评分人身份取自令牌，不再接受入参 consumerId：此前任何登录用户都能指定
     * consumerId 打任意分，而评分是协同过滤与精排的直接输入，污染它等于污染推荐结果。
     * 前端本就在传自己的 userId（SongListAlbum.vue），行为对正常用户无变化。
     *
     * @param userId      评分人 id，取自令牌并写入 rank.consumerId，不接受入参
     * @param username    评分人用户名，仅用于审计日志
     * @param userType    评分人角色（consumer/admin），仅用于审计日志
     * @param request     评分请求体：songListId 歌单id、score 分值（1-10）
     * @param httpRequest 当前请求，用于取 IP 写审计日志
     * @return 结果
     */
    @Operation(summary = "提交评分", description = "为指定歌单打分，取值 1-10；同一用户重复提交视为改分")
    @PostMapping(value = "/api/v1/ranks")
    @RequireUserType("consumer")
    public R<Void> add(@RequestAttribute("userId") Integer userId,
                       @RequestAttribute("username") String username,
                       @RequestAttribute("userType") String userType,
                       @Valid RankRequest request,
                       HttpServletRequest httpRequest) {
        Rank rank = new Rank();
        rank.setSongListId(request.getSongListId());
        rank.setConsumerId(userId);
        rank.setScore(request.getScore());

        if (rankService.save(rank)) {
            // 评分是协同过滤与精排的直接输入，重复提交会覆盖旧分值，
            // 不记这条就查不出"这个分是谁什么时候打的、打的是几分"。
            // 描述刻意写成"新增或改分"：RankService#save 是 upsert，
            // controller 拿不到本次究竟走了插入还是覆盖，
            // 与其让日志把一个改分动作说成"新增评分"，不如把两种可能都写出来。
            operationLogService.logAdd(userId, username, userType, "互动模块",
                    "提交评分（新增或改分） 歌单 id=" + request.getSongListId() + " 分值=" + request.getScore(),
                    ipUtils.getIpAddress(httpRequest));
            return R.ok("评价成功");
        }

        return R.error("评价失败");
    }

    /**
     * 计算平均分
     *
     * songListId 由查询串改成路径段之后，"参数缺失"这件事在路由层就不可能发生了：
     * 路径匹配不上直接 404，不会再走到这里。所以原先用 ParseUtils 兜 "空串/漏传 → 返回 0"
     * 的那段容错已无对应输入，删掉，交给 Spring 做类型转换。
     *
     * @param songListId 歌单id
     * @return 结果
     */
    @Operation(summary = "查询歌单平均分", description = "返回 1-10 分制的平均值；无人评分时给量纲中点")
    @GetMapping(value = "/api/v1/song-lists/{songListId}/rank")
    public R<Double> rankOfSongListId(@PathVariable("songListId") Integer songListId) {
        return R.ok(rankService.rankOfSongListId(songListId));
    }

}

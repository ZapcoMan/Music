package com.shanzhu.music.controller;

import com.shanzhu.music.annotation.RequireUserType;
import com.shanzhu.music.common.R;
import com.shanzhu.music.entity.po.Collect;
import com.shanzhu.music.entity.po.SongList;
import com.shanzhu.music.service.CollectService;
import com.shanzhu.music.service.OperationLogService;
import com.shanzhu.music.utils.IpUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 收藏 控制层
 *
 * 收藏属于用户私有数据，userId 一律取自令牌（由 JwtAuthenticationInterceptor
 * 写入 request attribute），不再接受客户端传参，否则任意人可凭他人 userId
 * 增删收藏。
 *
 * 取消收藏为什么不用 DELETE /api/v1/collections/{id}：collect 表的主键是关联行 id，
 * 而前端做"点亮/取消点亮"时手里只有 songId / songListId（播放条按当前歌曲 id 判断心形状态，
 * 管理端收藏页把收藏记录还原成歌曲行之后拿的也是歌曲 id）。要改成 {id}，
 * 每次取消前都得先拉一次列表换取行 id，多一次往返且把内部主键变成对外契约。
 * 故这里保留"按业务定位符删除"的形态，用查询串表达定位条件。
 */
@RestController
@RequestMapping("/api/v1/collections")
@RequireUserType({"consumer", "admin"})
@RequiredArgsConstructor
public class CollectController {

    private final CollectService collectService;
    private final OperationLogService operationLogService;
    private final IpUtils ipUtils;

    /**
     * 添加收藏
     *
     * @param userId 当前登录用户，取自令牌
     * @param type   收藏类型（0歌曲1歌单）
     * @param songId 歌曲id
     * @return 添加结果
     */
    @PostMapping
    @RequireUserType("consumer")
    public R<Void> addCollect(@RequestAttribute("userId") Integer userId,
                        @RequestAttribute("username") String username,
                        @RequestAttribute("userType") String userType,
                        @RequestParam Byte type,
                        @RequestParam(required = false) Integer songId,
                        @RequestParam(required = false) Integer songListId,
                        HttpServletRequest httpRequest) {
        Collect collect = new Collect();
        collect.setUserId(userId);
        collect.setType(type);
        // 原先只有歌曲一条路：songId 必传且无条件写入 song_id，
        // 所以 type=1 的歌单收藏根本存不进去（也就无从读取）
        if (type == 0) {
            if (songId == null) {
                return R.error("收藏歌曲为空");
            }
            if (collectService.existSongId(userId, songId)) {
                return R.error(2, "已收藏");
            }
            collect.setSongId(songId);
        } else {
            if (songListId == null) {
                return R.error("收藏歌单为空");
            }
            if (collectService.existSongListId(userId, songListId)) {
                return R.error(2, "已收藏");
            }
            collect.setSongListId(songListId);
        }
        if (collectService.insert(collect)) {
            // 收藏行是用户私有数据，管理员侧看不到来源，只有审计能回答"这条心形谁点的"
            operationLogService.logAdd(userId, username, userType, "互动模块",
                    "新增收藏 " + (type == 0 ? "歌曲 id=" + songId : "歌单 id=" + songListId),
                    ipUtils.getIpAddress(httpRequest));
            return R.ok("收藏成功");
        }

        return R.error("收藏失败");
    }

    /**
     * 取消当前登录用户对某首歌/某张歌单的收藏
     *
     * @param userId 当前登录用户，取自令牌
     * @param songId 歌曲id（收藏歌曲时的定位符）
     * @param songListId 歌单id（收藏歌单时的定位符）
     * @return 删除结果
     */
    @DeleteMapping
    @RequireUserType("consumer")
    public R<Boolean> deleteCollect(@RequestAttribute("userId") Integer userId,
                                @RequestAttribute("username") String username,
                                @RequestAttribute("userType") String userType,
                                @RequestParam(required = false) Integer songId,
                                @RequestParam(required = false) Integer songListId,
                                HttpServletRequest httpRequest) {
        // 两个定位符都不给时，老代码会拿 songId=null 去执行
        // "delete ... and song_id = null"，条件恒不成立 → 返回 false，
        // 调用方看到的是"删除失败"而不是"你少传了参数"。这里显式区分。
        if (songId == null && songListId == null) {
            return R.error("请指定要取消收藏的歌曲或歌单");
        }
        if (songListId != null) {
            boolean deleted = collectService.deleteByUserIdSongListId(userId, songListId);
            // 没删到行（原本就没收藏）不记，否则审计里会堆出一堆没有实际写入的"取消收藏"
            if (deleted) {
                operationLogService.logDelete(userId, username, userType, "互动模块",
                        "取消收藏歌单 id=" + songListId, ipUtils.getIpAddress(httpRequest));
            }
            return R.ok(deleted);
        }
        boolean deleted = collectService.deleteByUserIdSongId(userId, songId);
        if (deleted) {
            operationLogService.logDelete(userId, username, userType, "互动模块",
                    "取消收藏歌曲 id=" + songId, ipUtils.getIpAddress(httpRequest));
        }
        return R.ok(deleted);
    }

    /**
     * 查询当前登录用户收藏的歌单
     *
     * @param userId 当前登录用户，取自令牌
     * @return 歌单列表
     */
    @GetMapping(value = "/song-lists")
    public R<List<SongList>> collectSongList(@RequestAttribute("userId") Integer userId) {
        return R.ok(collectService.collectSongListOfUserId(userId));
    }

    /**
     * 查询所有收藏（管理端功能）
     *
     * @return 收藏列表
     */
    @GetMapping
    @RequireUserType("admin")
    public R<List<Collect>> allCollect() {
        return R.ok(collectService.allCollect());
    }

    /**
     * 查询当前登录用户的收藏列表
     *
     * @param userId 当前登录用户，取自令牌
     * @return 收藏列表
     */
    @GetMapping(value = "/mine")
    public R<List<Collect>> collectOfUserId(@RequestAttribute("userId") Integer userId) {
        return R.ok(collectService.collectOfUserId(userId));
    }

}

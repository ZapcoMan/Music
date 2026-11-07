package com.shanzhu.music.controller;

import com.shanzhu.music.annotation.RequireUserType;
import com.shanzhu.music.common.R;
import com.shanzhu.music.entity.po.Collect;
import com.shanzhu.music.service.CollectService;
import lombok.RequiredArgsConstructor;
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
 */
@RestController
@RequestMapping("/collect")
@RequiredArgsConstructor
public class CollectController {

    private final CollectService collectService;

    /**
     * 添加收藏
     *
     * @param userId 当前登录用户，取自令牌
     * @param type   收藏类型（0歌曲1歌单）
     * @param songId 歌曲id
     * @return 添加结果
     */
    @PostMapping(value = "/add")
    public R addCollect(@RequestAttribute("userId") Integer userId,
                        @RequestParam Byte type,
                        @RequestParam Integer songId) {
        if (songId == null) {
            return R.error("收藏歌曲为空");
        }
        if (collectService.existSongId(userId, songId)) {
            return R.error(2, "已收藏");
        }
        Collect collect = new Collect();
        collect.setUserId(userId);
        collect.setType(type);
        collect.setSongId(songId);
        if (collectService.insert(collect)) {
            return R.ok("收藏成功");
        }

        return R.error("收藏失败");
    }

    /**
     * 删除当前登录用户对某首歌的收藏
     *
     * @param userId 当前登录用户，取自令牌
     * @param songId 歌曲id
     * @return 删除结果
     */
    @GetMapping(value = "/delete")
    public Boolean deleteCollect(@RequestAttribute("userId") Integer userId,
                                @RequestParam Integer songId) {
        return collectService.deleteByUserIdSongId(userId, songId);
    }

    /**
     * 查询所有收藏（管理端功能）
     *
     * @return 收藏列表
     */
    @GetMapping(value = "/allCollect")
    @RequireUserType("admin")
    public List<Collect> allCollect() {
        return collectService.allCollect();
    }

    /**
     * 查询当前登录用户的收藏列表
     *
     * @param userId 当前登录用户，取自令牌
     * @return 收藏列表
     */
    @GetMapping(value = "/collectOfUserId")
    public List<Collect> collectOfUserId(@RequestAttribute("userId") Integer userId) {
        return collectService.collectOfUserId(userId);
    }

}

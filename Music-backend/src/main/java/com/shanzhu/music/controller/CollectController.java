package com.shanzhu.music.controller;

import com.shanzhu.music.common.R;
import com.shanzhu.music.entity.po.Collect;
import com.shanzhu.music.service.CollectService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 收藏 控制层
 */
@RestController
@RequestMapping("/collect")
@RequiredArgsConstructor
public class CollectController {

    private final CollectService CollectService;

    /**
     * 添加收藏
     *
     * @param userId 用户id
     * @param type   收藏类型（0歌曲1歌单）
     * @param songId 歌曲id
     * @return 添加结果
     */
    @PostMapping(value = "/add")
    public R addCollect(@RequestParam Integer userId, @RequestParam Byte type, @RequestParam Integer songId) {
        if (songId == null) {
            return R.error("收藏歌曲为空");
        }
        if (CollectService.existSongId(userId, songId)) {
            return R.error(2, "已收藏");
        }
        //保存到收藏的对象中
        Collect Collect = new Collect();
        Collect.setUserId(userId);
        Collect.setType(type);
        Collect.setSongId(songId);
        //保存成功
        if (CollectService.insert(Collect)) {
            return R.ok("收藏成功");
        }

        return R.error("收藏失败");
    }

    /**
     * 删除收藏
     *
     * @param userId 用户id
     * @param songId 歌曲id
     * @return 删除结果
     */
    @GetMapping(value = "/delete")
    public Boolean deleteCollect(@RequestParam Integer userId, @RequestParam Integer songId) {
        return CollectService.deleteByUserIdSongId(userId, songId);
    }

    /**
     * 查询所有收藏
     *
     * @return 收藏列表
     */
    @GetMapping(value = "/allCollect")
    public List<Collect> allCollect() {
        return CollectService.allCollect();
    }

    /**
     * 查询某个用户的收藏列表
     *
     * @param userId 用户id
     * @return 收藏列表
     */
    @GetMapping(value = "/collectOfUserId")
    public List<Collect> collectOfUserId(@RequestParam Integer userId) {
        return CollectService.collectOfUserId(userId);
    }

}

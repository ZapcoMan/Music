package com.shanzhu.music.controller;

import com.shanzhu.music.common.R;
import com.shanzhu.music.entity.po.Rank;
import com.shanzhu.music.service.RankService;
import com.shanzhu.music.utils.ParseUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 评价 控制层
 */
@RestController
@RequestMapping("/rank")
@RequiredArgsConstructor
public class RankController {

    private final RankService rankService;

    /**
     * 新增评价
     *
     * 评分人身份取自令牌，不再接受入参 consumerId：此前任何登录用户都能指定
     * consumerId 打任意分，而评分是协同过滤与精排的直接输入，污染它等于污染推荐结果。
     * 前端本就在传自己的 userId（SongListAlbum.vue），行为对正常用户无变化。
     *
     * @param songListId 歌单id
     * @param score      评价
     * @return 结果
     */
    @PostMapping(value = "/add")
    public R add(@RequestAttribute("userId") Integer userId, Integer songListId, Integer score) {
        Rank rank = new Rank();
        rank.setSongListId(songListId);
        rank.setConsumerId(userId);
        rank.setScore(score);

        if (rankService.insert(rank)) {
            return R.ok("评价成功");
        }

        return R.error("评价失败");
    }

    /**
     * 计算平均分
     *
     * @param songListId 歌单id
     * @return 结果
     */
    @GetMapping
    public Integer rankOfSongListId(String songListId) {
        Integer parsedSongListId = ParseUtils.toIntegerOrNull(songListId);        if (parsedSongListId == null) {
            return 0;
        }
        return rankService.rankOfSongListId(parsedSongListId);
    }

}

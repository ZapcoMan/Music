package com.shanzhu.music.controller;

import com.shanzhu.music.annotation.RequireUserType;
import com.shanzhu.music.common.R;
import com.shanzhu.music.entity.po.ListSong;
import com.shanzhu.music.service.ListSongService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 歌单内歌曲 控制层
 */
@RestController
@RequestMapping("/listSong")
@RequiredArgsConstructor
public class ListSongController {

    private final ListSongService listSongService;

    /**
     * 给歌单添加歌曲
     *
     * @param songId     歌曲id
     * @param songListId 歌单id
     * @return 添加结果
     */
    @PostMapping(value = "/add")
    @RequireUserType("admin")
    public R<Void> addListSong(Integer songId, Integer songListId) {
        //获取前端传来的参数
        ListSong listSong = new ListSong();
        listSong.setSongId(songId);
        listSong.setSongListId(songListId);

        //插入歌单
        if (listSongService.insert(listSong)) {
            return R.ok("保存成功");
        }
        return R.error("保存失败");
    }

    /**
     * 根据歌单id查询歌曲
     *
     * @param songListId 歌单id
     * @return 歌曲列表
     */
    @GetMapping(value = "/detail")
    public R<List<ListSong>> detail(Integer songListId) {
        return R.ok(listSongService.listSongOfSongListId(songListId));
    }

    /**
     * 删除歌单里的歌曲
     *
     * @param songId     歌曲id
     * @param songListId 歌单id
     * @return 删除结果
     */
    @DeleteMapping(value = "/delete")
    @RequireUserType("admin")
    public R<Boolean> delete(Integer songId, Integer songListId) {
        return R.ok(listSongService.deleteBySongIdAndSongListId(songId, songListId));
    }

}

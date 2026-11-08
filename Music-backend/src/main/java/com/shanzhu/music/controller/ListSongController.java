package com.shanzhu.music.controller;

import com.shanzhu.music.annotation.RequireUserType;
import com.shanzhu.music.common.R;
import com.shanzhu.music.entity.po.ListSong;
import com.shanzhu.music.service.ListSongService;
import com.shanzhu.music.service.OperationLogService;
import com.shanzhu.music.utils.IpUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 歌单内歌曲 控制层
 *
 * 这批端点表达的是"某张歌单里的那些歌"，URL 归属在歌单一侧，所以写成
 * /api/v1/song-lists/{songListId}/songs 这样的子资源；本类不声明类级 @RequestMapping，
 * 否则会出现 /api/v1/listSong 下的路径其实不属于 listSong 这个资源的错位。
 * 角色域仍留在类级兜底，方法级注解优先级更高。
 */
@RestController
@RequireUserType({"consumer", "admin"})
@RequiredArgsConstructor
public class ListSongController {

    private final ListSongService listSongService;
    private final OperationLogService operationLogService;
    private final IpUtils ipUtils;

    /**
     * 给歌单添加歌曲
     *
     * @param songListId 目标歌单id，以 URL 为准
     * @param songId     歌曲id
     * @return 添加结果
     */
    @PostMapping(value = "/api/v1/song-lists/{songListId}/songs")
    @RequireUserType("admin")
    public R<Void> addListSong(@RequestAttribute("userId") Integer operatorId,
                               @RequestAttribute("username") String operatorName,
                               @RequestAttribute("userType") String operatorType,
                               @PathVariable("songListId") Integer songListId,
                               Integer songId,
                               HttpServletRequest httpRequest) {
        // 歌单id 只认 URL：请求体里再传一个不同的 songListId 也不能把歌加进别的歌单
        ListSong listSong = new ListSong();
        listSong.setSongId(songId);
        listSong.setSongListId(songListId);

        //插入歌单
        if (listSongService.insert(listSong)) {
            operationLogService.logAdd(operatorId, operatorName, operatorType,
                    "曲库模块", "歌单 id=" + songListId + " 添加歌曲 id=" + songId,
                    ipUtils.getIpAddress(httpRequest));
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
    @GetMapping(value = "/api/v1/song-lists/{songListId}/songs")
    public R<List<ListSong>> detail(@PathVariable("songListId") Integer songListId) {
        return R.ok(listSongService.listSongOfSongListId(songListId));
    }

    /**
     * 删除歌单里的歌曲
     *
     * @param songListId 歌单id
     * @param songId     歌曲id
     * @return 删除结果
     */
    @DeleteMapping(value = "/api/v1/song-lists/{songListId}/songs/{songId}")
    @RequireUserType("admin")
    public R<Boolean> delete(@RequestAttribute("userId") Integer operatorId,
                             @RequestAttribute("username") String operatorName,
                             @RequestAttribute("userType") String operatorType,
                             @PathVariable("songListId") Integer songListId,
                             @PathVariable("songId") Integer songId,
                             HttpServletRequest httpRequest) {
        boolean deleted = listSongService.deleteBySongIdAndSongListId(songId, songListId);
        // 关联行删除后本表不留任何痕迹，是谁在什么时候摘掉的不记审计就查不回来
        if (deleted) {
            operationLogService.logDelete(operatorId, operatorName, operatorType,
                    "曲库模块", "歌单 id=" + songListId + " 移除歌曲 id=" + songId,
                    ipUtils.getIpAddress(httpRequest));
        }
        return R.ok(deleted);
    }

}

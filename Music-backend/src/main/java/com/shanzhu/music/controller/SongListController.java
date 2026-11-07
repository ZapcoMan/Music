package com.shanzhu.music.controller;

import com.github.pagehelper.PageInfo;
import com.shanzhu.music.annotation.RequireUserType;
import com.shanzhu.music.common.R;
import com.shanzhu.music.entity.po.Song;
import com.shanzhu.music.entity.po.SongList;
import com.shanzhu.music.entity.vo.UploadPicVo;
import com.shanzhu.music.service.SongListService;
import com.shanzhu.music.service.FileStorageService;
import com.shanzhu.music.utils.ParseUtils;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * 歌单 控制层
 */
@RestController
@RequestMapping("/songList")
@RequiredArgsConstructor
public class SongListController {

    private static final Logger log = LoggerFactory.getLogger(SongListController.class);
    private final SongListService songListService;
    private final FileStorageService fileStorageService;

    /**
     * 添加歌单
     *
     * @param title        标题
     * @param pic          歌单图片
     * @param introduction 简介
     * @param style        风格
     * @return 添加结果
     */
    @PostMapping(value = "/add")
    @RequireUserType("admin")
    public R<Void> addSongList(
            String title,
            String pic,
            String introduction,
            String style
    ) {
        //保存到歌单的对象中
        SongList songList = new SongList();
        songList.setTitle(title);
        songList.setPic(pic);
        songList.setIntroduction(introduction);
        songList.setStyle(style);

        //添加成功
        if (songListService.insert(songList)) {
            return R.ok("添加成功");
        }

        return R.error("添加失败");
    }

    /**
     * 修改歌单
     *
     * @param id           主键
     * @param title        标题
     * @param introduction 简介
     * @param style        风格
     * @return 修改结果
     */
    @PostMapping(value = "/update")
    @RequireUserType("admin")
    public R<Void> updateSongList(
            String id,
            String title,
            String introduction,
            String style
    ) {
        //保存到歌单的对象中
        SongList songList = new SongList();
        Integer parsedId = ParseUtils.toIntegerOrNull(id);
        if (parsedId == null) {
            return R.error("歌单ID不合法");
        }
        songList.setId(parsedId);
        songList.setTitle(title);
        songList.setIntroduction(introduction);
        songList.setStyle(style);

        //修改成功
        if (songListService.update(songList)) {
            return R.ok("修改成功");
        }
        return R.error("修改失败");
    }

    /**
     * 删除歌单
     *
     * @param id 歌单id
     * @return 删除结果
     */
    @DeleteMapping(value = "/delete")
    @RequireUserType("admin")
    public R<Boolean> deleteSongList(Integer id) {
        return R.ok(songListService.delete(id));
    }

    /**
     * 查询歌单
     *
     * @param id 歌单id
     * @return 歌单
     */
    @GetMapping(value = "/selectByPrimaryKey")
    public R<SongList> selectByPrimaryKey(Integer id) {
        return R.ok(songListService.selectByPrimaryKey(id));
    }

    /**
     * 查询所有歌单
     *
     * @return 歌单列表
     */
    @GetMapping(value = "/allSongList")
    public R<List<SongList>> allSongList() {
        log.info("songListService.allSongList() = " + songListService.allSongList());
        return R.ok(songListService.allSongList());
    }

    /**
     * 根据标题精确查询歌单列表
     *
     * @param title 标题
     * @return 歌单列表
     */
    @GetMapping(value = "/songListOfTitle")
    public R<List<SongList>> songListOfName(String title) {
        return R.ok(songListService.songListOfTitle(title));
    }

    /**
     * 根据标题模糊查询歌单列表
     *
     * @param title 标题
     * @return 歌单列表
     */
    @GetMapping(value = "/likeTitle")
    public R<List<SongList>> likeTitle(String title) {
        return R.ok(songListService.likeTitle("%" + title + "%"));
    }

    /**
     * 根据风格模糊查询歌单列表
     *
     * @param style 歌单风格
     * @return 歌单列表
     */
    @RequestMapping(value = "/likeStyle", method = RequestMethod.GET)
    public R<List<SongList>> likeStyle(String style) {
        return R.ok(songListService.likeStyle("%" + style + "%"));
    }

    /**
     * 更新歌单图片
     *
     * @param avatorFile 头像文件
     * @param id         歌单id
     * @return 更新结果
     */
    @PostMapping(value = "/updateSongListPic")
    @RequireUserType("admin")
    public R<UploadPicVo> updateSongListPic(
            @RequestParam("file") MultipartFile avatorFile,
            @RequestParam("id") Integer id
    ) throws IOException {
        String storeAvatorPath;
        try {
            storeAvatorPath = fileStorageService.saveImage(avatorFile, "img/songListPic");
        } catch (IllegalArgumentException | IllegalStateException e) {
            return R.error(e.getMessage());
        }
        SongList songList = new SongList();
        songList.setId(id);
        songList.setPic(storeAvatorPath);

        //更新歌单
        if (songListService.update(songList)) {
            UploadPicVo uploadPicVo = new UploadPicVo();
            uploadPicVo.setPic(storeAvatorPath);
            return R.ok("上传成功", uploadPicVo);
        }

        return R.error("上传失败");
    }

    @PostMapping(value = "/queryRecommendedSongListById")
    @RequireUserType("consumer")
    public R<List<Song>> queryRecommendedSongListById(String id) {
        log.info("queryRecommendedSongListById 触发了");
        log.info("id : {}", id);
        List<Song> songLists = songListService.queryRecommendedSongById(id);
//        log.info("songLists : {}",songLists.toString());
        return R.ok("OK",songLists);
    }
    
    /**
     * 分页查询所有歌单
     *
     * @param pageNum 页码，默认1
     * @param pageSize 每页大小，默认10
     * @return 分页结果
     */
    @GetMapping(value = "/allSongListByPage")
    public R<PageInfo<SongList>> allSongListByPage(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize
    ) {
        PageInfo<SongList> pageInfo = songListService.allSongListByPage(pageNum, pageSize);
        return R.ok(pageInfo);
    }

}

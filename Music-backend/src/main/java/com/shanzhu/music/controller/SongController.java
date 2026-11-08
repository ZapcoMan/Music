package com.shanzhu.music.controller;

import com.github.pagehelper.PageInfo;
import com.shanzhu.music.annotation.RequireUserType;
import com.shanzhu.music.common.R;
import com.shanzhu.music.entity.po.Song;
import com.shanzhu.music.entity.vo.UploadPicVo;
import com.shanzhu.music.entity.vo.UploadSongVo;
import com.shanzhu.music.service.SongService;
import com.shanzhu.music.service.FileStorageService;
import com.shanzhu.music.utils.ParseUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * 歌曲 控制层
 *
 * 业务接口挂在 /api/v1/songs 下，与免登录的静态音频前缀彻底分开：
 * /song/* 由 WebMvcConfig 排除用于 <audio> 直接取流（该标签发不出 Authorization 头），
 * 上一轮曾把业务接口下沉到 /song/manage/* 来躲开这条白名单，那只是中间态——
 * 现在两者字面不同域（/api/v1/songs 与 /song/{文件}），不再依赖"避开某个前缀"来保证鉴权。
 */
@RestController
@RequestMapping("/api/v1/songs")
// 类级角色域兜底：本类未单独标注的方法（详情、列表、搜索等读接口）一律要求已登录，
// 具体到方法的 @RequireUserType("admin") 优先级更高，不会被这里放宽。
@RequireUserType({"consumer", "admin"})
@RequiredArgsConstructor
public class SongController {

    private final SongService songService;
    private final FileStorageService fileStorageService;

    /**
     * 添加歌曲
     *
     * @param singerId     所属歌手id
     * @param name         歌名
     * @param introduction 简介
     * @param pic          默认图片
     * @param lyric        歌词
     * @param mpFile       歌曲文件
     * @return 添加结果
     */
    @PostMapping
    @RequireUserType("admin")
    public R<UploadSongVo> addSong(
            String singerId,
            String name,
            String introduction,
            String pic,
            String lyric,
            @RequestParam("file") MultipartFile mpFile
    ) throws IOException {
        String storeUrlPath;
        try {
            storeUrlPath = fileStorageService.saveAudio(mpFile, "song");
        } catch (IllegalArgumentException | IllegalStateException e) {
            return R.error(e.getMessage());
        }

        Integer parsedSingerId = ParseUtils.toIntegerOrNull(singerId);
        if (parsedSingerId == null) {
            return R.error("歌手ID不合法");
        }

        Song song = new Song();
        song.setSingerId(parsedSingerId);
        song.setName(name);
        song.setIntroduction(introduction);
        song.setPic(pic);
        song.setLyric(lyric);
        song.setUrl(storeUrlPath);

        //保存歌曲成功
        if (songService.insert(song)) {
            UploadSongVo uploadSongVo = new UploadSongVo();
            uploadSongVo.setAvator(storeUrlPath);
            return R.ok(uploadSongVo);
        }

        return R.error("保存失败");
    }

    /**
     * 修改歌曲
     *
     * @param id           主键
     * @param name         歌名
     * @param introduction 专辑
     * @param lyric        歌词
     * @return 修改结果
     */
    @PatchMapping(value = "/{id}")
    @RequireUserType("admin")
    public R<Void> updateSong(@PathVariable("id") Integer id,
                             String name, String introduction, String lyric) {
        //保存到歌手的对象中
        Song song = new Song();
        song.setId(id);
        song.setName(name);
        song.setIntroduction(introduction);
        song.setLyric(lyric);

        //修改成功
        if (songService.update(song)) {
            return R.ok("修改成功");
        }

        return R.error("修改失败");
    }

    /**
     * 删除歌曲
     *
     * @param id 歌曲id
     * @return 删除结果
     */
    @DeleteMapping(value = "/{id}")
    @RequireUserType("admin")
    public R<Boolean> deleteSong(@PathVariable("id") Integer id) {
        return R.ok(songService.delete(id));
    }

    /**
     * 更新歌曲图片
     *
     * @param avatorFile 歌曲图片
     * @param id         歌曲id
     * @return 更新结果
     */
    @PostMapping(value = "/{id}/picture")
    @RequireUserType("admin")
    public R<UploadPicVo> updateSongPic(
            @RequestParam("file") MultipartFile avatorFile,
            @PathVariable("id") Integer id
    ) throws IOException {

        String storeAvatorPath;
        try {
            storeAvatorPath = fileStorageService.saveImage(avatorFile, "img/songPic");
        } catch (IllegalArgumentException | IllegalStateException e) {
            return R.error(e.getMessage());
        }
        Song song = new Song();
        song.setId(id);
        song.setPic(storeAvatorPath);

        //更新成功
        if (songService.update(song)) {
            UploadPicVo uploadPicVo = new UploadPicVo();
            uploadPicVo.setPic(storeAvatorPath);
            return R.ok("上传成功", uploadPicVo);
        }

        return R.error("上传失败");
    }

    /**
     * 更新歌曲
     *
     * @param avatorFile 歌曲图片文件
     * @param id         歌曲id
     * @return 更新结果
     */
    @PostMapping(value = "/{id}/audio")
    @RequireUserType("admin")
    public R<UploadSongVo> updateSongUrl(
            @RequestParam("file") MultipartFile avatorFile,
            @PathVariable("id") Integer id
    ) throws IOException {
        String storeAvatorPath;
        try {
            storeAvatorPath = fileStorageService.saveAudio(avatorFile, "song");
        } catch (IllegalArgumentException | IllegalStateException e) {
            return R.error(e.getMessage());
        }
        Song song = new Song();
        song.setId(id);
        song.setUrl(storeAvatorPath);

        //更新歌曲
        if (songService.update(song)) {
            UploadSongVo uploadSongVo = new UploadSongVo();
            uploadSongVo.setAvator(storeAvatorPath);
            return R.ok("上传成功", uploadSongVo);
        }

        return R.error("上传失败");
    }

    /**
     * 根据歌曲id查询歌曲对象
     *
     * @param id 歌曲id
     * @return 歌曲
     */
    @GetMapping(value = "/{id}")
    public R<Song> detail(@PathVariable("id") Integer id) {
        return R.ok(songService.selectByPrimaryKey(id));
    }

    /**
     * 按歌名精确查询歌曲
     *
     * @param songName 歌名
     * @return 歌曲列表
     */
    @GetMapping(params = "songName")
    public R<List<Song>> songOfSongName(String songName) {
        return R.ok(songService.songOfName(songName));
    }

    /**
     * 按关键词模糊搜索歌曲
     *
     * @param keyword 搜索关键词
     * @return 歌曲列表
     */
    @GetMapping(params = "keyword")
    public R<List<Song>> likeSongOfName(String keyword) {
        return R.ok(songService.likeSongOfName(keyword));
    }

    /**
     * 查询所有歌曲
     *
     * @return 歌曲列表
     */
    @GetMapping
    public R<List<Song>> allSong() {
        return R.ok(songService.allSong());
    }
    
    /**
     * 分页查询所有歌曲
     *
     * @param pageNum 页码，默认1
     * @param pageSize 每页大小，默认10
     * @return 分页结果
     */
    @GetMapping(value = "/page")
    public R<PageInfo<Song>> allSongByPage(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize
    ) {
        PageInfo<Song> pageInfo = songService.allSongByPage(pageNum, pageSize);
        return R.ok(pageInfo);
    }

}

package com.shanzhu.music.controller;

import com.github.pagehelper.PageInfo;
import com.shanzhu.music.annotation.RequireUserType;
import com.shanzhu.music.common.R;
import com.shanzhu.music.entity.po.Song;
import com.shanzhu.music.entity.vo.UploadPicVo;
import com.shanzhu.music.entity.vo.UploadSongVo;
import com.shanzhu.music.service.OperationLogService;
import com.shanzhu.music.service.SongService;
import com.shanzhu.music.service.FileStorageService;
import com.shanzhu.music.utils.IpUtils;
import com.shanzhu.music.utils.ParseUtils;
import jakarta.servlet.http.HttpServletRequest;
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
    private final OperationLogService operationLogService;
    private final IpUtils ipUtils;

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
            @RequestAttribute("userId") Integer operatorId,
            @RequestAttribute("username") String operatorName,
            @RequestAttribute("userType") String operatorType,
            String singerId,
            String name,
            String introduction,
            String pic,
            String lyric,
            @RequestParam("file") MultipartFile mpFile,
            HttpServletRequest httpRequest
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
            // 曲库改动必须留痕：此前 27 个写端点里只有认证与用户资料两类接了审计，
            // 歌曲新增/修改/删除在 operation_log 里一条都不记，
            // 事后无法回答"这首歌是谁什么时候传上来的"
            operationLogService.logAdd(operatorId, operatorName, operatorType,
                    "曲库模块", "新增歌曲 " + name + " (id=" + song.getId() + ")",
                    ipUtils.getIpAddress(httpRequest));
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
    public R<Void> updateSong(@RequestAttribute("userId") Integer operatorId,
                              @RequestAttribute("username") String operatorName,
                              @RequestAttribute("userType") String operatorType,
                              @PathVariable("id") Integer id,
                              String name, String introduction, String lyric,
                              HttpServletRequest httpRequest) {
        //保存到歌手的对象中
        Song song = new Song();
        song.setId(id);
        song.setName(name);
        song.setIntroduction(introduction);
        song.setLyric(lyric);

        //修改成功
        if (songService.update(song)) {
            operationLogService.logUpdate(operatorId, operatorName, operatorType,
                    "曲库模块", "修改歌曲 id=" + id + " 歌名=" + name,
                    ipUtils.getIpAddress(httpRequest));
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
    public R<Boolean> deleteSong(@RequestAttribute("userId") Integer operatorId,
                                 @RequestAttribute("username") String operatorName,
                                 @RequestAttribute("userType") String operatorType,
                                 @PathVariable("id") Integer id,
                                 HttpServletRequest httpRequest) {
        boolean deleted = songService.delete(id);
        // 删除是曲库里最难恢复的动作：行连同 list_song、song_tag 的关联一起消失，
        // 不记审计就只剩"这行不见了"这一个事实，连是谁删的都不知道
        if (deleted) {
            operationLogService.logDelete(operatorId, operatorName, operatorType,
                    "曲库模块", "删除歌曲 id=" + id, ipUtils.getIpAddress(httpRequest));
        }
        return R.ok(deleted);
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
            @RequestAttribute("userId") Integer operatorId,
            @RequestAttribute("username") String operatorName,
            @RequestAttribute("userType") String operatorType,
            @RequestParam("file") MultipartFile avatorFile,
            @PathVariable("id") Integer id,
            HttpServletRequest httpRequest
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
            operationLogService.logUpdate(operatorId, operatorName, operatorType,
                    "曲库模块", "更换歌曲图片 id=" + id + " → " + storeAvatorPath,
                    ipUtils.getIpAddress(httpRequest));
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
            @RequestAttribute("userId") Integer operatorId,
            @RequestAttribute("username") String operatorName,
            @RequestAttribute("userType") String operatorType,
            @RequestParam("file") MultipartFile avatorFile,
            @PathVariable("id") Integer id,
            HttpServletRequest httpRequest
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
            operationLogService.logUpdate(operatorId, operatorName, operatorType,
                    "曲库模块", "更换歌曲音频 id=" + id + " → " + storeAvatorPath,
                    ipUtils.getIpAddress(httpRequest));
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

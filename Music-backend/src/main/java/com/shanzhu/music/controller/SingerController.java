package com.shanzhu.music.controller;

import com.alibaba.fastjson2.JSONObject;
import com.github.pagehelper.PageInfo;
import com.shanzhu.music.annotation.RequireUserType;
import com.shanzhu.music.common.R;
import com.shanzhu.music.entity.dto.SingerRequest;
import com.shanzhu.music.entity.po.Singer;
import com.shanzhu.music.entity.po.Song;
import com.shanzhu.music.entity.vo.UploadPicVo;
import com.shanzhu.music.service.SingerService;
import com.shanzhu.music.service.SongService;
import com.shanzhu.music.service.FileStorageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * 歌手 控制层
 */
@RestController
@RequestMapping("/api/v1/singers")
@RequireUserType({"consumer", "admin"})
@RequiredArgsConstructor
public class SingerController {

    private final SingerService singerService;
    private final SongService songService;
    private final FileStorageService fileStorageService;

    /**
     * 添加歌手
     *
     * @param request 歌手信息
     * @return 添加结果
     */
    @PostMapping
    @RequireUserType("admin")
    public R<Void> addSinger(@Valid @RequestBody SingerRequest request) {
        // 生日转换与 DTO→PO 组装在 service，这里只做响应塑形
        if (singerService.insert(request)) {
            return R.ok("添加成功");
        }
        return R.error("添加失败");
    }

    /**
     * 修改歌手
     *
     * @param id      歌手id，以路径为准
     * @param request 歌手信息
     * @return 结果
     */
    @PatchMapping(value = "/{id}")
    @RequireUserType("admin")
    public R<Void> updateSinger(@PathVariable("id") Integer id,
                                @Valid @RequestBody SingerRequest request) {
        if (singerService.update(id, request)) {
            return R.ok("修改成功");
        }
        return R.error("修改失败");
    }

    /**
     * 删除歌手
     *
     * @param id 歌手id
     * @return 删除结果
     */
    @DeleteMapping(value = "/{id}")
    @RequireUserType("admin")
    public R<Boolean> deleteSinger(@PathVariable("id") Integer id) {
        return R.ok(singerService.delete(id));
    }

    /**
     * 查询歌手
     *
     * @param id 歌手id
     * @return 歌手
     */
    @GetMapping(value = "/{id}")
    public R<Singer> selectByPrimaryKey(@PathVariable("id") Integer id) {
        return R.ok(singerService.selectByPrimaryKey(id));
    }

    /**
     * 查询所有歌手
     *
     * @return 歌手列表
     */
    @GetMapping
    public R<List<Singer>> allSinger() {
        return R.ok(singerService.allSinger());
    }
    
    /**
     * 分页查询所有歌手
     *
     * @param pageNum 页码，默认1
     * @param pageSize 每页大小，默认10
     * @return 分页结果
     */
    @GetMapping(value = "/page")
    public R<PageInfo<Singer>> allSingerByPage(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize
    ) {
        PageInfo<Singer> pageInfo = singerService.allSingerByPage(pageNum, pageSize);
        return R.ok(pageInfo);
    }

    /**
     * 根据歌手名字模糊查询列表
     *
     * @param name 歌手名称
     * @return 歌手列表
     */
    @GetMapping(params = "name")
    public R<List<Singer>> singerOfName(String name) {
        return R.ok(singerService.singerOfName(name));
    }

    /**
     * 根据性别查询
     *
     * @param sex 性别
     * @return 歌手列表
     */
    @GetMapping(params = "sex")
    public R<List<Singer>> singerOfSex(Integer sex) {
        return R.ok(singerService.singerOfSex(sex));
    }

    /**
     * 查询某位歌手的歌曲
     *
     * 原先挂在 SongController 的 /song/manage/singer/detail?singerId= 上：
     * 读的是"某个歌手的歌曲集合"，资源归属在歌手一侧，所以按子资源表达。
     *
     * @param id 歌手id
     * @return 歌曲列表
     */
    @GetMapping(value = "/{id}/songs")
    public R<List<Song>> singerSongs(@PathVariable("id") Integer id) {
        return R.ok(songService.songOfSingerId(id));
    }

    /**
     * 更新歌手图片
     *
     * @param avatorFile 头像文件
     * @param id         更是id
     * @return 更新结果
     */
    @PostMapping(value = "/{id}/picture")
    @RequireUserType("admin")
    public R<UploadPicVo> updateSingerPic(
            @RequestParam("file") MultipartFile avatorFile,
            @PathVariable("id") Integer id
    ) throws IOException {
        String storeAvatorPath;
        try {
            storeAvatorPath = fileStorageService.saveImage(avatorFile, "img/singerPic");
        } catch (IllegalArgumentException | IllegalStateException e) {
            return R.error(e.getMessage());
        }
        Singer singer = new Singer();
        singer.setId(id);
        singer.setPic(storeAvatorPath);

        if (singerService.update(singer)) {
            UploadPicVo songListVo = new UploadPicVo();
            songListVo.setPic(storeAvatorPath);
            return R.ok("上传成功", songListVo);
        }

        return R.error("上传失败");
    }

}

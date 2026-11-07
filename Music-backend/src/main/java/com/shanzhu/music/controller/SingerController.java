package com.shanzhu.music.controller;

import com.alibaba.fastjson2.JSONObject;
import com.github.pagehelper.PageInfo;
import com.shanzhu.music.annotation.RequireUserType;
import com.shanzhu.music.common.R;
import com.shanzhu.music.entity.dto.SingerRequest;
import com.shanzhu.music.entity.po.Singer;
import com.shanzhu.music.entity.vo.UploadPicVo;
import com.shanzhu.music.service.SingerService;
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
@RequestMapping("/api/v1/singer")
@RequireUserType({"consumer", "admin"})
@RequiredArgsConstructor
public class SingerController {

    private final SingerService singerService;
    private final FileStorageService fileStorageService;

    /**
     * 添加歌手
     *
     * @param request 歌手信息
     * @return 添加结果
     */
    @PostMapping(value = "/add")
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
     * @param request 歌手信息
     * @return 结果
     */
    @PostMapping(value = "/update")
    @RequireUserType("admin")
    public R<Void> updateSinger(@Valid @RequestBody SingerRequest request) {
        if (singerService.update(request)) {
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
    @DeleteMapping(value = "/delete")
    @RequireUserType("admin")
    public R<Boolean> deleteSinger(Integer id) {
        return R.ok(singerService.delete(id));
    }

    /**
     * 查询歌手
     *
     * @param id 歌手id
     * @return 歌手
     */
    @GetMapping(value = "/selectByPrimaryKey")
    public R<Singer> selectByPrimaryKey(Integer id) {
        return R.ok(singerService.selectByPrimaryKey(id));
    }

    /**
     * 查询所有歌手
     *
     * @return 歌手列表
     */
    @GetMapping(value = "/allSinger")
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
    @GetMapping(value = "/allSingerByPage")
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
    @GetMapping(value = "/singerOfName")
    public R<List<Singer>> singerOfName(String name) {
        return R.ok(singerService.singerOfName("%" + name + "%"));
    }

    /**
     * 根据性别查询
     *
     * @param sex 性别
     * @return 歌手列表
     */
    @GetMapping(value = "/singerOfSex")
    public R<List<Singer>> singerOfSex(Integer sex) {
        return R.ok(singerService.singerOfSex(sex));
    }

    /**
     * 更新歌手图片
     *
     * @param avatorFile 头像文件
     * @param id         更是id
     * @return 更新结果
     */
    @PostMapping(value = "/updateSingerPic")
    @RequireUserType("admin")
    public R<UploadPicVo> updateSingerPic(
            @RequestParam("file") MultipartFile avatorFile,
            @RequestParam("id") Integer id
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

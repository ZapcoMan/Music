package com.shanzhu.music.controller;

import cn.hutool.core.date.DatePattern;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import com.alibaba.fastjson2.JSONObject;
import com.github.pagehelper.PageInfo;
import com.shanzhu.music.common.R;
import com.shanzhu.music.entity.po.Singer;
import com.shanzhu.music.entity.vo.UploadPicVo;
import com.shanzhu.music.service.SingerService;
import com.shanzhu.music.utils.PathUtils;
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
@RequestMapping("/singer")
@RequiredArgsConstructor
public class SingerController {

    private final SingerService singerService;

    /**
     * 添加歌手
     *
     * @param name         姓名
     * @param sex          性别
     * @param pic          头像
     * @param birth        生日
     * @param location     地区
     * @param introduction 简介
     * @return 添加结果
     */
    @PostMapping(value = "/add")
    public R addSinger(
            String name,
            Byte sex,
            String pic,
            String birth,
            String location,
            String introduction
    ) {
        //把生日转换成Date格式
        DateTime birthDate = DateUtil.parse(birth, DatePattern.NORM_DATE_PATTERN);
        //保存到歌手的对象中
        Singer singer = new Singer();
        singer.setName(name);
        singer.setSex(sex);
        singer.setPic(pic);
        singer.setBirth(birthDate);
        singer.setLocation(location);
        singer.setIntroduction(introduction);
        //保存成功
        if (singerService.insert(singer)) {
            return R.ok("添加成功");
        }
        return R.error("添加失败");
    }

    /**
     * 修改歌手
     *
     * @param name         姓名
     * @param sex          性别
     * @param birth        生日
     * @param location     地区
     * @param introduction 简介
     * @return 结果
     */
    @PostMapping(value = "/update")
    public R updateSinger(
            String id,
            String name,
            String sex,
            String birth,
            String location,
            String introduction
    ) {
        //把生日转换成Date格式
        DateTime birthDate = DateUtil.parse(birth, DatePattern.NORM_DATE_PATTERN);
        //保存到歌手的对象中
        Singer singer = new Singer();
        singer.setId(Integer.parseInt(id));
        singer.setName(name);
        singer.setSex(Byte.parseByte(sex));
        singer.setBirth(birthDate);
        singer.setLocation(location);
        singer.setIntroduction(introduction);
        //保存成功
        if (singerService.update(singer)) {
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
    @GetMapping(value = "/delete")
    public Boolean deleteSinger(Integer id) {
        return singerService.delete(id);
    }

    /**
     * 查询歌手
     *
     * @param id 歌手id
     * @return 歌手
     */
    @GetMapping(value = "/selectByP rimaryKey")
    public Singer selectByPrimaryKey(Integer id) {
        return singerService.selectByPrimaryKey(id);
    }

    /**
     * 查询所有歌手
     *
     * @return 歌手列表
     */
    @GetMapping(value = "/allSinger")
    public List<Singer> allSinger() {
        return singerService.allSinger();
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
    public List<Singer> singerOfName(String name) {
        return singerService.singerOfName("%" + name + "%");
    }

    /**
     * 根据性别查询
     *
     * @param sex 性别
     * @return 歌手列表
     */
    @GetMapping(value = "/singerOfSex")
    public List<Singer> singerOfSex(Integer sex) {
        return singerService.singerOfSex(sex);
    }

    /**
     * 更新歌手图片
     *
     * @param avatorFile 头像文件
     * @param id         更是id
     * @return 更新结果
     */
    @PostMapping(value = "/updateSingerPic")
    public R<UploadPicVo> updateSingerPic(
            @RequestParam("file") MultipartFile avatorFile,
            @RequestParam("id") Integer id
    ) throws IOException {
        JSONObject jsonObject = new JSONObject();

        if (avatorFile.isEmpty()) {
            return R.error("文件上传失败");
        }

        //文件名=当前时间到毫秒+原来的文件名
        String fileName = System.currentTimeMillis() + avatorFile.getOriginalFilename();
        //文件路径
        String filePath = PathUtils.getClassLoadRootPath() + "/img/singerPic/";
        //如果文件路径不存在，新增该路径
        File file1 = new File(filePath);
        if (!file1.exists()) {
            file1.mkdir();
        }
        //实际的文件地址
        File dest = new File(filePath + fileName);
        //存储到数据库里的相对文件地址
        String storeAvatorPath = "/img/singerPic/" + fileName;
        avatorFile.transferTo(dest);
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

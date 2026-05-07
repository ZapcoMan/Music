package com.shanzhu.music.controller;

import com.github.pagehelper.PageInfo;
import com.shanzhu.music.common.R;
import com.shanzhu.music.entity.po.Song;
import com.shanzhu.music.entity.vo.UploadPicVo;
import com.shanzhu.music.entity.vo.UploadSongVo;
import com.shanzhu.music.service.SongService;
import com.shanzhu.music.utils.PathUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * 歌曲 控制层
 */
@RestController
@RequestMapping("/song")
@RequiredArgsConstructor
public class SongController {

    private final SongService songService;

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
    @PostMapping(value = "/add")
    public R<UploadSongVo> addSong(
            String singerId,
            String name,
            String introduction,
            String pic,
            String lyric,
            @RequestParam("file") MultipartFile mpFile
    ) throws IOException {
        //上传歌曲文件
        if (mpFile.isEmpty()) {
            return R.error("歌曲上传失败");
        }

        //文件名=当前时间到毫秒+原来的文件名
        String fileName = System.currentTimeMillis() + mpFile.getOriginalFilename();
        //文件路径
        String filePath = PathUtils.getClassLoadRootPath() + "/song/";
        //如果文件路径不存在，新增该路径
        File file1 = new File(filePath);
        if (!file1.exists()) {
            file1.mkdir();
        }
        //实际的文件地址
        File dest = new File(filePath + fileName);
        //存储到数据库里的相对文件地址
        String storeUrlPath = "/song/" + fileName;

        mpFile.transferTo(dest);
        Song song = new Song();
        song.setSingerId(Integer.parseInt(singerId));
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
     * 根据歌手id查询歌曲
     *
     * @param singerId 歌手id
     * @return 歌曲列表
     */
    @GetMapping(value = "/singer/detail")
    public List<Song> songOfSingerId(Integer singerId) {
        return songService.songOfSingerId(singerId);
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
    @PostMapping(value = "/update")
    public R updateSong(Integer id, String name, String introduction, String lyric) {
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
    @GetMapping(value = "/delete")
    public Boolean deleteSinger(Integer id) {
        return songService.delete(id);
    }

    /**
     * 更新歌曲图片
     *
     * @param avatorFile 歌曲图片
     * @param id         歌曲id
     * @return 更新结果
     */
    @RequestMapping(value = "/updateSongPic", method = RequestMethod.POST)
    public R<UploadPicVo> updateSongPic(
            @RequestParam("file") MultipartFile avatorFile,
            @RequestParam("id") Integer id
    ) throws IOException {

        if (avatorFile.isEmpty()) {
            return R.error("文件上传失败");
        }

        //文件名=当前时间到毫秒+原来的文件名
        String fileName = System.currentTimeMillis() + avatorFile.getOriginalFilename();
        //文件路径
        String filePath = PathUtils.getClassLoadRootPath() + "/img/songPic/";
        //如果文件路径不存在，新增该路径
        File file1 = new File(filePath);
        if (!file1.exists()) {
            file1.mkdir();
        }
        //实际的文件地址
        File dest = new File(filePath + fileName);
        //存储到数据库里的相对文件地址
        String storeAvatorPath = "/img/songPic/" + fileName;
        avatorFile.transferTo(dest);
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
    @PostMapping(value = "/updateSongUrl")
    public R<UploadSongVo> updateSongUrl(
            @RequestParam("file") MultipartFile avatorFile,
            @RequestParam("id") Integer id
    ) throws IOException {
        if (avatorFile.isEmpty()) {
            return R.error("文件上传失败");
        }

        //文件名=当前时间到毫秒+原来的文件名
        String fileName = System.currentTimeMillis() + avatorFile.getOriginalFilename();
        //文件路径
        String filePath = PathUtils.getClassLoadRootPath() + "/song/";
        //如果文件路径不存在，新增该路径
        File file1 = new File(filePath);
        if (!file1.exists()) {
            file1.mkdir();
        }
        //实际的文件地址
        File dest = new File(filePath + fileName);
        //存储到数据库里的相对文件地址
        String storeAvatorPath = "/song/" + fileName;
        avatorFile.transferTo(dest);
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
     * @param songId 歌曲id
     * @return 歌曲
     */
    @GetMapping(value = "/detail")
    public Object detail(Integer songId) {
        return songService.selectByPrimaryKey(songId);
    }

    /**
     * 根据歌手名字精确查询歌曲
     *
     * @param songName 歌手名
     * @return 歌曲列表
     */
    @GetMapping(value = "/songOfSongName")
    public Object songOfSongName(String songName) {
        return songService.songOfName(songName);
    }

    /**
     * 根据歌手名字模糊查询歌曲
     *
     * @param songName 歌手名称
     * @return 歌曲列表
     */
    @GetMapping(value = "/likeSongOfName")
    public Object likeSongOfName(String songName) {
        return songService.likeSongOfName(songName);
    }

    /**
     * 查询所有歌曲
     *
     * @return 歌曲列表
     */
    @GetMapping(value = "/allSong")
    public Object allSong() {
        return songService.allSong();
    }
    
    /**
     * 分页查询所有歌曲
     *
     * @param pageNum 页码，默认1
     * @param pageSize 每页大小，默认10
     * @return 分页结果
     */
    @GetMapping(value = "/allSongByPage")
    public R<PageInfo<Song>> allSongByPage(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize
    ) {
        PageInfo<Song> pageInfo = songService.allSongByPage(pageNum, pageSize);
        return R.ok(pageInfo);
    }

}

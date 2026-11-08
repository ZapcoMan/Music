package com.shanzhu.music.controller;

import com.github.pagehelper.PageInfo;
import com.shanzhu.music.annotation.RequireUserType;
import com.shanzhu.music.common.R;
import com.shanzhu.music.entity.po.Song;
import com.shanzhu.music.entity.po.SongList;
import com.shanzhu.music.entity.vo.UploadPicVo;
import com.shanzhu.music.service.SongListService;
import com.shanzhu.music.service.FileStorageService;
import com.shanzhu.music.service.OperationLogService;
import com.shanzhu.music.utils.IpUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

/**
 * 歌单 控制层
 *
 * 资源式路径下不再有"动词写在方法名上"的形态：
 * 集合用 GET /api/v1/song-lists（过滤条件由 query 参数区分），
 * 单个资源用 /{id}，上传这类动作挂在 /{id}/picture 子资源上。
 */
@RestController
@RequestMapping("/api/v1/song-lists")
// 类级角色域兜底：本类未单独标注的读接口一律要求已登录（拦截器 fail-closed）；
// 方法级 @RequireUserType 优先级更高，不会被这里放宽。
@RequireUserType({"consumer", "admin"})
@RequiredArgsConstructor
public class SongListController {

    private static final Logger log = LoggerFactory.getLogger(SongListController.class);
    private final SongListService songListService;
    private final FileStorageService fileStorageService;
    private final OperationLogService operationLogService;
    private final IpUtils ipUtils;

    /**
     * 添加歌单
     *
     * @param title        标题
     * @param pic          歌单图片
     * @param introduction 简介
     * @param style        风格
     * @return 添加结果
     */
    @PostMapping
    @RequireUserType("admin")
    public R<Void> addSongList(
            @RequestAttribute("userId") Integer operatorId,
            @RequestAttribute("username") String operatorName,
            @RequestAttribute("userType") String operatorType,
            String title,
            String pic,
            String introduction,
            String style,
            HttpServletRequest httpRequest
    ) {
        //保存到歌单的对象中
        SongList songList = new SongList();
        songList.setTitle(title);
        songList.setPic(pic);
        songList.setIntroduction(introduction);
        songList.setStyle(style);

        //添加成功
        if (songListService.insert(songList)) {
            operationLogService.logAdd(operatorId, operatorName, operatorType,
                    "曲库模块", "新增歌单 " + title + " (id=" + songList.getId() + ")",
                    ipUtils.getIpAddress(httpRequest));
            return R.ok("添加成功");
        }

        return R.error("添加失败");
    }

    /**
     * 部分修改歌单
     *
     * 改哪一条以路径上的 id 为准，请求里同名的 id 字段不再采纳：
     * URL 指向 A、表单指向 B 时结果取决于参数绑定顺序，管理端一次误填就能改坏别人的歌单。
     * 图片不在这里更新，走 POST /{id}/picture；SQL 是动态 &lt;set&gt;，未提交的字段保持原值，
     * 所以语义是 PATCH 而不是 PUT。
     *
     * @param id           歌单id
     * @param title        标题
     * @param introduction 简介
     * @param style        风格
     * @return 修改结果
     */
    @PatchMapping(value = "/{id}")
    @RequireUserType("admin")
    public R<Void> updateSongList(@RequestAttribute("userId") Integer operatorId,
                                  @RequestAttribute("username") String operatorName,
                                  @RequestAttribute("userType") String operatorType,
                                  @PathVariable("id") Integer id,
                                  String title,
                                  String introduction,
                                  String style,
                                  HttpServletRequest httpRequest
    ) {
        //保存到歌单的对象中
        SongList songList = new SongList();
        songList.setId(id);
        songList.setTitle(title);
        songList.setIntroduction(introduction);
        songList.setStyle(style);

        //修改成功
        if (songListService.update(songList)) {
            operationLogService.logUpdate(operatorId, operatorName, operatorType,
                    "曲库模块", "修改歌单 id=" + id + " 标题=" + title,
                    ipUtils.getIpAddress(httpRequest));
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
    @DeleteMapping(value = "/{id}")
    @RequireUserType("admin")
    public R<Boolean> deleteSongList(@RequestAttribute("userId") Integer operatorId,
                                     @RequestAttribute("username") String operatorName,
                                     @RequestAttribute("userType") String operatorType,
                                     @PathVariable("id") Integer id,
                                     HttpServletRequest httpRequest) {
        boolean deleted = songListService.delete(id);
        // 删除歌单会级联清理 list_song、评论与评分，是最难恢复的动作，必须留痕
        if (deleted) {
            operationLogService.logDelete(operatorId, operatorName, operatorType,
                    "曲库模块", "删除歌单 id=" + id, ipUtils.getIpAddress(httpRequest));
        }
        return R.ok(deleted);
    }

    /**
     * 查询歌单
     *
     * @param id 歌单id
     * @return 歌单
     */
    @GetMapping(value = "/{id}")
    public R<SongList> selectByPrimaryKey(@PathVariable("id") Integer id) {
        return R.ok(songListService.selectByPrimaryKey(id));
    }

    /**
     * 查询所有歌单
     *
     * @return 歌单列表
     */
    @GetMapping
    public R<List<SongList>> allSongList() {
        // 不能再像旧版那样于日志里多调一次 service：那会让每次列表请求走两遍缓存/整表查询
        return R.ok(songListService.allSongList());
    }

    /**
     * 根据标题精确查询歌单列表
     *
     * @param title 标题
     * @return 歌单列表
     */
    @GetMapping(params = "title")
    public R<List<SongList>> songListOfTitle(String title) {
        return R.ok(songListService.songListOfTitle(title));
    }

    /**
     * 根据标题模糊查询歌单列表
     *
     * @param keyword 标题关键词
     * @return 歌单列表
     */
    @GetMapping(params = "keyword")
    public R<List<SongList>> likeTitle(String keyword) {
        return R.ok(songListService.likeTitle(keyword));
    }

    /**
     * 根据风格模糊查询歌单列表
     *
     * @param style 歌单风格
     * @return 歌单列表
     */
    @GetMapping(params = "style")
    public R<List<SongList>> likeStyle(String style) {
        return R.ok(songListService.likeStyle(style));
    }

    /**
     * 更新歌单图片
     *
     * 目标歌单取路径 id，理由同 PATCH：表单里再带一个 id 等于同一件事有两套权威。
     *
     * @param avatorFile 歌单图片
     * @param id         歌单id
     * @return 更新结果
     */
    @PostMapping(value = "/{id}/picture")
    @RequireUserType("admin")
    public R<UploadPicVo> updateSongListPic(
            @RequestAttribute("userId") Integer operatorId,
            @RequestAttribute("username") String operatorName,
            @RequestAttribute("userType") String operatorType,
            @RequestParam("file") MultipartFile avatorFile,
            @PathVariable("id") Integer id,
            HttpServletRequest httpRequest
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
            operationLogService.logUpdate(operatorId, operatorName, operatorType,
                    "曲库模块", "更换歌单封面 id=" + id + " → " + storeAvatorPath,
                    ipUtils.getIpAddress(httpRequest));
            UploadPicVo uploadPicVo = new UploadPicVo();
            uploadPicVo.setPic(storeAvatorPath);
            return R.ok("上传成功", uploadPicVo);
        }

        return R.error("上传失败");
    }

    /**
     * 查询推荐歌曲
     *
     * 原先是 POST /queryRecommendedSongListById?id=，id 由前端从 sessionStorage 里取出来传：
     * 1) 这是纯读操作（除 warm 一下推荐缓存外没有副作用），不该占 POST；
     * 2) 用户身份摆在请求参数里，任何消费者遍历 id 就能读到别人的推荐结果。
     * 与 /consumers/profile 同一口径：身份只取令牌，不接受外部传入的 id。
     * 载荷是推荐的"歌曲"而不是歌单，这是推荐链路的既有产品形态（算法本身挂在 SongListService 上）；
     * 挪到 songs 或 consumers 资源下要跨出本文件的改动边界，故仍留在 song-lists 域内。
     *
     * @param currentUserId 令牌中的用户id
     * @return 推荐歌曲列表
     */
    @GetMapping(value = "/recommended")
    @RequireUserType("consumer")
    public R<List<Song>> recommendedSongs(@RequestAttribute("userId") Integer currentUserId) {
        log.info("为用户 {} 生成推荐歌曲", currentUserId);
        return R.ok("OK", songListService.queryRecommendedSongById(currentUserId));
    }
    
    /**
     * 分页查询所有歌单
     *
     * @param pageNum  页码，默认1
     * @param pageSize 每页大小，默认10
     * @return 分页结果
     */
    @GetMapping(value = "/page")
    public R<PageInfo<SongList>> allSongListByPage(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize
    ) {
        PageInfo<SongList> pageInfo = songListService.allSongListByPage(pageNum, pageSize);
        return R.ok(pageInfo);
    }

}

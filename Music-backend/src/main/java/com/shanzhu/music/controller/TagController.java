package com.shanzhu.music.controller;

import com.shanzhu.music.annotation.RequireUserType;
import com.shanzhu.music.common.R;
import com.shanzhu.music.entity.dto.TagSaveRequest;
import com.shanzhu.music.entity.po.Tag;
import com.shanzhu.music.entity.po.Singer;
import com.shanzhu.music.entity.po.Song;
import com.shanzhu.music.entity.vo.TaggedSongVo;
import com.shanzhu.music.service.SingerService;
import com.shanzhu.music.service.SongService;
import com.shanzhu.music.service.TagService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 标签管理 控制层
 *
 * 存在的理由：TagMapper 里 batchInsertIgnore、selectHotTags、deleteSongTags、
 * batchInsertSongTags 这些方法长期没有任何 Java 调用方，
 * 标签系统基本停留在 TAG_SYSTEM_OPTIMIZATION.md 文档里 ——
 * 数据在库里（V6 已补齐 song_tag/singer_tag），但后台无法维护它。
 *
 * 本类不写类级 @RequestMapping：标签字典在 /api/v1/tags 下，而歌曲标签、歌手标签
 * 是别人名下的子资源（/api/v1/songs/{id}/tags、/api/v1/singers/{id}/tags），
 * 把三类路径套进同一个 /tag 前缀会让 URL 层级骗人，
 * 所以与 singers / songs 那批同构，在每个方法上写全路径。
 * 角色标注不受影响：拦截器和端点守卫都是先查方法、再查所在类，与路径前缀无关。
 */
@Slf4j
@RestController
@RequireUserType({"consumer", "admin"})
@RequiredArgsConstructor
@io.swagger.v3.oas.annotations.tags.Tag(name = "标签管理", description = "歌曲标签查询与维护接口")
public class TagController {

    private final TagService tagService;
    private final SongService songService;
    private final SingerService singerService;

    /**
     * 热门标签，供前台筛选与后台标签云使用
     *
     * @param limit 数量上限
     * @return 标签列表，按关联歌曲数倒序
     */
    @Operation(summary = "热门标签", description = "按关联歌曲数倒序返回标签")
    @GetMapping(value = "/api/v1/tags/hot")
    public R<List<Tag>> hot(@Parameter(description = "数量上限", example = "10") @RequestParam(defaultValue = "10") int limit) {
        // 上限夹到 50：这个接口出现在首页一类的公开路径上，不该被用来整表拉取
        return R.ok(tagService.hotTags(Math.min(Math.max(limit, 1), 50)));
    }

    /**
     * 查询某首歌的标签
     *
     * @param songId 歌曲id
     * @return 标签名列表，取自中间表这一真源
     */
    @Operation(summary = "查询歌曲标签", description = "从 song_tag 中间表读取，避免逗号串滞后")
    @GetMapping(value = "/api/v1/songs/{songId}/tags")
    public R<List<String>> ofSong(@Parameter(description = "歌曲ID", required = true)
                                  @PathVariable("songId") Integer songId) {
        return R.ok(tagService.songTagNames(songId));
    }

    /**
     * 按标签查歌曲，返回带匹配度的结果
     *
     * @param tagIds 标签id列表
     * @param limit  数量上限
     * @return 歌曲及其 matchCount
     */
    @Operation(summary = "按标签查歌曲", description = "返回结果携带 matchCount，可解释命中多少个偏好标签")
    @GetMapping(value = "/api/v1/tags/songs")
    public R<List<TaggedSongVo>> songs(@RequestParam List<Integer> tagIds,
                                       @RequestParam(defaultValue = "20") int limit) {
        // 空集合直接给明确的客户端错误，而不是让 service 返回空列表
        // 让人误以为"这些标签下没有歌"
        if (tagIds == null || tagIds.isEmpty()) {
            return R.error("tagIds 不能为空");
        }
        return R.ok(tagService.selectSongsByTagIds(tagIds, limit));
    }

    /**
     * 设置某首歌的标签（整组替换）
     *
     * @param songId 歌曲id
     * @param tags   标签名数组，空数组表示清空
     * @return 生效的标签数量
     */
    @Operation(summary = "设置歌曲标签", description = "整组替换：先删后插重建关联，并同步 song.tags 逗号串")
    @PutMapping(value = "/api/v1/songs/{songId}/tags")
    @RequireUserType("admin")
    public R<Integer> updateSongTags(@RequestAttribute("userId") Integer operatorId,
                                     @Parameter(description = "歌曲ID", required = true)
                                     @PathVariable("songId") Integer songId,
                                     // 标签走 JSON 数组而不是查询串逗号分隔：名字本身含逗号时会被拆成两个，
                                     // 列表也不该为了迁就旧的表单式入参留在 URL 上
                                     @RequestBody(required = false) List<String> tags) {
        if (songId == null) {
            return R.error("songId 不能为空");
        }
        if (tags == null) {
            // 明确传空数组表示清空标签，缺省则拒绝，避免误清
            return R.error("tags 不能为空，清空请传空数组");
        }
        try {
            // 必须走歌曲自身的更新路径，而不是只重建中间表：
            // song.tags 逗号串与缓存都在那条路径上写，绕开它就会出现
            // "中间表已改、逗号串仍是旧值"的两轨背离（本接口初版正是如此）。
            Song song = new Song();
            song.setId(songId);
            song.setTags(tagService.normalizeTagNames(tags));
            if (!songService.update(song)) {
                return R.error("设置失败：歌曲不存在或更新未生效");
            }
            return R.ok("设置成功", tagService.songTagNames(songId).size());
        } catch (IllegalArgumentException e) {
            return R.error(e.getMessage());
        }
    }

    /**
     * 查询某位歌手的标签
     *
     * @param singerId 歌手id
     * @return 标签名列表，取自 singer_tag 中间表
     */
    @Operation(summary = "查询歌手标签", description = "从 singer_tag 中间表读取，避免逗号串滞后")
    @GetMapping(value = "/api/v1/singers/{singerId}/tags")
    public R<List<String>> ofSinger(@Parameter(description = "歌手ID", required = true)
                                    @PathVariable("singerId") Integer singerId) {
        return R.ok(tagService.singerTagNames(singerId));
    }

    /**
     * 设置某位歌手的标签（整组替换）
     *
     * @param singerId 歌手id
     * @param tags     标签名数组，空数组表示清空
     * @return 生效的标签数量
     */
    @Operation(summary = "设置歌手标签", description = "整组替换：先删后插重建关联，并同步 singer.tags 逗号串")
    @PutMapping(value = "/api/v1/singers/{singerId}/tags")
    @RequireUserType("admin")
    public R<Integer> updateSingerTags(@RequestAttribute("userId") Integer operatorId,
                                       @Parameter(description = "歌手ID", required = true)
                                       @PathVariable("singerId") Integer singerId,
                                       @RequestBody(required = false) List<String> tags) {
        if (singerId == null) {
            return R.error("singerId 不能为空");
        }
        if (tags == null) {
            return R.error("tags 不能为空，清空请传空数组");
        }
        try {
            // 走歌手自身的更新路径：逗号串写入与缓存失效都在它身上
            Singer singer = new Singer();
            singer.setId(singerId);
            singer.setTags(tagService.normalizeTagNames(tags));
            if (!singerService.update(singer)) {
                return R.error("设置失败：歌手不存在或更新未生效");
            }
            return R.ok("设置成功", tagService.singerTagNames(singerId).size());
        } catch (IllegalArgumentException e) {
            return R.error(e.getMessage());
        }
    }

    /**
     * 批量新增标签到字典（已存在的按名字忽略）
     *
     * @param tags 标签列表，name 必填，type 取 0风格1情感2场景3语言4其他
     * @return 新增条数
     */
    @Operation(summary = "批量新增标签", description = "按名字去重写入标签字典；name 空白的条目跳过")
    @PostMapping(value = "/api/v1/tags")
    @RequireUserType("admin")
    public R<Integer> batchAdd(@RequestBody List<TagSaveRequest> tags) {
        if (tags == null || tags.isEmpty()) {
            return R.error("标签列表不能为空");
        }
        // 校验与落库都在 TagService#batchAdd：原先这两步写在 Controller 里，
        // 还直接接了持久层对象 List<Tag> 并就地改写它的 name/type——
        // web 层因此可以往实体里塞任意字段，其中一部分会被当成列写进库。
        int submitted = tagService.batchAdd(tags);
        if (submitted == 0) {
            return R.error("没有有效的标签名");
        }
        return R.ok("提交成功", submitted);
    }

}

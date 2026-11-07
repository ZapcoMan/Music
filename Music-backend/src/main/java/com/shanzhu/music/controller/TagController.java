package com.shanzhu.music.controller;

import com.shanzhu.music.annotation.RequireUserType;
import com.shanzhu.music.common.R;
import com.shanzhu.music.entity.po.Tag;
import com.shanzhu.music.entity.po.Song;
import com.shanzhu.music.entity.vo.TaggedSongVo;
import com.shanzhu.music.service.SongService;
import com.shanzhu.music.mapper.TagMapper;
import com.shanzhu.music.service.TagService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

/**
 * 标签管理 控制层
 *
 * 存在的理由：TagMapper 里 batchInsertIgnore、selectHotTags、deleteSongTags、
 * batchInsertSongTags 这些方法长期没有任何 Java 调用方，
 * 标签系统基本停留在 TAG_SYSTEM_OPTIMIZATION.md 文档里 ——
 * 数据在库里（V6 已补齐 song_tag/singer_tag），但后台无法维护它。
 */
@Slf4j
@RestController
@RequestMapping("/tag")
@RequiredArgsConstructor
@io.swagger.v3.oas.annotations.tags.Tag(name = "标签管理", description = "歌曲标签查询与维护接口")
public class TagController {

    private final TagService tagService;
    private final TagMapper tagMapper;
    private final SongService songService;

    /**
     * 热门标签，供前台筛选与后台标签云使用
     *
     * @param limit 数量上限
     * @return 标签列表，按关联歌曲数倒序
     */
    @Operation(summary = "热门标签", description = "按关联歌曲数倒序返回标签")
    @GetMapping(value = "/hot")
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
    @GetMapping(value = "/ofSong")
    public R<List<String>> ofSong(@Parameter(description = "歌曲ID", required = true) Integer songId) {
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
    @GetMapping(value = "/songs")
    public R<List<TaggedSongVo>> songs(@RequestParam List<Integer> tagIds,
                                       @RequestParam(defaultValue = "20") int limit) {
        if (tagIds == null || tagIds.isEmpty()) {
            return R.error("tagIds 不能为空");
        }
        return R.ok(tagMapper.selectSongsByTagIds(tagIds, Math.min(Math.max(limit, 1), 100)));
    }

    /**
     * 设置某首歌的标签（整体替换）
     *
     * @param songId 歌曲id
     * @param tags   逗号分隔的标签名
     * @return 生效的标签数量
     */
    @Operation(summary = "设置歌曲标签", description = "先删后插重建关联，并同步 song.tags 逗号串")
    @PostMapping(value = "/song/update")
    @RequireUserType("admin")
    public R<Integer> updateSongTags(@RequestAttribute("userId") Integer operatorId,
                                     Integer songId, String tags) {
        if (songId == null) {
            return R.error("songId 不能为空");
        }
        if (tags == null) {
            // 明确传空串表示清空标签，缺省则拒绝，避免误清
            return R.error("tags 不能为空，清空请传空串");
        }
        try {
            // 必须走歌曲自身的更新路径，而不是只重建中间表：
            // song.tags 逗号串与缓存都在那条路径上写，绕开它就会出现
            // "中间表已改、逗号串仍是旧值"的两轨背离（本接口初版正是如此）。
            Song song = new Song();
            song.setId(songId);
            song.setTags(tagService.normalizeTags(tags));
            if (!songService.update(song)) {
                return R.error("设置失败：歌曲不存在或更新未生效");
            }
            return R.ok("设置成功", tagService.songTagNames(songId).size());
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
    @Operation(summary = "批量新增标签", description = "按名字去重写入标签字典")
    @PostMapping(value = "/batchAdd")
    @RequireUserType("admin")
    public R<Integer> batchAdd(@RequestBody List<Tag> tags) {
        if (tags == null || tags.isEmpty()) {
            return R.error("标签列表不能为空");
        }
        List<Tag> valid = new ArrayList<>();
        for (Tag tag : tags) {
            if (tag == null || tag.getName() == null || tag.getName().isBlank()) {
                continue;
            }
            tag.setName(tag.getName().trim());
            if (tag.getType() == null) {
                tag.setType((byte) 4);   // 4 其它：字典里 type 允许缺省，落到最宽的一类
            }
            valid.add(tag);
        }
        if (valid.isEmpty()) {
            return R.error("没有有效的标签名");
        }
        // INSERT IGNORE 的返回值不能当"新增条数"用：实测重复项被忽略时它仍返回提交条数，
        // 直接透给调用方会谎报。这里只报提交条数，是否真正入库由字典表自身去重保证。
        tagMapper.batchInsertIgnore(valid);
        log.info("批量新增标签 - 提交 {} 条（按名字去重，重复项忽略）", valid.size());
        return R.ok("提交成功", valid.size());
    }

}

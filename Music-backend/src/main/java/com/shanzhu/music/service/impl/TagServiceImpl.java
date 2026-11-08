package com.shanzhu.music.service.impl;

import com.shanzhu.music.entity.dto.TagSaveRequest;
import com.shanzhu.music.entity.po.Tag;
import com.shanzhu.music.entity.vo.TaggedSongVo;
import com.shanzhu.music.mapper.TagMapper;
import com.shanzhu.music.service.TagService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 标签服务实现：song_tag 为真源，song.tags 逗号串是它的派生视图。
 *
 * 派生视图不可手工编辑：任何绕过本类的写入（直接 UPDATE song SET tags=...）
 * 都会让两侧重新分叉。写路径收敛之前留下的历史行由 V13 一次性按中间表重算。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TagServiceImpl implements TagService {

    private final TagMapper tagMapper;

    /**
     * 按标签查歌的条数上限。夹取放在服务层而不是 Controller，
     * 是为了让"换一种调用方式"也绕不过去。
     */
    private static final int SONGS_BY_TAG_MAX = 100;

    /**
     * 标签类型缺省值：4 其它。字典表允许 type 为空，
     * 但留空的那条在标签云按类型筛选时谁也筛不到，等于写进去就失效。
     */
    private static final byte DEFAULT_TYPE = 4;

    @Override
    public String normalizeTags(String tagsCsv) {
        if (tagsCsv == null) {
            return null;
        }
        List<String> names = new ArrayList<>();
        for (String raw : tagsCsv.split(",")) {
            names.add(raw.trim());
        }
        return canonicalize(names);
    }

    @Override
    public String normalizeTagNames(List<String> tagNames) {
        if (tagNames == null) {
            return null;
        }
        List<String> names = new ArrayList<>();
        for (String raw : tagNames) {
            // 数组入参没有分隔符语义，null 元素按空白项跳过而不是拼进校验
            names.add(raw == null ? "" : raw.trim());
        }
        return canonicalize(names);
    }

    /**
     * 两种入参形态共用的校验主体：去空白、去重、保持输入顺序、逐个确认在字典里。
     *
     * @param names 已去掉首尾空白的标签名，允许出现空项（由逗号串的空白段带来）
     * @return 规范化后的逗号串
     */
    private String canonicalize(List<String> names) {
        // 去空白、去重，并保持输入顺序
        Set<String> distinct = new LinkedHashSet<>();
        for (String name : names) {
            if (!name.isEmpty()) {
                distinct.add(name);
            }
        }
        List<String> canonical = new ArrayList<>();
        for (String name : distinct) {
            Tag tag = tagMapper.selectByName(name);
            if (tag == null) {
                // 不静默丢弃：字典里没有的标签名判为非法输入。
                // 否则调用方以为设置了，实际少一个且毫无察觉
                throw new IllegalArgumentException("标签不存在: " + name);
            }
            canonical.add(tag.getName());
        }
        return String.join(",", canonical);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int replaceSongTags(Integer songId, String tagsCsv) {
        if (songId == null) {
            return 0;
        }
        List<Integer> tagIds = new ArrayList<>();
        if (tagsCsv != null && !tagsCsv.isEmpty()) {
            for (String name : tagsCsv.split(",")) {
                Tag tag = tagMapper.selectByName(name.trim());
                if (tag != null) {
                    tagIds.add(tag.getId());
                }
            }
        }
        // 先删后插，等价于"以本次输入为准"的整体替换
        tagMapper.deleteSongTags(songId);
        if (!tagIds.isEmpty()) {
            tagMapper.batchInsertSongTags(songId, tagIds);
        }
        log.info("重建歌曲标签关联 - songId={}, tags={}, 数量={}", songId, tagsCsv, tagIds.size());
        return tagIds.size();
    }

    @Override
    public List<String> songTagNames(Integer songId) {
        List<String> result = new ArrayList<>();
        List<Tag> tags = tagMapper.selectTagsBySongId(songId);
        if (tags != null) {
            for (Tag tag : tags) {
                result.add(tag.getName());
            }
        }
        return result;
    }

    @Override
    public List<Tag> hotTags(int limit) {
        return tagMapper.selectHotTags(limit);
    }

    @Override
    public List<TaggedSongVo> selectSongsByTagIds(List<Integer> tagIds, int limit) {
        if (tagIds == null || tagIds.isEmpty()) {
            return List.of();
        }
        int capped = Math.min(Math.max(limit, 1), SONGS_BY_TAG_MAX);
        return tagMapper.selectSongsByTagIds(tagIds, capped);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int batchAdd(List<TagSaveRequest> requests) {
        if (requests == null || requests.isEmpty()) {
            return 0;
        }
        List<Tag> valid = new ArrayList<>();
        for (TagSaveRequest request : requests) {
            if (request == null || request.getName() == null || request.getName().isBlank()) {
                continue;
            }
            Tag tag = new Tag();
            tag.setName(request.getName().trim());
            // type 缺省落到 4（其他）：字典表允许该列为空，但一旦留空，
            // 标签云按类型筛选时这条就谁也筛不到，等于写进去就失效
            tag.setType(request.getType() != null ? request.getType() : DEFAULT_TYPE);
            valid.add(tag);
        }
        if (valid.isEmpty()) {
            return 0;
        }
        // INSERT IGNORE 的返回值不能当"新增条数"用：实测重复项被忽略时它仍返回提交条数，
        // 直接透给调用方会谎报。这里只报提交条数，是否真正入库由字典表自身去重保证。
        tagMapper.batchInsertIgnore(valid);
        log.info("批量新增标签 - 提交 {} 条（按名字去重，重复项忽略）", valid.size());
        return valid.size();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int replaceSingerTags(Integer singerId, String tagsCsv) {
        if (singerId == null) {
            return 0;
        }
        List<Integer> tagIds = new ArrayList<>();
        if (tagsCsv != null && !tagsCsv.isEmpty()) {
            for (String name : tagsCsv.split(",")) {
                Tag tag = tagMapper.selectByName(name.trim());
                if (tag != null) {
                    tagIds.add(tag.getId());
                }
            }
        }
        tagMapper.deleteSingerTags(singerId);
        if (!tagIds.isEmpty()) {
            tagMapper.batchInsertSingerTags(singerId, tagIds);
        }
        log.info("重建歌手标签关联 - singerId={}, tags={}, 数量={}", singerId, tagsCsv, tagIds.size());
        return tagIds.size();
    }

    @Override
    public List<String> singerTagNames(Integer singerId) {
        List<String> result = new ArrayList<>();
        List<Tag> tags = tagMapper.selectTagsBySingerId(singerId);
        if (tags != null) {
            for (Tag tag : tags) {
                result.add(tag.getName());
            }
        }
        return result;
    }
}

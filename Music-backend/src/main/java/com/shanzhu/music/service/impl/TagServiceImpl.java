package com.shanzhu.music.service.impl;

import com.shanzhu.music.entity.po.Tag;
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
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TagServiceImpl implements TagService {

    private final TagMapper tagMapper;

    @Override
    public String normalizeTags(String tagsCsv) {
        if (tagsCsv == null) {
            return null;
        }
        // 去空白、去重，并保持输入顺序
        Set<String> names = new LinkedHashSet<>();
        for (String raw : tagsCsv.split(",")) {
            String name = raw.trim();
            if (!name.isEmpty()) {
                names.add(name);
            }
        }
        List<String> canonical = new ArrayList<>();
        for (String name : names) {
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

package com.shanzhu.music.entity.vo;

import com.shanzhu.music.entity.po.Song;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 带匹配度的歌曲。
 *
 * selectSongsByTagIds 的 SQL 一直算出 COUNT(DISTINCT st.tag_id) as match_count
 * 并按它倒序排，但返回类型是 Song，Song 没有对应属性 ——
 * 排序用的是这个值，出参里却看不见它，等于"按一个无法解释的数字排序"。
 * 继承 Song 是为了让召回链路仍能把它当 Song 用，不必到处改类型。
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class TaggedSongVo extends Song {

    /**
     * 命中了多少个用户偏好标签，值越大表示与该用户越相关
     */
    private Integer matchCount;

}

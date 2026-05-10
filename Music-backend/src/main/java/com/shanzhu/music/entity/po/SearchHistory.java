package com.shanzhu.music.entity.po;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 用户搜索历史
 */
@Data
public class SearchHistory implements Serializable {

    /**
     * 主键
     */
    private Integer id;

    /**
     * 用户ID
     */
    private Integer userId;

    /**
     * 搜索关键词
     */
    private String keywords;

    /**
     * 搜索类型（0歌曲1歌手2歌单）
     */
    private Byte searchType;

    /**
     * 搜索时间
     */
    private Date createTime;

}

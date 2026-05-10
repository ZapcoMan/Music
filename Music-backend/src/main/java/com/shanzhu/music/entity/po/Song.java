package com.shanzhu.music.entity.po;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 歌曲
 */
@Data
public class Song implements Serializable {

    /**
     * 主键
     */
    private Integer id;

    /**
     * 歌手id
     */
    private Integer singerId;

    /**
     * 歌名
     */
    private String name;

    /**
     * 简介
     */
    private String introduction;

    /**
     * 歌曲标签（逗号分隔，如：流行,抒情,伤感）
     */
    private String tags;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 更新时间
     */
    private Date updateTime;

    /**
     * 歌曲图片
     */
    private String pic;

    /**
     * 歌词
     */
    private String lyric;

    /**
     * 歌曲地址
     */
    private String url;

}

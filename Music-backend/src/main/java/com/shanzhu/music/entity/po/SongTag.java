package com.shanzhu.music.entity.po;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 歌曲标签关联实体
 */
@Data
public class SongTag implements Serializable {

    /**
     * 主键
     */
    private Integer id;

    /**
     * 歌曲ID
     */
    private Integer songId;

    /**
     * 标签ID
     */
    private Integer tagId;

    /**
     * 关联时间
     */
    private Date createTime;
}

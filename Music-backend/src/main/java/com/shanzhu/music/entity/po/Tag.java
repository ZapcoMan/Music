package com.shanzhu.music.entity.po;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 标签实体
 */
@Data
public class Tag implements Serializable {

    /**
     * 标签ID
     */
    private Integer id;

    /**
     * 标签名称
     */
    private String name;

    /**
     * 标签类型（0风格1情感2场景3语言4其他）
     */
    private Byte type;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 更新时间
     */
    private Date updateTime;
}

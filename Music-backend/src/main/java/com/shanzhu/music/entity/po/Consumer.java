package com.shanzhu.music.entity.po;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.Date;

/**
 * 前端用户
 *
 * ignoreUnknown 用于兼容 Redis 中的历史缓存：
 * 加 @JsonIgnore 之前写入的 JSON 仍带 password 字段，
 * 若不忽略未知属性，旧缓存读取会直接抛反序列化异常。
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class Consumer {

    /**
     * 主键
     */
    private Integer id;

    /**
     * 账号
     */
    private String username;

    /**
     * 密码（BCrypt 哈希）
     * 只作为入参参与校验与写入，任何出参都不应返回它：
     * 该对象会被整体缓存进 Redis 并随查询接口返回，序列化出去即凭据外泄。
     */
    @JsonIgnore
    private String password;

    /**
     * 性别
     */
    private Byte sex;

    /**
     * 手机号
     */
    private String phoneNum;

    /**
     * 电子邮箱
     */
    private String email;

    /**
     * 生日
     */
    private Date birth;

    /**
     * 签名
     */
    private String introduction;

    /**
     * 地区
     */
    private String location;

    /**
     * 头像
     */
    private String avator;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 更新时间
     */
    private Date updateTime;

}

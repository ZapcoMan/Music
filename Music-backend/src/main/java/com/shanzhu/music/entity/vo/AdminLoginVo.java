package com.shanzhu.music.entity.vo;

import lombok.Data;

/**
 * 管理员登录出参。
 * 与 ConsumerLoginVo 同形但字段不同：管理端登录不返回实体本身，只回账号名，
 * 避免把 admin.password 带进出参序列化路径。
 */
@Data
public class AdminLoginVo {

    /**
     * 管理员账号名
     */
    private String name;

    /**
     * JWT Token
     */
    private String token;

    /**
     * Token 有效期（秒）
     */
    private long expiresIn;

}

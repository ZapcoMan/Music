package com.shanzhu.music.entity.vo;

import com.shanzhu.music.entity.po.Consumer;
import lombok.Data;

/**
 * 前端用户vo
 */
@Data
public class ConsumerLoginVo {

    /**
     * 用户信息
     */
    private Consumer userMsg;

    /**
     * JWT Token，前端存入 localStorage 后由请求拦截器带成 Authorization 头
     */
    private String token;

    /**
     * Token 有效期（秒），由 jwt.expiration 派生
     */
    private long expiresIn;

}


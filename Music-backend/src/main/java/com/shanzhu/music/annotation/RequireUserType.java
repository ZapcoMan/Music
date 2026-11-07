package com.shanzhu.music.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标注接口所需的用户角色（admin / consumer）。
 *
 * 由 JwtAuthenticationInterceptor 在令牌校验通过后，比对令牌里的 userType claim；
 * 不匹配返回 403。未标注的接口只要求已登录，不限定角色。
 * 管理端与用户端共用同一个签名密钥，因此必须有这一层角色隔离，
 * 否则消费者令牌可直接调用管理接口。
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface RequireUserType {

    /**
     * 要求的角色：admin 或 consumer
     */
    String value();
}

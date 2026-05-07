package com.shanzhu.music.annotation;

import java.lang.annotation.*;

/**
 * API限流注解
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RateLimit {

    /**
     * 限流key前缀
     */
    String key() default "rate_limit:";

    /**
     * 限流时间窗口(秒),默认60秒
     */
    int timeWindow() default 60;

    /**
     * 限流次数,默认100次
     */
    int maxRequests() default 100;

    /**
     * 限流提示信息
     */
    String message() default "访问过于频繁,请稍后再试";
}

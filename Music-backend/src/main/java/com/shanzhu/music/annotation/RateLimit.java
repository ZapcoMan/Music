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

    /**
     * Redis 不可用时的降级方向，默认 false（拒绝请求）。
     *
     * 挂在登录、注册、改密、删账号上的限流是爆破与滥用的唯一闸门，
     * 缓存层故障期间放开它，等于在最需要防护的时候解除防护。
     * 纯读取类接口若希望维持可用，显式写 failOpen = true。
     *
     * 之所以要显式声明：此前拦截器指望 increment() 返回 null 来识别"Redis 异常"，
     * 但连接失败时 spring-data-redis 抛的是 DataAccessException。真实行为既不是
     * 放行也不是限流，而是异常一路冒到全局处理变成 500——
     * 注释里写的"不阻断业务"从来没有成立过。
     */
    boolean failOpen() default false;
}

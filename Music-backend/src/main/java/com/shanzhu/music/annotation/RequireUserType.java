package com.shanzhu.music.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标注接口允许的角色（admin / consumer），可声明多个。
 * <p>
 * 由 JwtAuthenticationInterceptor 在令牌校验通过后，比对令牌里的 userType claim；
 * 不匹配返回 403。
 * <p>
 * 为什么是数组而不是单值：评论的 update 与 delete 同时允许"本人 consumer"和"管理员"
 * 两类调用者（管理员可清理违规评论），单值表达不了这个集合，此前只能靠方法体里
 * 手写 "admin".equals(userType) 分支兜住——那是把角色策略散进了业务代码。
 * <p>
 * 未标注的接口只要求已登录，不限定角色；但**写接口（POST/PUT/DELETE/PATCH）未标注
 * 会被拦截器直接拒绝**（fail-closed），所以新增写接口必须显式声明角色域。
 * 单值写法 @RequireUserType("admin") 在数组属性下依然合法，无需改成大括号。
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface RequireUserType {

    /**
     * 允许的角色，命中其中之一即通过
     */
    String[] value();
}

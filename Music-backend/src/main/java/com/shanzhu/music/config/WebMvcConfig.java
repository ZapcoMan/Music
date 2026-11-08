package com.shanzhu.music.config;

import com.shanzhu.music.interceptor.JwtAuthenticationInterceptor;
import com.shanzhu.music.interceptor.RateLimitInterceptor;
import com.shanzhu.music.interceptor.RequestResponseLoggingInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Mvc配置
 */
@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final JwtAuthenticationInterceptor jwtAuthenticationInterceptor;
    private final RateLimitInterceptor rateLimitInterceptor;
    private final RequestResponseLoggingInterceptor requestResponseLoggingInterceptor;

    /**
     * 跨域配置统一由 SecurityConfig 的 CorsConfigurationSource 提供。
     * 这里此前也定义了一份 allowedOriginPatterns("*")，两处配置各自演进且都允许任意来源，
     * 重复定义会让实际生效的一方难以判断，故删除。
     *
     * @param registry CorsRegistry
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        // no-op：见 SecurityConfig#corsConfigurationSource
    }

    /**
     * 注册拦截器
     *
     * @param registry InterceptorRegistry
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 请求/响应日志拦截器 - 最先执行,记录所有请求
        registry.addInterceptor(requestResponseLoggingInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns(
                        "/actuator/**",      // 排除监控端点
                        "/swagger-ui/**",    // 排除Swagger UI
                        "/v3/api-docs/**",   // 排除API文档
                        "/error"             // 排除错误页面
                )
                .order(0);

        // JWT认证拦截器 - 除白名单外一律要求携带有效 Token。
        //
        // 这里只排除"与 HTTP 方法无关"的入口：静态资源、后台页面、接口文档、错误页。
        // 登录/注册这类"同一字面上有匿名方法"的入口改由
        // JwtAuthenticationInterceptor#ANONYMOUS_ENTRIES 按 (方法, 路径) 精确放行——
        // excludePathPatterns 认路径不认方法，用它放行 POST /sessions 会把
        // DELETE /sessions（登出，必须带令牌）一起放成匿名可调。
        registry.addInterceptor(jwtAuthenticationInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns(
                        // 原先这里还排除 "/manage"：那是 AdminController 上一个把视图名
                        // 写成 /src/main/resources/static/screenIndex/index.html 的端点，
                        // 而 resources/static 目录并不存在，它只会 500；前端也没有任何地方引用它。
                        // 端点已删，这条匿名入口一并撤掉——少一个匿名面，就少一处要人记着的例外。
                        "/img/**",               // 静态图片
                        // 静态音频。只放行单层 /song/*：音频文件的落盘路径就是
                        // /song/{时间戳-随机名}（见 FileStorageService.store），全部单层且多数无扩展名，
                        // 因此既不能整体放行 /song/**（此前正是这样，把 SongController 的
                        // add/update/delete/updateSongPic/updateSongUrl 一起放成了匿名可调），
                        // 也不能按扩展名收窄（库里 50+ 条历史 URL 形如 /song/1715868205989，没有后缀）。
                        // 这些前缀必须保持裸路径：<audio>/<img> 发不出 Authorization 头，
                        // 而且库里存的历史 URL 就是 /song/xxx 这种形态，加 /api/v1 会让全库歌曲集体 404。
                        // 业务接口因此下沉一层到 /song/manage/*，天然落在本条排除之外。
                        "/song/*",
                        "/userImages/**",        // 用户头像
                        "/v3/api-docs/**",       // OpenAPI 文档
                        "/swagger-ui/**",
                        "/swagger-ui.html",
                        "/actuator/health",      // 容器健康检查
                        "/error"
                )
                .order(1);

        // API限流拦截器 - 只对标注了@RateLimit的接口生效
        registry.addInterceptor(rateLimitInterceptor)
                .addPathPatterns("/**")
                .order(2);
    }

}

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
     * 增加跨域支持
     *
     * @param registry CorsRegistry
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOriginPatterns("*")
//                        "http://localhost:8080",   // MusicUser-frontend
//                        "http://localhost:8081",   // MusicAdmin-frontend
//                        "http://localhost:9172"    // MusicAdmin-frontend (alternative port)

                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);  // 预检请求缓存1小时
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
        // 免登录接口在此显式声明，而不是靠拦截器"无 Token 就放行"
        registry.addInterceptor(jwtAuthenticationInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns(
                        "/admin/login/status",   // 管理员登录
                        "/consumer/login",       // 用户登录
                        "/consumer/add",         // 用户注册
                        "/manage",               // 管理端页面入口
                        "/img/**",               // 静态图片
                        "/song/**",              // 静态音频
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

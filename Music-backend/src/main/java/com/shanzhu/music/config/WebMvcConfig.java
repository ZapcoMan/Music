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
                .allowedOrigins(
                        "http://localhost:8080",   // MusicUser-frontend
                        "http://localhost:8081",   // MusicAdmin-frontend
                        "http://localhost:9172"    // MusicAdmin-frontend (alternative port)
                )
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

        // JWT认证拦截器 - 对所有接口生效,但只在有Token时验证
        registry.addInterceptor(jwtAuthenticationInterceptor)
                .addPathPatterns("/**")
                .order(1);

        // API限流拦截器 - 只对标注了@RateLimit的接口生效
        registry.addInterceptor(rateLimitInterceptor)
                .addPathPatterns("/**")
                .order(2);
    }

}

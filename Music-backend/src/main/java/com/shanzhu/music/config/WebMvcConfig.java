package com.shanzhu.music.config;

import com.shanzhu.music.interceptor.JwtAuthenticationInterceptor;
import com.shanzhu.music.interceptor.RateLimitInterceptor;
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
                        "http://localhost:8081"    // MusicAdmin-frontend
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

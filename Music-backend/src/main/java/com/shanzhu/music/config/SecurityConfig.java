package com.shanzhu.music.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

/**
 * Spring Security 配置
 * 
 * 说明:
 * 1. 本项目使用自定义 JWT 认证拦截器(JwtAuthenticationInterceptor)
 * 2. Spring Security 仅用于提供 BCrypt 密码加密功能
 * 3. 禁用默认的安全过滤,避免与自定义拦截器冲突
 * 4. 配置 CORS 支持,允许前端跨域访问
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    /**
     * 配置安全过滤链
     * 
     * @param http HttpSecurity 对象
     * @return SecurityFilterChain
     * @throws Exception 配置异常
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // 禁用 CSRF(前后端分离项目使用 JWT,不需要 CSRF 保护)
            .csrf(csrf -> csrf.disable())
            
            // 配置会话管理为无状态(使用 JWT,不需要 Session)
            .sessionManagement(session -> 
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )
            
            // 配置授权规则
            .authorizeHttpRequests(auth -> auth
                // 放行所有请求(由自定义 JwtAuthenticationInterceptor 处理认证)
                .anyRequest().permitAll()
            )
            
            // 配置 CORS
            .cors(cors -> cors.configurationSource(corsConfigurationSource()));
        
        return http.build();
    }

    /**
     * 允许的前端来源，取自配置。
     * 此前写的是 allowedOriginPatterns("*") 且 allowCredentials(true)，
     * 等于任意站点都能带凭据发起跨域调用；文档中声称只允许 localhost:8080/8081，
     * 与实际配置不符。开启强制鉴权后，通配来源会成为令牌窃取面，必须收敛为白名单。
     */
    @Value("${security.cors.allowed-origins:http://localhost:8080,http://localhost:8081,http://localhost:9172}")
    private List<String> allowedOrigins;

    /**
     * CORS 配置源
     * 全局唯一出处，WebMvcConfig 中不再重复定义
     * 
     * @return CorsConfigurationSource
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        
        // 允许的源：allowCredentials 为 true 时不能使用通配符，必须精确匹配
        configuration.setAllowedOrigins(allowedOrigins);
        
        // 允许的 HTTP 方法
        configuration.setAllowedMethods(Arrays.asList(
            "GET", "POST", "PUT", "DELETE", "OPTIONS"
        ));
        
        // 允许的请求头
        configuration.setAllowedHeaders(List.of("*"));
        
        // 允许携带凭证(Cookie、Authorization 头等)
        configuration.setAllowCredentials(true);
        
        // 预检请求缓存时间(秒)
        configuration.setMaxAge(3600L);
        
        // 暴露的响应头(让前端可以读取)
        configuration.setExposedHeaders(List.of(
            "Authorization",
            "Content-Type"
        ));
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        // 对所有路径应用 CORS 配置
        source.registerCorsConfiguration("/**", configuration);
        
        return source;
    }

    /**
     * 密码编码器(BCrypt)
     * 用于 ConsumerService 和 AdminService 的密码加密/验证
     * 
     * @return PasswordEncoder
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}

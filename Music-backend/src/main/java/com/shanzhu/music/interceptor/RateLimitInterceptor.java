package com.shanzhu.music.interceptor;

import com.alibaba.fastjson2.JSON;
import com.shanzhu.music.annotation.RateLimit;
import com.shanzhu.music.common.R;
import com.shanzhu.music.utils.IpUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.concurrent.TimeUnit;

/**
 * API限流拦截器
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RateLimitInterceptor implements HandlerInterceptor {

    private final RedisTemplate<String, Object> redisTemplate;
    private final IpUtils ipUtils;

    // API限流全局开关
    @Value("${rate-limit.enabled:true}")
    private boolean rateLimitEnabled;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 检查全局开关是否开启
        if (!rateLimitEnabled) {
            log.debug("API限流已全局关闭");
            return true;
        }

        // 只处理方法级别的请求
        if (!(handler instanceof HandlerMethod)) {
            return true;
        }

        HandlerMethod handlerMethod = (HandlerMethod) handler;

        // 检查方法或类上是否有RateLimit注解
        RateLimit rateLimit = handlerMethod.getMethodAnnotation(RateLimit.class);
        if (rateLimit == null) {
            rateLimit = handlerMethod.getBeanType().getAnnotation(RateLimit.class);
        }

        if (rateLimit == null) {
            return true;
        }

        // 获取客户端IP
        String ip = ipUtils.getIpAddress(request);

        // 构建限流key: rate_limit:ip:uri
        String key = rateLimit.key() + ip + ":" + request.getRequestURI();

        // 获取当前访问次数
        Integer currentCount = (Integer) redisTemplate.opsForValue().get(key);

        if (currentCount == null) {
            // 第一次访问,设置初始值为1,并设置过期时间
            redisTemplate.opsForValue().set(key, 1, rateLimit.timeWindow(), TimeUnit.SECONDS);
            log.debug("API限流 - 首次访问: IP={}, URI={}, 窗口={}秒", ip, request.getRequestURI(), rateLimit.timeWindow());
            return true;
        }

        if (currentCount >= rateLimit.maxRequests()) {
            // 超过限流次数,返回错误
            log.warn("API限流 - 访问超限: IP={}, URI={}, 当前次数={}/{}", 
                    ip, request.getRequestURI(), currentCount, rateLimit.maxRequests());

            response.setContentType("application/json;charset=UTF-8");
            response.setStatus(429); // HTTP 429 Too Many Requests
            response.getWriter().write(JSON.toJSONString(R.error(429, rateLimit.message())));
            return false;
        }

        // 访问次数+1
        redisTemplate.opsForValue().increment(key);
        log.debug("API限流 - 正常访问: IP={}, URI={}, 当前次数={}/{}", 
                ip, request.getRequestURI(), currentCount + 1, rateLimit.maxRequests());

        return true;
    }
}

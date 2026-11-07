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

        // 限流主体优先用登录身份，退回 IP。
        // 只按 IP 计数时，同一出口（校园网、公司 NAT、移动网络）下的所有用户
        // 会共用一个计数器，一个人刷接口就把整栋楼限死；本应用在 JWT 拦截器之后执行，
        // 已登录请求可以从 attribute 拿到 userId。
        Object userIdAttr = request.getAttribute("userId");
        String identity = userIdAttr != null ? "user:" + userIdAttr : "ip:" + ip;

        // 构建限流key: 前缀 + 身份 + uri
        String key = rateLimit.key() + identity + ":" + request.getRequestURI();

        // 计数与判定必须原子。原实现是 get -> 判空 -> set(1, ttl)，
        // 并发请求会同时读到 null 并各自把计数写回 1，限流窗口被直接绕过；
        // 且 currentCount 强转 Integer，而 increment 返回 Long，存在类型漂移风险。
        Long currentCount = redisTemplate.opsForValue().increment(key);
        if (currentCount == null) {
            // Redis 异常时不阻断业务
            return true;
        }

        if (currentCount == 1L) {
            // 仅窗口内首次请求设置过期，保证计数与时间窗口同生命周期
            redisTemplate.expire(key, rateLimit.timeWindow(), TimeUnit.SECONDS);
            log.debug("API限流 - 开启窗口: IP={}, URI={}, 窗口={}秒",
                    ip, request.getRequestURI(), rateLimit.timeWindow());
        }

        if (currentCount > rateLimit.maxRequests()) {
            log.warn("API限流 - 访问超限: IP={}, URI={}, 当前次数={}/{}",
                    ip, request.getRequestURI(), currentCount, rateLimit.maxRequests());

            response.setContentType("application/json;charset=UTF-8");
            response.setStatus(429); // HTTP 429 Too Many Requests
            response.getWriter().write(JSON.toJSONString(R.error(429, rateLimit.message())));
            return false;
        }

        log.debug("API限流 - 正常访问: IP={}, URI={}, 当前次数={}/{}",
                ip, request.getRequestURI(), currentCount, rateLimit.maxRequests());

        return true;
    }
}

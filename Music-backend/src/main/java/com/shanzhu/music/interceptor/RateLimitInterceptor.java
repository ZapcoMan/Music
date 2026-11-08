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
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;
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

        // 限流 key = 注解前缀 + 身份，不再拼原始 URI。
        //
        // 注解前缀（comment:add:、login:admin: 等）本身已经按接口划分，拼 URI 对固定路径
        // 不增加任何区分度；但它会让同一接口的 URL 变体各自建键——大小写、尾部斜杠、
        // 百分号编码、矩阵参数都能造出一个新计数器，攻击者由此绕过限流。
        // 已核实现有 10 个 @RateLimit 全部挂在字面路径上、无 {PathVariable}，
        // 因此去掉 URI 不改变任何既有接口的计数行为。
        // URI 仍保留在日志里，用于排查是哪个入口触发了限流。
        String key = rateLimit.key() + identity;

        // 计数与判定必须原子。原实现是 get -> 判空 -> set(1, ttl)，
        // 并发请求会同时读到 null 并各自把计数写回 1，限流窗口被直接绕过；
        // 且 currentCount 强转 Integer，而 increment 返回 Long，存在类型漂移风险。
        long currentCount;
        try {
            Long counted = redisTemplate.opsForValue().increment(key);
            if (counted == null) {
                // 正常不会发生，留作 API 契约变化的兜底，走同一条降级
                return degradeOnRedisUnavailable(rateLimit, request, response, ip, "计数返回空值");
            }
            currentCount = counted;
        } catch (DataAccessException e) {
            // Redis 不可达。不能让它冒成 500：那既不告诉调用方真实原因，
            // 也不表达"这是依赖故障"还是"业务失败"。降级方向交给注解决定。
            log.error("限流计数失败 - Redis 不可用: key={}, URI={}, 原因={}",
                    key, request.getRequestURI(), e.getMessage());
            return degradeOnRedisUnavailable(rateLimit, request, response, ip, "连接异常");
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

    /**
     * Redis 不可用时的降级：放开还是拒绝，由注解的 failOpen 决定。
     *
     * 拒绝时回 503 而不是复用 429：429 的意思是"你请求太快"，
     * 而此刻的真实原因是服务端自己的依赖故障。混在一起，调用方会以为
     * 退避重试就能过，实际上等多久都一样。
     */
    private boolean degradeOnRedisUnavailable(RateLimit rateLimit, HttpServletRequest request,
                                              HttpServletResponse response, String ip, String reason)
            throws IOException {
        if (rateLimit.failOpen()) {
            log.warn("限流降级放行(failOpen) - IP={}, URI={}, 原因={}",
                    ip, request.getRequestURI(), reason);
            return true;
        }
        response.setContentType("application/json;charset=UTF-8");
        response.setStatus(503);
        response.getWriter().write(JSON.toJSONString(
                R.error(503, "服务暂时不可用，请稍后再试")));
        return false;
    }
}

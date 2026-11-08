package com.shanzhu.music.interceptor;

import com.shanzhu.music.annotation.RateLimit;
import com.shanzhu.music.utils.IpUtils;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.method.HandlerMethod;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 限流拦截器在 Redis 故障时的降级方向。
 *
 * 写成纯单元测试而不是 @SpringBootTest：集成测试环境里 Redis 是真实可用的，
 * 降级分支一次都不会被执行，用例只会绿着证明"代码能编译"。
 * 要覆盖它就得把 RedisTemplate 换成会抛异常的实现，
 * 而 @MockBean 会另起一套 Spring 上下文，代价远大于收益。
 */
class RateLimitInterceptorTest {

    @SuppressWarnings("unchecked")
    private final RedisTemplate<String, Object> redisTemplate = mock(RedisTemplate.class);

    @SuppressWarnings("unchecked")
    private final ValueOperations<String, Object> valueOperations = mock(ValueOperations.class);

    private final IpUtils ipUtils = mock(IpUtils.class);

    private RateLimitInterceptor interceptor;

    @BeforeEach
    void setUp() {
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        lenient().when(ipUtils.getIpAddress(any(HttpServletRequest.class))).thenReturn("10.1.2.3");
        interceptor = new RateLimitInterceptor(redisTemplate, ipUtils);
        // rateLimitEnabled 平时由 @Value 注入，单元测试里没有容器，必须显式打开，
        // 否则默认 false 会让 preHandle 直接放行，三条用例测的都是"限流根本没启用"
        ReflectionTestUtils.setField(interceptor, "rateLimitEnabled", true);
    }

    /**
     * 被限流注解标注的假 handler：只用它的方法签名，不执行任何逻辑
     */
    static class Guarded {

        @RateLimit(key = "login:admin:", timeWindow = 60, maxRequests = 5, message = "登录尝试次数过多，请60秒后再试")
        public void failClose() {
        }

        @RateLimit(key = "song:list:", timeWindow = 60, maxRequests = 100, failOpen = true)
        public void failOpen() {
        }

        public void plain() {
        }
    }

    private HandlerMethod handlerOf(String methodName) throws Exception {
        Method method = Guarded.class.getMethod(methodName);
        return new HandlerMethod(new Guarded(), method);
    }

    private void redisIsDown() {
        when(valueOperations.increment(anyString()))
                .thenThrow(new RedisConnectionFailureException("模拟 Redis 不可达"));
    }

    @Test
    @DisplayName("Redis 故障时敏感接口默认拒绝，回 503 而不是 500")
    void redisFailureRejectsSensitiveEndpoint() throws Exception {
        redisIsDown();
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/admin/sessions");
        MockHttpServletResponse response = new MockHttpServletResponse();

        boolean passed = interceptor.preHandle(request, response, handlerOf("failClose"));

        assertFalse(passed, "限流依赖故障时，登录这类入口不应被放行");
        // 这里刻意不是 500：异常若冒到全局处理，调用方看到的是一个无信息的
        // 服务端错误，看不出"退避重试也没用，是服务端自己的依赖挂了"
        assertEquals(503, response.getStatus(), "应回 503 表达依赖故障");
    }

    @Test
    @DisplayName("标注 failOpen 的接口在 Redis 故障时放行")
    void redisFailureStillServesFailOpenEndpoint() throws Exception {
        redisIsDown();
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/songs");
        MockHttpServletResponse response = new MockHttpServletResponse();

        assertTrue(interceptor.preHandle(request, response, handlerOf("failOpen")),
                "纯读取接口显式要求故障期间维持可用");
        assertEquals(200, response.getStatus());
    }

    @Test
    @DisplayName("Redis 正常时超限仍回 429，降级改动没有碰坏主路径")
    void overLimitStillReturns429() throws Exception {
        when(valueOperations.increment(anyString())).thenReturn(6L);
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/admin/sessions");
        MockHttpServletResponse response = new MockHttpServletResponse();

        boolean passed = interceptor.preHandle(request, response, handlerOf("failClose"));

        assertFalse(passed);
        assertEquals(429, response.getStatus(), "正常的超限判定仍是 429，不该被换成 503");
    }

    @Test
    @DisplayName("没有 @RateLimit 的 handler 不受 Redis 故障影响")
    void unannotatedHandlerBypassesRateLimit() throws Exception {
        redisIsDown();
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/singers");
        MockHttpServletResponse response = new MockHttpServletResponse();

        assertTrue(interceptor.preHandle(request, response, handlerOf("plain")),
                "未标注解的接口不经过计数，Redis 挂了也不该被牵连");
    }
}

package com.shanzhu.music.interceptor;

import com.shanzhu.music.utils.IpUtils;
import com.shanzhu.music.utils.JwtUtils;
import com.shanzhu.music.utils.RedisUtils;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 令牌黑名单不可用时的鉴权降级方向。
 *
 * 同样是纯单元测试：集成环境里 Redis 是活的，catch 分支不会被执行，
 * 用 @SpringBootTest 测它只会得到一个绿色的空壳。
 *
 * handler 故意传普通 Object 而不是 HandlerMethod——
 * 那样会同时触发"未声明角色域即拒绝"的 fail-closed 分支，
 * 与本类要验证的黑名单降级混在一起，看不清是哪条规则决定了结果。
 */
class JwtBlacklistDegradationTest {

    private final JwtUtils jwtUtils = mock(JwtUtils.class);
    private final RedisUtils redisUtils = mock(RedisUtils.class);
    private final IpUtils ipUtils = mock(IpUtils.class);

    private JwtAuthenticationInterceptor interceptor;

    @BeforeEach
    void setUp() {
        interceptor = new JwtAuthenticationInterceptor(jwtUtils, redisUtils, ipUtils);

        lenient().when(jwtUtils.validateToken(anyString())).thenReturn(true);
        lenient().when(jwtUtils.getUserIdFromToken(anyString())).thenReturn(5);
        lenient().when(jwtUtils.getUserTypeFromToken(anyString())).thenReturn("consumer");
        lenient().when(jwtUtils.getUsernameFromToken(anyString())).thenReturn("someone");
        lenient().when(jwtUtils.getIpFromToken(anyString())).thenReturn("10.1.2.3");
        lenient().when(ipUtils.getIpAddress(any(HttpServletRequest.class))).thenReturn("10.1.2.3");
    }

    private MockHttpServletRequest authedRequest() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/songs");
        request.addHeader("Authorization", "a.valid.token");
        return request;
    }

    @Test
    @DisplayName("Redis 抛连接异常时按未吊销放行，而不是让整站 500")
    void passesThroughWhenRedisIsDown() throws Exception {
        when(redisUtils.hasKey(anyString()))
                .thenThrow(new RedisConnectionFailureException("模拟 Redis 不可达"));
        MockHttpServletRequest request = authedRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        assertTrue(interceptor.preHandle(request, response, new Object()),
                "黑名单不可用不应把已签名、未过期的令牌判成无效");
        assertEquals(5, request.getAttribute("userId"), "放行时仍要写入身份供 Controller 使用");
    }

    @Test
    @DisplayName("hasKey 返回 null 时不得因拆箱抛 NullPointerException")
    void treatsNullHasKeyAsNotRevoked() {
        // redisTemplate.hasKey 的返回类型是 Boolean，直接写进 if 会在 null 时拆箱崩
        when(redisUtils.hasKey(anyString())).thenReturn(null);
        MockHttpServletRequest request = authedRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        assertDoesNotThrow(
                () -> assertTrue(interceptor.preHandle(request, response, new Object())),
                "hasKey 给出 null 应按未吊销处理，而不是抛异常");
    }

    @Test
    @DisplayName("令牌确实在黑名单里时仍然拒绝")
    void stillRejectsRevokedToken() throws Exception {
        when(redisUtils.hasKey(anyString())).thenReturn(Boolean.TRUE);
        MockHttpServletResponse response = new MockHttpServletResponse();

        assertFalse(interceptor.preHandle(authedRequest(), response, new Object()));
        assertEquals(401, response.getStatus(), "降级不能把真正的已吊销令牌也放过");
    }

    @Test
    @DisplayName("黑名单不可用不影响未携带令牌时的 401")
    void missingTokenStillFails() throws Exception {
        when(redisUtils.hasKey(anyString())).thenThrow(new RedisConnectionFailureException("down"));
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/songs");
        MockHttpServletResponse response = new MockHttpServletResponse();

        assertFalse(interceptor.preHandle(request, response, new Object()));
        assertEquals(401, response.getStatus());
    }
}

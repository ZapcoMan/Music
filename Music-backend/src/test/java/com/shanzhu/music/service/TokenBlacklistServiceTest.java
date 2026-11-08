package com.shanzhu.music.service;

import com.shanzhu.music.utils.JwtUtils;
import com.shanzhu.music.utils.RedisUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.RedisConnectionFailureException;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 令牌吊销在 Redis 故障时的如实回报。
 *
 * 关心的是"谁被允许认为登出成功了"：revoke 抛异常的话，登出接口变 500，
 * 用户本地已经退出了却看到一个错误页；revoke 吞掉异常返回 void 的话，
 * 接口回一句"已退出登录"，而泄漏出去的令牌其实照样可用。
 * 两种都比"退出成功但告知吊销未完成"更糟。
 */
class TokenBlacklistServiceTest {

    private final JwtUtils jwtUtils = mock(JwtUtils.class);
    private final RedisUtils redisUtils = mock(RedisUtils.class);

    private TokenBlacklistService service;

    @BeforeEach
    void setUp() {
        service = new TokenBlacklistService(jwtUtils, redisUtils);
    }

    @Test
    @DisplayName("写入成功时返回 true 并按剩余有效期设 TTL")
    void returnsTrueWhenBlacklisted() {
        when(jwtUtils.getRemainingSeconds("a.valid.token")).thenReturn(3600L);

        assertTrue(service.revoke("a.valid.token"));
        verify(redisUtils).setCacheObject(anyString(), eq(Boolean.TRUE),
                eq(3600L), eq(TimeUnit.SECONDS));
    }

    @Test
    @DisplayName("Redis 故障时返回 false 而不是抛出异常")
    void returnsFalseWhenRedisIsDown() {
        when(jwtUtils.getRemainingSeconds("a.valid.token")).thenReturn(3600L);
        // setCacheObject 返回 void，只能用 doThrow 形式打桩
        doThrow(new RedisConnectionFailureException("模拟 Redis 不可达"))
                .when(redisUtils).setCacheObject(anyString(), any(), anyLong(), any(TimeUnit.class));

        // 异常若逃出去，登出接口就是 500：本地已经退出了，页面却报系统异常
        assertFalse(service.revoke("a.valid.token"),
                "吊销没成功必须让调用方知道，否则等于把泄漏风险藏在一句\"已退出登录\"后面");
    }

    @Test
    @DisplayName("令牌已过期时不必写入黑名单，视为完成")
    void skipsExpiredToken() {
        when(jwtUtils.getRemainingSeconds("expired")).thenReturn(-1L);

        assertTrue(service.revoke("expired"));
        verify(redisUtils, never()).setCacheObject(anyString(), any(), anyLong(), any(TimeUnit.class));
    }

    @Test
    @DisplayName("没有令牌时视为无事可做")
    void handlesMissingToken() {
        assertTrue(service.revoke(null));
        assertTrue(service.revoke("   "));
    }
}

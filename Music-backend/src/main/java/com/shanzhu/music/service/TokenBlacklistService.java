package com.shanzhu.music.service;
import com.shanzhu.music.common.CacheKeys;

import com.shanzhu.music.utils.JwtUtils;
import com.shanzhu.music.utils.RedisUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * 令牌吊销（登出）服务。
 *
 * 拦截器一直在读取 jwt:blacklist:{token}，但全项目此前没有任何写入点，
 * 因此文档宣称的"主动登出"实际并未实现：前端删掉本地 token 后，
 * 泄漏出去的令牌在有效期内仍然可用。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TokenBlacklistService {

    /**
     * 黑名单键前缀，须与 JwtAuthenticationInterceptor 中读取的键保持一致
     */
    private static final String BLACKLIST_PREFIX = CacheKeys.TOKEN_BLACKLIST;

    private final JwtUtils jwtUtils;
    private final RedisUtils redisUtils;

    /**
     * 使令牌立即失效。黑名单条目的 TTL 与令牌剩余有效期对齐，
     * 过期令牌无需记录，避免黑名单无限增长。
     *
     * @return 服务端是否确实完成吊销。Redis 不可用时返回 false 而不是把异常抛出去：
     *         登出接口该清的本地状态仍然要清，但"没能吊销"这件事必须如实告诉调用方，
     *         否则页面显示"已退出登录"、泄漏出去的令牌却照旧可用，风险被藏掉了。
     */
    public boolean revoke(String token) {
        if (token == null || token.isBlank()) {
            return true;
        }
        long remainingSeconds = jwtUtils.getRemainingSeconds(token);
        if (remainingSeconds <= 0L) {
            log.debug("令牌已过期或无效，跳过写入黑名单");
            return true;
        }
        try {
            redisUtils.setCacheObject(BLACKLIST_PREFIX + token, Boolean.TRUE,
                    remainingSeconds, TimeUnit.SECONDS);
        } catch (DataAccessException e) {
            log.error("令牌吊销写入失败 - Redis 不可用，该令牌在自然过期前仍可正常使用，原因={}",
                    e.getMessage());
            return false;
        }
        log.info("令牌已吊销，黑名单有效期 {} 秒", remainingSeconds);
        return true;
    }
}

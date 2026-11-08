package com.shanzhu.music.utils;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Map;

/**
 * JWT工具类
 */
@Slf4j
@Component
public class JwtUtils {

    /**
     * JWT密钥。
     * 刻意不提供默认值：漏配时应启动失败，而不是悄悄改用源码里公开的密钥 ——
     * 后者会让任何读到源码的人都能离线自签合法令牌，且线上无人察觉。
     */
    @Value("${jwt.secret}")
    private String secret;

    /**
     * JWT过期时间(毫秒),默认24小时
     */
    @Value("${jwt.expiration:86400000}")
    private Long expiration;

    /**
     * 令牌签发方与受众。
     *
     * 写成常量而不是配置项：这两个值是"本服务的身份"，与部署环境无关，
     * 做成可覆盖的开关只会让不同环境的令牌互不通用而没人记得原因。
     * 真要与其他服务共用密钥时，应当各自换一个密钥，而不是靠 iss/aud 区分。
     */
    private static final String ISSUER = "music-backend";
    private static final String AUDIENCE = "music-clients";

    /**
     * 生成密钥
     */
    private SecretKey getSecretKey() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 令牌有效期（秒），供登录接口出参使用。
     * 此前 AdminController 与 ConsumerController 各自硬编码 86400，与 jwt.expiration
     * 是两个来源——改了配置后前端拿到的过期时间不会跟着变。
     */
    public long getExpirationSeconds() {
        return expiration / 1000;
    }

    /**
     * 生成JWT Token
     *
     * @param userId   用户ID
     * @param username 用户名
     * @param userType 用户类型(admin/consumer)
     * @param ip       IP地址
     * @return JWT Token
     */
    public String generateToken(Integer userId, String username, String userType, String ip) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expiration);

        return Jwts.builder()
                .subject(username)
                .issuer(ISSUER)
                .audience().add(AUDIENCE).and()
                .claim("userId", userId)
                .claim("userType", userType)
                .claim("ip", ip)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(getSecretKey())
                .compact();
    }

    /**
     * 从Token中获取Claims
     *
     * @param token JWT Token
     * @return Claims
     */
    public Claims getClaimsFromToken(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(getSecretKey())
                    // 只验签名不够：密钥一旦被另一个服务复用（同一公司内共享 JWT 密钥
                    // 是很常见的做法），那边签发的任意令牌在本系统里同样"合法"。
                    // 绑定签发方与受众，把令牌的作用域钉回本服务。
                    .requireIssuer(ISSUER)
                    .requireAudience(AUDIENCE)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (Exception e) {
            log.error("解析JWT Token失败: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 从Token中获取用户ID
     *
     * @param token JWT Token
     * @return 用户ID
     */
    public Integer getUserIdFromToken(String token) {
        Claims claims = getClaimsFromToken(token);
        if (claims == null) {
            return null;
        }
        return claims.get("userId", Integer.class);
    }

    /**
     * 从Token中获取用户名
     *
     * @param token JWT Token
     * @return 用户名
     */
    public String getUsernameFromToken(String token) {
        Claims claims = getClaimsFromToken(token);
        if (claims == null) {
            return null;
        }
        return claims.getSubject();
    }

    /**
     * 从Token中获取用户类型
     *
     * @param token JWT Token
     * @return 用户类型
     */
    public String getUserTypeFromToken(String token) {
        Claims claims = getClaimsFromToken(token);
        if (claims == null) {
            return null;
        }
        return claims.get("userType", String.class);
    }

    /**
     * 从Token中获取IP地址
     *
     * @param token JWT Token
     * @return IP地址
     */
    public String getIpFromToken(String token) {
        Claims claims = getClaimsFromToken(token);
        if (claims == null) {
            return null;
        }
        return claims.get("ip", String.class);
    }

    /**
     * 验证Token是否有效
     *
     * @param token JWT Token
     * @return 是否有效
     */
    public boolean validateToken(String token) {
        try {
            Claims claims = getClaimsFromToken(token);
            if (claims == null) {
                return false;
            }
            // 检查是否过期
            Date expiration = claims.getExpiration();
            return expiration.after(new Date());
        } catch (Exception e) {
            log.error("验证JWT Token失败: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 计算令牌剩余有效期（秒）。令牌无效或已过期时返回 0。
     * 登出时用它把黑名单条目的存活时间与令牌自身对齐，避免黑名单永驻内存。
     */
    public long getRemainingSeconds(String token) {
        Claims claims = getClaimsFromToken(token);
        if (claims == null || claims.getExpiration() == null) {
            return 0L;
        }
        long remaining = (claims.getExpiration().getTime() - System.currentTimeMillis()) / 1000L;
        return Math.max(0L, remaining);
    }

    /**
     * 刷新Token
     *
     * @param token 原Token
     * @return 新Token
     */
    public String refreshToken(String token) {
        Claims claims = getClaimsFromToken(token);
        if (claims == null) {
            return null;
        }

        Integer userId = claims.get("userId", Integer.class);
        String username = claims.getSubject();
        String userType = claims.get("userType", String.class);
        String ip = claims.get("ip", String.class);

        return generateToken(userId, username, userType, ip);
    }
}

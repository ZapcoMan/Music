package com.shanzhu.music.utils;

import com.shanzhu.music.BaseTest;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * JwtUtils 的签发与校验口径。
 */
class JwtUtilsTest extends BaseTest {

    @Autowired
    private JwtUtils jwtUtils;

    /**
     * 与 test profile 配置同源，用于手工伪造"同密钥、不同签发方"的令牌
     */
    @Value("${jwt.secret}")
    private String secret;

    private SecretKey key() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    @DisplayName("本服务签发的令牌可解析出身份字段")
    void ownTokenParses() {
        String token = jwtUtils.generateToken(7, "someone", "consumer", "10.0.0.7");

        Claims claims = jwtUtils.getClaimsFromToken(token);

        assertNotNull(claims, "本服务签发的令牌应当可解析");
        assertEquals(7, claims.get("userId", Integer.class));
        assertEquals("consumer", claims.get("userType", String.class));
    }

    /**
     * issuer/audience 校验只有这一条实际验证。
     *
     * 其它用例的令牌都由 jwtUtils.generateToken 造出来，即便把 requireIssuer
     * 整行删掉也照样全绿——也就是说这个防护本身没人盯着。这里用同一把密钥
     * 手工签一个签发方不同的令牌：签名是有效的，所以"只验签名"的实现会放行它，
     * 只有真正校验 iss 才会拒。
     */
    @Test
    @DisplayName("同密钥但签发方不同的令牌必须被拒")
    void foreignIssuerIsRejected() {
        String foreign = Jwts.builder()
                .subject("someone")
                .issuer("other-service")
                .audience().add("music-clients").and()
                .claim("userId", 1)
                .claim("userType", "admin")
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 60_000L))
                .signWith(key())
                .compact();

        assertNull(jwtUtils.getClaimsFromToken(foreign),
                "同一把密钥签出、但签发方不是本服务的令牌，不应被当作合法登录凭证");
    }

    @Test
    @DisplayName("缺少受众声明的令牌必须被拒")
    void tokenWithoutAudienceIsRejected() {
        String noAudience = Jwts.builder()
                .subject("someone")
                .issuer("music-backend")
                .claim("userId", 1)
                .claim("userType", "admin")
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 60_000L))
                .signWith(key())
                .compact();

        assertNull(jwtUtils.getClaimsFromToken(noAudience),
                "没有 aud 的令牌应解析失败，而不是被静默接受");
    }
}

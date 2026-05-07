package com.shanzhu.music.interceptor;

import com.alibaba.fastjson2.JSON;
import com.shanzhu.music.common.R;
import com.shanzhu.music.utils.IpUtils;
import com.shanzhu.music.utils.JwtUtils;
import com.shanzhu.music.utils.RedisUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * JWT认证拦截器
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationInterceptor implements HandlerInterceptor {

    private final JwtUtils jwtUtils;
    private final RedisUtils redisUtils;
    private final IpUtils ipUtils;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 获取Token(从Header或Parameter)
        String token = request.getHeader("Authorization");
        
        if (!StringUtils.hasText(token)) {
            token = request.getParameter("token");
        }

        // 不需要认证的接口直接放行
        if (!StringUtils.hasText(token)) {
            return true;
        }

        // 验证Token格式是否有效
        if (!jwtUtils.validateToken(token)) {
            log.warn("JWT认证失败 - Token无效或已过期");
            response.setContentType("application/json;charset=UTF-8");
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write(JSON.toJSONString(R.error(401, "Token无效或已过期")));
            return false;
        }

        // 获取Token中的信息
        Integer userId = jwtUtils.getUserIdFromToken(token);
        String tokenIp = jwtUtils.getIpFromToken(token);
        String userType = jwtUtils.getUserTypeFromToken(token);

        // IP绑定验证:检查当前请求IP是否与Token中绑定的IP一致
        String currentIp = ipUtils.getIpAddress(request);
        if (tokenIp != null && !tokenIp.equals(currentIp)) {
            log.warn("JWT认证失败 - IP不匹配, TokenIP={}, 当前IP={}", tokenIp, currentIp);
            response.setContentType("application/json;charset=UTF-8");
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.getWriter().write(JSON.toJSONString(R.error(403, "IP地址不匹配,请重新登录")));
            return false;
        }

        // 检查Token是否在Redis中被禁用(用于登出场景)
        String redisKey = "jwt:blacklist:" + token;
        if (redisUtils.hasKey(redisKey)) {
            log.warn("JWT认证失败 - Token已被禁用");
            response.setContentType("application/json;charset=UTF-8");
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write(JSON.toJSONString(R.error(401, "Token已被禁用,请重新登录")));
            return false;
        }

        // 将用户信息放入request属性,供Controller使用
        request.setAttribute("userId", userId);
        request.setAttribute("userType", userType);
        request.setAttribute("token", token);

        log.debug("JWT认证成功 - userId={}, userType={}, ip={}", userId, userType, currentIp);
        return true;
    }
}

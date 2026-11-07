package com.shanzhu.music.interceptor;
import com.shanzhu.music.common.CacheKeys;

import com.alibaba.fastjson2.JSON;
import com.shanzhu.music.annotation.RequireUserType;
import com.shanzhu.music.common.R;
import com.shanzhu.music.utils.IpUtils;
import com.shanzhu.music.utils.JwtUtils;
import com.shanzhu.music.utils.RedisUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.method.HandlerMethod;
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

        // 未携带 Token 一律拒绝。免登录接口由 WebMvcConfig 的白名单排除，
        // 而不是靠"没有 Token 就放行"——后者会让全部业务接口处于未认证状态
        if (!StringUtils.hasText(token)) {
            log.warn("鉴权失败 - 未携带 Token, uri={}", request.getRequestURI());
            response.setContentType("application/json;charset=UTF-8");
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write(JSON.toJSONString(R.error(401, "未登录或登录已失效")));
            return false;
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
        String redisKey = CacheKeys.TOKEN_BLACKLIST + token;
        if (redisUtils.hasKey(redisKey)) {
            log.warn("JWT认证失败 - Token已被禁用");
            response.setContentType("application/json;charset=UTF-8");
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write(JSON.toJSONString(R.error(401, "Token已被禁用,请重新登录")));
            return false;
        }

        // 角色域校验：标注了 @RequireUserType 的接口必须匹配令牌中的 userType。
        // admin 与 consumer 共用同一签名密钥，缺这层校验时消费者令牌可直接调管理接口
        RequireUserType required = resolveRequiredUserType(handler);
        if (required != null && !required.value().equals(userType)) {
            log.warn("鉴权失败 - 角色不匹配, 需要={}, 实际={}, uri={}",
                    required.value(), userType, request.getRequestURI());
            response.setContentType("application/json;charset=UTF-8");
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.getWriter().write(JSON.toJSONString(R.error(403, "无权访问该接口")));
            return false;
        }

        // 将用户信息放入request属性,供Controller使用
        request.setAttribute("userId", userId);
        request.setAttribute("userType", userType);
        request.setAttribute("token", token);

        log.debug("JWT认证成功 - userId={}, userType={}, ip={}", userId, userType, currentIp);
        return true;
    }

    /**
     * 读取接口或其所在类上的 @RequireUserType。
     * 静态资源等非 HandlerMethod 请求返回 null，即不做角色限制。
     */
    private RequireUserType resolveRequiredUserType(Object handler) {
        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return null;
        }
        RequireUserType onMethod = AnnotatedElementUtils.findMergedAnnotation(
                handlerMethod.getMethod(), RequireUserType.class);
        if (onMethod != null) {
            return onMethod;
        }
        return AnnotatedElementUtils.findMergedAnnotation(
                handlerMethod.getBeanType(), RequireUserType.class);
    }
}

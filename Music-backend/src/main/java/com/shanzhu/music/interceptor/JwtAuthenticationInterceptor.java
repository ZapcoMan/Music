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

import java.util.Arrays;
import java.util.Set;

/**
 * JWT认证拦截器
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationInterceptor implements HandlerInterceptor {

    /**
     * 会改变服务端状态的动词。OPTIONS 是 CORS 预检，不算写操作。
     */
    private static final Set<String> WRITE_METHODS = Set.of("POST", "PUT", "PATCH", "DELETE");

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
            return reject(response, HttpServletResponse.SC_UNAUTHORIZED, "未登录或登录已失效");
        }

        // 验证Token格式是否有效
        if (!jwtUtils.validateToken(token)) {
            log.warn("JWT认证失败 - Token无效或已过期");
            return reject(response, HttpServletResponse.SC_UNAUTHORIZED, "Token无效或已过期");
        }

        // 获取Token中的信息
        Integer userId = jwtUtils.getUserIdFromToken(token);
        String tokenIp = jwtUtils.getIpFromToken(token);
        String userType = jwtUtils.getUserTypeFromToken(token);

        // IP绑定验证:检查当前请求IP是否与Token中绑定的IP一致
        String currentIp = ipUtils.getIpAddress(request);
        if (tokenIp != null && !tokenIp.equals(currentIp)) {
            log.warn("JWT认证失败 - IP不匹配, TokenIP={}, 当前IP={}", tokenIp, currentIp);
            return reject(response, HttpServletResponse.SC_FORBIDDEN, "IP地址不匹配,请重新登录");
        }

        // 检查Token是否在Redis中被禁用(用于登出场景)
        String redisKey = CacheKeys.TOKEN_BLACKLIST + token;
        if (redisUtils.hasKey(redisKey)) {
            log.warn("JWT认证失败 - Token已被禁用");
            return reject(response, HttpServletResponse.SC_UNAUTHORIZED, "Token已被禁用,请重新登录");
        }

        // 角色域校验：标注了 @RequireUserType 的接口必须命中令牌里的 userType 之一。
        // admin 与 consumer 共用同一签名密钥，缺这层校验时消费者令牌可直接调管理接口
        RequireUserType required = resolveRequiredUserType(handler);
        if (required != null && !Arrays.asList(required.value()).contains(userType)) {
            log.warn("鉴权失败 - 角色不匹配, 需要={}, 实际={}, uri={}",
                    Arrays.toString(required.value()), userType, request.getRequestURI());
            return reject(response, HttpServletResponse.SC_FORBIDDEN, "无权访问该接口");
        }

        // fail-closed：写接口必须显式声明角色域，没写注解就拒绝。
        //
        // 此前的语义是"未标注只要求登录、不限角色"，于是漏写注解的后果不是报错而是静默放行。
        // 本轮补 22 处 admin 标注时发现其中 21 处长期没有任何用例验证过——正是这个机制的产物：
        // 漏标注不会让测试变红，只会在生产被人拿来变红。改成拒绝之后，新增写接口忘标角色
        // 第一次调用就会暴露。
        //
        // 只作用于真实 Controller 方法：静态资源、404 兜底等 handler 不是 HandlerMethod，
        // 若一并拒绝，favicon 之类的请求会平白多出 403。
        if (required == null
                && handler instanceof HandlerMethod
                && WRITE_METHODS.contains(request.getMethod())) {
            log.error("鉴权拒绝 - 写接口未声明角色域, method={}, uri={} —— 请为该接口补 @RequireUserType",
                    request.getMethod(), request.getRequestURI());
            return reject(response, HttpServletResponse.SC_FORBIDDEN, "该接口未声明可访问角色，已按默认拒绝处理");
        }

        // 将用户信息放入request属性,供Controller使用
        request.setAttribute("userId", userId);
        request.setAttribute("userType", userType);
        // 审计日志需要记录操作者账号；此前只有 userId，接线方只能再去查库
        request.setAttribute("username", jwtUtils.getUsernameFromToken(token));
        request.setAttribute("token", token);

        log.debug("JWT认证成功 - userId={}, userType={}, ip={}", userId, userType, currentIp);
        return true;
    }

    /**
     * 输出一条拒绝响应并返回 false。
     * 此前五个拒绝分支各写一遍 contentType/status/write，第六个分支（fail-closed）
     * 再加一次就该出错了，故收敛。
     */
    private boolean reject(HttpServletResponse response, int status, String message) throws Exception {
        response.setContentType("application/json;charset=UTF-8");
        response.setStatus(status);
        response.getWriter().write(JSON.toJSONString(R.error(status, message)));
        return false;
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

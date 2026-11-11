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
import org.springframework.dao.DataAccessException;
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

    private final JwtUtils jwtUtils;
    private final RedisUtils redisUtils;
    private final IpUtils ipUtils;

    /**
     * 免登录入口，按"HTTP 方法 + 路径"整体匹配。
     * <p>
     * 之所以不再用 WebMvcConfig#excludePathPatterns 表达：那条规则只认路径不认方法。
     * 登录是 POST /consumers/sessions，登出是 DELETE /consumers/sessions —— 同一个字面，
     * 一个必须匿名可调、另一个必须带令牌。按路径排除会把登出一起放成匿名入口，
     * 而登出方法用 @RequestAttribute 读令牌身份，匿名请求会因为属性缺失直接抛 500，
     * 客户端看到"系统异常"，真实原因只是没登录。
     */
    private static final Set<String> ANONYMOUS_ENTRIES = Set.of(
            "POST /api/v1/admin/sessions",        // 管理员登录
            "POST /api/v1/consumers/sessions",    // 用户登录
            "POST /api/v1/consumers/registrations" // 用户注册
    );

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if (ANONYMOUS_ENTRIES.contains(request.getMethod() + " " + request.getRequestURI())) {
            return true;
        }

        // 令牌只认 Authorization 头。
        //
        // 此前在没有头时回落到 request.getParameter("token")，看上去只是"多一种方便"，
        // 实际后果是：RequestResponseLoggingInterceptor 会把 query string 原样打进 INFO 日志，
        // 而 logback 的 info appender 保留 30 天——于是任何一个带令牌的 URL 都会把完整 JWT
        // 落盘。拿到日志文件等于拿到 24 小时有效的会话，且不需要任何破解。
        // 已核实前端与两套管理端都不使用该参数形式，故直接关掉这条入口。
        String token = request.getHeader("Authorization");

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
        boolean revoked;
        try {
            // hasKey 返回的是 Boolean，直接放进 if 会在 null 时拆箱抛 NPE；
            // spring-data-redis 在部分管道异常下确实会给出 null 而不是抛错
            revoked = Boolean.TRUE.equals(redisUtils.hasKey(redisKey));
        } catch (DataAccessException e) {
            // 这里刻意 fail-open，方向与限流那边相反，原因是代价不对称：
            // 关成拒绝的话，Redis 一挂，所有已登录请求当场全部不可用——
            // 本应用每个业务接口都要经过这条黑名单；
            // 放开的话，风险只是"已经登出的令牌多活一会儿"，
            // 而它仍然受签名校验与自身过期时间约束，窗口有界。
            // 前提是这个降级必须被看见：用 error 级日志，便于接告警，不能悄悄发生。
            log.error("令牌黑名单不可用，本次按未吊销放行 - uri={}, 原因={}",
                    request.getRequestURI(), e.getMessage());
            revoked = false;
        }
        if (revoked) {
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

        // fail-closed：**任何**业务端点都必须显式声明角色域，没写注解就拒绝。
        //
        // 此前只约束写接口，读接口保持"已登录即可、不限角色"。但那仍是靠开发者记得标注：
        // 漏一个多参数查询接口，就是又一条 /comment/allComment 那样的跨用户数据读取路径。
        // 白名单反转之后，"公开"这件事必须由 WebMvcConfig 的路径排除显式表达（登录、注册、
        // 静态资源），而不是靠"没人标注所以放行"。
        //
        // 只作用于真实 Controller 方法：静态资源与 404 兜底的 handler 不是 HandlerMethod，
        // 一并拒绝会让 favicon 之类请求平白多出 403。
        if (required == null && handler instanceof HandlerMethod) {
            log.error("鉴权拒绝 - 接口未声明角色域, method={}, uri={} —— 请为该接口补 @RequireUserType",
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

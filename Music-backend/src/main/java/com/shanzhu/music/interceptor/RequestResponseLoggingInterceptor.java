package com.shanzhu.music.interceptor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shanzhu.music.utils.IpUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.WebUtils;

import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

/**
 * 请求/响应日志拦截器
 * 记录所有API请求的详细信息,包括:
 * - 请求方法、URL、IP地址
 * - 请求头、请求参数、请求体
 * - 响应状态码、响应时间
 * - 异常信息
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RequestResponseLoggingInterceptor implements HandlerInterceptor {

    /**
     * 取客户端 IP 只有一个口径。此前本类自带一份 getClientIp，
     * 无条件采信 X-Forwarded-For 等可伪造头，而鉴权侧用的是受
     * security.ip.trust-proxy-headers 开关控制的 IpUtils：
     * 同一个请求里，令牌绑定用的是真地址、审计日志记的是伪造值，
     * 事后拿日志溯源会得到一个不存在的主机，等于自断线索。
     */
    private final IpUtils ipUtils;

    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final String START_TIME_ATTRIBUTE = "startTime";
    private static final int MAX_PAYLOAD_LENGTH = 10000; // 最大日志长度

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 记录开始时间
        long startTime = System.currentTimeMillis();
        request.setAttribute(START_TIME_ATTRIBUTE, startTime);

        // 获取客户端IP（与鉴权侧同一口径，见 ipUtils 字段说明）
        String clientIp = ipUtils.getIpAddress(request);

        // 构建请求日志
        StringBuilder requestLog = new StringBuilder();
        requestLog.append("\n========== 请求开始 ==========\n");
        requestLog.append("【请求ID】      : {}\n");
        requestLog.append("【请求时间】    : {}\n");
        requestLog.append("【请求方法】    : {}\n");
        requestLog.append("【请求URL】     : {}\n");
        requestLog.append("【查询参数】    : {}\n");
        requestLog.append("【客户端IP】    : {}\n");
        requestLog.append("【User-Agent】  : {}\n");
        requestLog.append("【Content-Type】: {}\n");
        requestLog.append("【请求头】      : {}\n");

        String requestId = generateRequestId();
        // 此前只把 requestId 放在局部变量里，afterCompletion 再从 attribute 取永远为 null
        request.setAttribute("requestId", requestId);
        // 查询串必须脱敏：请求头有 maskSensitiveHeaders、请求体有 maskSensitiveInfo，
        // 唯独这里原样输出，而令牌曾从 ?token= 进入，于是完整 JWT 随 INFO 日志留档 30 天。
        // 令牌读取入口已收紧，这层脱敏仍保留——防的是以后有人图方便再加一次回落，
        // 把同一个问题静默带回来。
        String queryString = maskQueryString(request.getQueryString());
        String userAgent = request.getHeader("User-Agent");
        String contentType = request.getHeader("Content-Type");

        // 获取所有请求头
        Map<String, String> headers = getAllHeaders(request);

        log.info(requestLog.toString(),
                requestId,
                java.time.LocalDateTime.now(),
                request.getMethod(),
                request.getRequestURI(),
                queryString != null ? "?" + queryString : "",
                clientIp,
                maskSensitiveInfo(userAgent),
                contentType,
                maskSensitiveHeaders(headers)
        );

        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        // 计算耗时
        Long startTime = (Long) request.getAttribute(START_TIME_ATTRIBUTE);
        long duration = 0;
        if (startTime != null) {
            duration = System.currentTimeMillis() - startTime;
        }

        // 获取响应状态码
        int status = response.getStatus();

        // 构建响应日志
        StringBuilder responseLog = new StringBuilder();
        responseLog.append("\n========== 请求结束 ==========\n");
        responseLog.append("【请求ID】      : {}\n");
        responseLog.append("【响应状态】    : {}\n");
        responseLog.append("【耗时】        : {} ms\n");
        responseLog.append("【异常信息】    : {}\n");
        responseLog.append("============================\n");

        String requestId = (String) request.getAttribute("requestId");
        String exceptionMsg = ex != null ? ex.getClass().getSimpleName() + ": " + ex.getMessage() : "无";

        log.info(responseLog.toString(),
                requestId != null ? requestId : "N/A",
                status,
                duration,
                exceptionMsg
        );

        // 请求体要在处理结束后才读得到：此时流已被 @RequestBody 消费，
        // ContentCachingRequestWrapper 才把字节缓存起来
        logRequestBody(request);

        // 如果有异常,记录详细堆栈
        if (ex != null) {
            log.error("请求处理异常 - URL: {}, Method: {}", request.getRequestURI(), request.getMethod(), ex);
        }

        // 性能警告
        if (duration > 3000) {
            log.warn("⚠️ 慢请求警告 - URL: {}, 耗时: {} ms", request.getRequestURI(), duration);
        }
    }

    /**
     * 记录请求体。仅 POST/PUT/PATCH 且包装器有缓存时输出，
     * multipart（文件上传）不缓存也不记录。
     */
    private void logRequestBody(HttpServletRequest request) {
        String method = request.getMethod() == null ? "" : request.getMethod().toUpperCase();
        if (!"POST".equals(method) && !"PUT".equals(method) && !"PATCH".equals(method)) {
            return;
        }
        // 不能用 instanceof：Spring Security 等过滤器会在本包装器之外再套一层
        // HttpServletRequestWrapper，此时 request 的运行时类型不是 ContentCachingRequestWrapper。
        // getNativeRequest 沿包装链逐层解包，才取得到真正缓存了字节的那一层。
        ContentCachingRequestWrapper wrapper =
                WebUtils.getNativeRequest(request, ContentCachingRequestWrapper.class);
        if (wrapper == null) {
            return;
        }
        try {
            byte[] body = wrapper.getContentAsByteArray();
            if (body.length == 0) {
                return;
            }
            String charset = request.getCharacterEncoding() != null ? request.getCharacterEncoding() : "UTF-8";
            String bodyStr = new String(body, charset);
            String contentType = request.getContentType();
            if (bodyStr.length() <= MAX_PAYLOAD_LENGTH) {
                log.info("【请求体】      : {}", maskSensitiveBody(bodyStr, contentType));
            } else {
                log.info("【请求体】      : {}... (内容过长,已截断)", bodyStr.substring(0, MAX_PAYLOAD_LENGTH));
            }
        } catch (Exception e) {
            log.warn("读取请求体失败", e);
        }
    }

    /**
     * 生成请求ID
     */
    private String generateRequestId() {
        return String.format("%d-%d", System.currentTimeMillis(), (int) (Math.random() * 10000));
    }

    /**
     * 获取所有请求头
     */
    /**
     * 查询串脱敏。令牌、口令一类凭据都可能出现在 URL 上，而这条日志路径此前是三者中
     * 唯一没有脱敏的（请求头有 maskSensitiveHeaders、请求体有 maskSensitiveBody）。
     * INFO 日志按天滚动且保留 30 天，任何一次带凭据的 URL 都会长期留在磁盘上。
     */
    private String maskQueryString(String queryString) {
        if (queryString == null) {
            return null;
        }
        return queryString.replaceAll("(?i)(token|password|passwd|secret|authorization)=([^&]*)", "$1=***");
    }

    private Map<String, String> getAllHeaders(HttpServletRequest request) {
        Map<String, String> headers = new HashMap<>();
        Enumeration<String> headerNames = request.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String name = headerNames.nextElement();
            headers.put(name, request.getHeader(name));
        }
        return headers;
    }

    /**
     * 脱敏敏感请求头
     */
    private String maskSensitiveHeaders(Map<String, String> headers) {
        Map<String, String> masked = new HashMap<>(headers);
        // 脱敏Authorization头
        if (masked.containsKey("Authorization")) {
            String auth = masked.get("Authorization");
            if (auth != null && auth.length() > 20) {
                masked.put("Authorization", auth.substring(0, 15) + "***");
            }
        }
        // 脱敏Cookie
        if (masked.containsKey("Cookie")) {
            masked.put("Cookie", "***");
        }
        return masked.toString();
    }

    /**
     * 脱敏敏感信息
     */
    private String maskSensitiveInfo(String info) {
        if (info == null) {
            return "";
        }
        // 可以添加更多脱敏规则
        return info;
    }

    /**
     * 脱敏请求体中的敏感信息
     */
    private String maskSensitiveBody(String body, String contentType) {
        if (body == null || body.isEmpty()) {
            return body;
        }

        try {
            if (contentType != null && contentType.contains("application/json")) {
                // 简单的密码字段脱敏
                body = body.replaceAll("\"password\"\\s*:\\s*\"[^\"]*\"", "\"password\":\"***\"");
                body = body.replaceAll("\"token\"\\s*:\\s*\"[^\"]*\"", "\"token\":\"***\"");
                body = body.replaceAll("\"secret\"\\s*:\\s*\"[^\"]*\"", "\"secret\":\"***\"");
            } else {
                // 表单体同样带明文凭令：本项目登录与修改资料都走 x-www-form-urlencoded。
                // 只对 JSON 脱敏的话，请求体一旦能被记录，口令就跟着进了日志文件。
                body = body.replaceAll("(?i)(password|passwd|token|secret)=([^&\"]*)", "$1=***");
            }
        } catch (Exception e) {
            log.debug("请求体脱敏失败", e);
        }

        return body;
    }
}

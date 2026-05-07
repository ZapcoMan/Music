package com.shanzhu.music.interceptor;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.net.InetAddress;
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
public class RequestResponseLoggingInterceptor implements HandlerInterceptor {

    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final String START_TIME_ATTRIBUTE = "startTime";
    private static final int MAX_PAYLOAD_LENGTH = 10000; // 最大日志长度

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 记录开始时间
        long startTime = System.currentTimeMillis();
        request.setAttribute(START_TIME_ATTRIBUTE, startTime);

        // 包装请求和响应以支持多次读取
        ContentCachingRequestWrapper wrappedRequest = new ContentCachingRequestWrapper(request);
        ContentCachingResponseWrapper wrappedResponse = new ContentCachingResponseWrapper(response);

        // 获取客户端IP
        String clientIp = getClientIp(request);

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
        String queryString = request.getQueryString();
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

        // 记录请求体(仅POST/PUT/PATCH)
        String method = request.getMethod().toUpperCase();
        if ("POST".equals(method) || "PUT".equals(method) || "PATCH".equals(method)) {
            try {
                byte[] body = wrappedRequest.getContentAsByteArray();
                if (body.length > 0) {
                    String bodyStr = new String(body, request.getCharacterEncoding());
                    if (bodyStr.length() <= MAX_PAYLOAD_LENGTH) {
                        log.info("【请求体】      : {}", maskSensitiveBody(bodyStr, contentType));
                    } else {
                        log.info("【请求体】      : {}... (内容过长,已截断)", bodyStr.substring(0, MAX_PAYLOAD_LENGTH));
                    }
                }
            } catch (Exception e) {
                log.warn("读取请求体失败", e);
            }
        }

        // 将包装后的对象设置回request/response
        // 注意: 这里不能直接替换,需要在Controller中通过其他方式获取
        // Spring会自动处理ContentCachingRequestWrapper

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
     * 生成请求ID
     */
    private String generateRequestId() {
        return String.format("%d-%d", System.currentTimeMillis(), (int) (Math.random() * 10000));
    }

    /**
     * 获取客户端真实IP
     */
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        // 多个代理时,第一个IP为真实IP
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }

    /**
     * 获取所有请求头
     */
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

        // JSON格式脱敏
        if (contentType != null && contentType.contains("application/json")) {
            try {
                // 简单的密码字段脱敏
                body = body.replaceAll("\"password\"\\s*:\\s*\"[^\"]*\"", "\"password\":\"***\"");
                body = body.replaceAll("\"token\"\\s*:\\s*\"[^\"]*\"", "\"token\":\"***\"");
                body = body.replaceAll("\"secret\"\\s*:\\s*\"[^\"]*\"", "\"secret\":\"***\"");
            } catch (Exception e) {
                log.debug("JSON脱敏失败", e);
            }
        }

        return body;
    }
}

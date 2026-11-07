package com.shanzhu.music.utils;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * IP地址工具类
 */
@Slf4j
@Component
public class IpUtils {

    /**
     * 是否信任代理透传头（X-Forwarded-For / Proxy-Client-IP / X-Real-IP 等）。默认 false。
     *
     * 这类头任何客户端都能自己写。此前无条件采信，后果不是"IP 记不准"这种观感问题，
     * 而是三个安全用途同时失效：
     * (a) JWT 的 IP 绑定校验可被一个请求头绕过——令牌被盗后攻击者只需伪造成原 IP 即可复用；
     * (b) 审计日志与登录日志里的来源 IP 可写成任意值，事后溯源直接失真；
     * (c) 未登录请求的限流身份退化为 IP，伪造即可让计数不累加，也可让键基数膨胀。
     *
     * 只有确认服务确实位于可信 Nginx/SLB 之后、且客户端无法直连后端时，才应设为 true。
     */
    @Value("${security.ip.trust-proxy-headers:false}")
    private boolean trustProxyHeaders;

    /**
     * 获取客户端真实IP地址
     *
     * @param request HttpServletRequest
     * @return IP地址
     */
    public String getIpAddress(HttpServletRequest request) {
        String ip = null;

        if (trustProxyHeaders) {
            // 仅在确认位于可信代理之后才读透传头
            String ipAddresses = request.getHeader("X-Forwarded-For");

            if (ipAddresses == null || ipAddresses.length() == 0 || "unknown".equalsIgnoreCase(ipAddresses)) {
                // Proxy-Client-IP: Apache 服务代理
                ipAddresses = request.getHeader("Proxy-Client-IP");
            }

            if (ipAddresses == null || ipAddresses.length() == 0 || "unknown".equalsIgnoreCase(ipAddresses)) {
                // WL-Proxy-Client-IP: WebLogic 服务代理
                ipAddresses = request.getHeader("WL-Proxy-Client-IP");
            }

            if (ipAddresses == null || ipAddresses.length() == 0 || "unknown".equalsIgnoreCase(ipAddresses)) {
                // HTTP_CLIENT_IP: 有些代理服务器
                ipAddresses = request.getHeader("HTTP_CLIENT_IP");
            }

            if (ipAddresses == null || ipAddresses.length() == 0 || "unknown".equalsIgnoreCase(ipAddresses)) {
                // X-Real-IP: Nginx 服务代理
                ipAddresses = request.getHeader("X-Real-IP");
            }

            // 有些网络通过多层代理,那么获取到的ip就会有多个,一般都是通过逗号(,)分割开来,并且第一个ip为客户端的真实IP
            if (ipAddresses != null && ipAddresses.length() != 0) {
                ip = ipAddresses.split(",")[0];
            }
        }

        // 默认（以及上面没拿到时）用容器给出的对端地址：这个值客户端伪造不了
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }

        // 处理IPv6本地回环地址
        if ("0:0:0:0:0:0:0:1".equals(ip) || "::1".equals(ip)) {
            ip = "127.0.0.1";
        }

        log.debug("获取客户端IP地址: {}", ip);
        return ip;
    }
}

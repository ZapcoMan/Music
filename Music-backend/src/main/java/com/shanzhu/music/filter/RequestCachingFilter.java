package com.shanzhu.music.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;

import java.io.IOException;

/**
 * 把请求包装成 ContentCachingRequestWrapper 并挂到链路上，
 * 使日志拦截器在请求处理结束后仍能读到请求体。
 *
 * 为什么必须是 Filter 而不是拦截器：HandlerInterceptor 拿不到 request 对象本身，
 * 它只能读，不能替换。原实现在 preHandle 里 new 了一个包装器，
 * 既没传给后续链路、又在那个阶段就调用 getContentAsByteArray() ——
 * 此时流还没被读取，缓存必然是空数组，所以请求体日志一直恒为空。
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class RequestCachingFilter extends OncePerRequestFilter {

    /**
     * 缓存上限，与日志的最大记录长度同量级：
     * 无上限的缓存会让一次大 body 请求直接吃掉等量堆内存。
     */
    private static final int CACHE_LIMIT = 10000;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String contentType = request.getContentType();
        boolean multipart = contentType != null && contentType.toLowerCase().startsWith("multipart/");

        // 文件上传不缓存：二进制体记进日志没有意义，还会把整段文件读进内存
        if (multipart || request.getContentLength() <= 0) {
            chain.doFilter(request, response);
            return;
        }
        chain.doFilter(new ContentCachingRequestWrapper(request, CACHE_LIMIT), response);
    }
}

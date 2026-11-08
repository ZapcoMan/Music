package com.shanzhu.music.interceptor;

import com.shanzhu.music.utils.IpUtils;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * traceId 写入与清理。
 *
 * logback 的 %X{traceId} 只是读取方；如果没人往 MDC 里写，
 * 日志会安静地留空列，看起来像"没有跨请求关联"这种设计选择，
 * 而不是配置失效。所以写入侧必须有断言。
 */
class RequestTraceIdTest {

    private final IpUtils ipUtils = mock(IpUtils.class);

    private RequestResponseLoggingInterceptor interceptor;

    @BeforeEach
    void setUp() {
        interceptor = new RequestResponseLoggingInterceptor(ipUtils);
        when(ipUtils.getIpAddress(any(HttpServletRequest.class))).thenReturn("10.1.2.3");
    }

    @AfterEach
    void clearMdc() {
        MDC.clear();
    }

    private MockHttpServletRequest request() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/songs");
        request.setQueryString("pageNum=1");
        return request;
    }

    @Test
    @DisplayName("preHandle 把 requestId 同时放进 attribute 与 MDC，且两者一致")
    void preHandlePublishesTraceId() throws Exception {
        MockHttpServletRequest request = request();

        assertTrue(interceptor.preHandle(request, new MockHttpServletResponse(), new Object()));

        String fromAttribute = (String) request.getAttribute("requestId");
        assertNotNull(fromAttribute, "响应结束日志要从 attribute 取回本次标识");
        assertEquals(fromAttribute, MDC.get("traceId"),
                "MDC 里的值必须与日志行里的请求ID同源，否则两边对不上");
    }

    @Test
    @DisplayName("afterCompletion 清理 MDC，线程回池后不带旧标识")
    void afterCompletionClearsTraceId() throws Exception {
        MockHttpServletRequest request = request();
        interceptor.preHandle(request, new MockHttpServletResponse(), new Object());
        assertNotNull(MDC.get("traceId"), "前置条件：本条请求确实带着 traceId");

        interceptor.afterCompletion(request, new MockHttpServletResponse(), new Object(), null);

        assertNull(MDC.get("traceId"),
                "线程复用是 servlet 容器的常态，残留的 traceId 会把下一个请求的日志挂到本次请求上");
    }

    @Test
    @DisplayName("请求ID不再由 timestamp 加 Math.random 生成")
    void traceIdIsUniqueEnough() throws Exception {
        java.util.Set<String> seen = new java.util.HashSet<>();
        for (int i = 0; i < 500; i++) {
            MockHttpServletRequest request = request();
            interceptor.preHandle(request, new MockHttpServletResponse(), new Object());
            String id = (String) request.getAttribute("requestId");
            assertTrue(seen.add(id), "第 " + i + " 次生成的请求ID与之前的重复：同一毫秒加万分之一随机数会撞");
            MDC.remove("traceId");
        }
        assertEquals(500, seen.size());
    }
}

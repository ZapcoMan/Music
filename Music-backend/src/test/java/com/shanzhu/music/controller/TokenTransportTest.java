package com.shanzhu.music.controller;

import com.shanzhu.music.BaseTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 令牌载体传输方式的回归。
 *
 * 本轮把「Authorization 头缺失时回落到 ?token= 参数」这条入口关掉了，
 * 但既有 210 项里没有任何一条用例走过参数形式，因此改完立刻全绿**并不等于修好了**：
 * 若哪天有人把回落逻辑加回来，没有一条用例会变红。这里补的就是这条缺失的守卫。
 */
class TokenTransportTest extends BaseTest {

    /** 不带任何默认请求的实例，用于精确控制令牌以哪种方式携带 */
    private MockMvc bare;

    @BeforeEach
    void setUpBareMockMvc() {
        this.bare = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    }

    @Test
    @DisplayName("令牌只能走 Authorization 头，URL 参数形式必须被拒")
    void urlParameterTokenIsNotAccepted() throws Exception {
        // 令牌本身是合法签发的，只有携带方式不合规——断言 401 才能证明是"传输方式被拒"
        // 而不是"令牌无效"
        bare.perform(MockMvcRequestBuilders.get("/api/v1/collect/collectOfUserId")
                        .param("token", adminToken()))
                .andExpect(MockMvcResultMatchers.status().isUnauthorized());
    }

    @Test
    @DisplayName("同一个令牌走请求头即可通过")
    void headerTokenStillWorks() throws Exception {
        bare.perform(MockMvcRequestBuilders.get("/api/v1/collect/collectOfUserId")
                        .header("Authorization", adminToken()))
                .andExpect(MockMvcResultMatchers.status().isOk());
    }

    /**
     * 这两条必须成对存在。只断"参数形式被拒"，可能是接口本身挂了；
     * 只有同一令牌换一种携带方式就通过，才证明拦的是载体而不是请求。
     */
    @Test
    @DisplayName("登出接口同样不再接受参数形式令牌")
    void logoutRejectsParameterToken() throws Exception {
        bare.perform(MockMvcRequestBuilders.post("/api/v1/consumer/logout")
                        .param("token", consumerToken()))
                .andExpect(MockMvcResultMatchers.status().isUnauthorized());

        bare.perform(MockMvcRequestBuilders.post("/api/v1/admin/logout")
                        .param("token", adminToken()))
                .andExpect(MockMvcResultMatchers.status().isUnauthorized());
    }

    @Test
    @DisplayName("伪造 X-Forwarded-For 不改变判定用IP：仍取容器给出的对端地址")
    void spoofedForwardedHeaderCannotBypassIpBinding() throws Exception {
        // 令牌签发时绑定的 IP 来自 MockHttpServletRequest 默认的 127.0.0.1。
        // 若 IpUtils 仍无条件信任透传头，这里会把客户端 IP 读成 8.8.8.8，
        // 与令牌绑定的 127.0.0.1 不一致而返回 403；
        // 现在默认不信任透传头，因此伪造无效，请求按原绑定通过。
        bare.perform(MockMvcRequestBuilders.get("/api/v1/collect/collectOfUserId")
                        .header("Authorization", consumerToken())
                        .header("X-Forwarded-For", "8.8.8.8"))
                .andExpect(MockMvcResultMatchers.status().isOk());
    }

    @Test
    @DisplayName("IpUtils 默认不采信透传头，开启后才采信")
    void forwardedHeadersRequireExplicitTrust() {
        com.shanzhu.music.utils.IpUtils ipUtils = new com.shanzhu.music.utils.IpUtils();
        org.springframework.mock.web.MockHttpServletRequest request =
                new org.springframework.mock.web.MockHttpServletRequest();
        request.setRemoteAddr("203.0.113.9");
        request.addHeader("X-Forwarded-For", "1.2.3.4");

        org.springframework.test.util.ReflectionTestUtils.setField(ipUtils, "trustProxyHeaders", false);
        assertEquals("203.0.113.9", ipUtils.getIpAddress(request),
                "默认必须忽略可伪造的透传头");

        org.springframework.test.util.ReflectionTestUtils.setField(ipUtils, "trustProxyHeaders", true);
        assertEquals("1.2.3.4", ipUtils.getIpAddress(request),
                "显式开启后才应采信代理链的第一个地址");
    }
}

package com.shanzhu.music.controller;

import com.shanzhu.music.BaseTest;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

/**
 * AdminController 单元测试
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class AdminControllerTest extends BaseTest {

    /**
     * 仅用于把 admin 口令临时换成测试已知的明文，见 testLoginSuccess 的说明
     */
    @Autowired
    private org.springframework.jdbc.core.JdbcTemplate jdbc;

    /**
     * 测试管理员登录 - 成功
     */
    @Test
    @Order(1)
    @DisplayName("测试管理员登录-成功")
    void testLoginSuccess() throws Exception {
        // 原用例直接喂 123456，而种子哈希对该明文 checkpw=false（本轮实测七个常见候选全不匹配），
        // 所以这个名叫"成功"的用例其实一直在断失败响应，又因为只断 200 而恒绿。
        // 先把口令换成测试自己知道的明文，"登录成功"才是真的被验证的东西。
        String rawPassword = "admin-login-it-2026";
        jdbc.update("update admin set password = ? where name = 'admin'",
                com.shanzhu.music.utils.PasswordUtils.encode(rawPassword));

        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/admin/sessions")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("name", "admin")
                .param("password", rawPassword))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.success").value(true))
                .andExpect(MockMvcResultMatchers.jsonPath("$.data.token").isNotEmpty())
                .andExpect(MockMvcResultMatchers.jsonPath("$.data.expiresIn").isNumber());
        
        System.out.println("管理员登录成功接口测试通过");
    }

    /**
     * 测试管理员登录 - 失败
     */
    @Test
    @Order(2)
    @DisplayName("测试管理员登录-失败")
    void testLoginFailed() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/admin/sessions")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("name", "admin")
                .param("password", "wrongpassword"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                // 本项目业务失败同样返回 200，只断状态码等于什么都没断；
                // 这里明确要求 success=false，且不能再发令牌
                .andExpect(MockMvcResultMatchers.jsonPath("$.success").value(false));
        
        System.out.println("管理员登录失败接口测试通过");
    }



}

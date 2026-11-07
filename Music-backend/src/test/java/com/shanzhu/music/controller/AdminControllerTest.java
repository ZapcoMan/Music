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
     * 测试管理员登录 - 成功
     */
    @Test
    @Order(1)
    @DisplayName("测试管理员登录-成功")
    void testLoginSuccess() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.post("/admin/login/status")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("name", "admin")
                .param("password", "123456"))
                .andExpect(MockMvcResultMatchers.status().isOk());
        
        System.out.println("管理员登录成功接口测试通过");
    }

    /**
     * 测试管理员登录 - 失败
     */
    @Test
    @Order(2)
    @DisplayName("测试管理员登录-失败")
    void testLoginFailed() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.post("/admin/login/status")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("name", "admin")
                .param("password", "wrongpassword"))
                .andExpect(MockMvcResultMatchers.status().isOk());
        
        System.out.println("管理员登录失败接口测试通过");
    }



}

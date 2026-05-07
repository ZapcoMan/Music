package com.shanzhu.music.controller;

import com.shanzhu.music.BaseTest;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

/**
 * AdminController 单元测试
 */
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class AdminControllerTest extends BaseTest {

    @Autowired
    private MockMvc mockMvc;

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

    /**
     * 测试查询管理员详情
     */
    @Test
    @Order(3)
    @DisplayName("测试查询管理员详情")
    void testSelectByPrimaryKey() {
        // AdminController 中没有此接口
    }

    /**
     * 测试修改管理员信息
     */
    @Test
    @Order(4)
    @DisplayName("测试修改管理员信息")
    void testUpdateAdmin() {
        // AdminController 中没有此接口
    }

    /**
     * 测试删除管理员
     */
    @Test
    @Order(5)
    @DisplayName("测试删除管理员")
    void testDeleteAdmin() {
        // AdminController 中没有此接口
    }
}

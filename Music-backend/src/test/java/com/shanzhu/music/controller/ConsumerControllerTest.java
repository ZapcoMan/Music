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
 * ConsumerController 单元测试
 */
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ConsumerControllerTest extends BaseTest {

    @Autowired
    private MockMvc mockMvc;

    /**
     * 测试用户登录 - 成功
     */
    @Test
    @Order(1)
    @DisplayName("测试用户登录-成功")
    void testLoginSuccess() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.post("/consumer/login/status")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("username", "testuser")
                .param("password", "123456"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.code").value(20000));
        
        System.out.println("用户登录成功接口测试通过");
    }

    /**
     * 测试用户登录 - 失败
     */
    @Test
    @Order(2)
    @DisplayName("测试用户登录-失败")
    void testLoginFailed() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.post("/consumer/login/status")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("username", "testuser")
                .param("password", "wrongpassword"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.code").value(20001));
        
        System.out.println("用户登录失败接口测试通过");
    }

    /**
     * 测试用户注册
     */
    @Test
    @Order(3)
    @DisplayName("测试用户注册")
    void testSignUp() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.post("/consumer/add")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("username", "newuser2026")
                .param("password", "123456")
                .param("sex", "1")
                .param("phoneNum", "13800138002")
                .param("email", "newuser@example.com")
                .param("birth", "1995-06-15")
                .param("introduction", "新用户")
                .param("location", "深圳")
                .param("avator", "/userImages/test.jpg"))
                .andExpect(MockMvcResultMatchers.status().isOk());
        
        System.out.println("用户注册接口测试通过");
    }

    /**
     * 测试查询用户详情
     */
    @Test
    @Order(4)
    @DisplayName("测试查询用户详情")
    void testSelectByPrimaryKey() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/consumer/detail")
                .param("id", "1"))
                .andExpect(MockMvcResultMatchers.status().isOk());
        
        System.out.println("查询用户详情接口测试通过");
    }

    /**
     * 测试修改用户信息
     */
    @Test
    @Order(5)
    @DisplayName("测试修改用户信息")
    void testUpdateConsumer() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.post("/consumer/update")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("id", "1")
                .param("username", "testuser")
                .param("sex", "1")
                .param("phoneNum", "13800138000")
                .param("email", "test@example.com")
                .param("birth", "1990-01-01")
                .param("introduction", "修改后的简介")
                .param("location", "广州")
                .param("avator", "/userImages/test.jpg"))
                .andExpect(MockMvcResultMatchers.status().isOk());
        
        System.out.println("修改用户信息接口测试通过");
    }

    /**
     * 测试分页查询用户
     */
    @Test
    @Order(6)
    @DisplayName("测试分页查询用户")
    void testAllConsumerByPage() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/consumer/allConsumerByPage")
                .param("pageNum", "1")
                .param("pageSize", "5"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.code").value(20000))
                .andExpect(MockMvcResultMatchers.jsonPath("$.data.list").isArray());
        
        System.out.println("分页查询用户接口测试通过");
    }

    /**
     * 测试删除用户
     */
    @Test
    @Order(7)
    @DisplayName("测试删除用户")
    void testDeleteConsumer() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/consumer/delete")
                .param("id", "99999"))
                .andExpect(MockMvcResultMatchers.status().isOk());
        
        System.out.println("删除用户接口测试通过");
    }
}

package com.shanzhu.music.controller;

import com.shanzhu.music.BaseTest;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

/**
 * ConsumerController 单元测试
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ConsumerControllerTest extends BaseTest {

    /**
     * 测试用户登录 - 成功
     */
    @Test
    @Order(1)
    @DisplayName("测试用户登录-成功")
    void testLoginSuccess() throws Exception {
        // ConsumerController 中的登录接口是 /consumer/login
        mockMvc.perform(MockMvcRequestBuilders.post("/consumer/login")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("username", "testuser")
                .param("password", "123456"))
                .andExpect(MockMvcResultMatchers.status().isOk());
        
        System.out.println("用户登录成功接口测试通过");
    }

    /**
     * 测试用户登录 - 失败
     */
    @Test
    @Order(2)
    @DisplayName("测试用户登录-失败")
    void testLoginFailed() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.post("/consumer/login")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("username", "testuser")
                .param("password", "wrongpassword"))
                .andExpect(MockMvcResultMatchers.status().isOk());
        
        System.out.println("用户登录失败接口测试通过");
    }

    /**
     * 测试用户注册
     */
    @Test
    @Order(3)
    @DisplayName("测试用户注册")
    void testSignUp() throws Exception {
        String jsonRequest = "{" +
                "\"username\":\"newuser2026\"," +
                "\"password\":\"123456\"," +
                "\"sex\":1," +
                "\"phoneNum\":\"13800138002\"," +
                "\"email\":\"newuser@example.com\"," +
                "\"birth\":\"1995-06-15\"," +
                "\"introduction\":\"新用户\"," +
                "\"location\":\"深圳\"," +
                "\"avator\":\"/userImages/test.jpg\"" +
                "}";
        
        mockMvc.perform(MockMvcRequestBuilders.post("/consumer/add")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonRequest))
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
        // ConsumerController 中的接口是 /consumer/selectByPrimaryKey
        mockMvc.perform(MockMvcRequestBuilders.get("/consumer/selectByPrimaryKey")
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
        String jsonRequest = "{" +
                "\"username\":\"testuser\"," +
                "\"password\":\"123456\"," +
                "\"sex\":1," +
                "\"phoneNum\":\"13800138000\"," +
                "\"email\":\"test@example.com\"," +
                "\"birth\":\"1990-01-01\"," +
                "\"introduction\":\"修改后的简介\"," +
                "\"location\":\"广州\"," +
                "\"avator\":\"/userImages/test.jpg\"" +
                "}";
        
        consumerMvc.perform(MockMvcRequestBuilders.post("/consumer/update")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonRequest))
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
        mockMvc.perform(MockMvcRequestBuilders.delete("/consumer/delete")
                .param("id", "99999"))
                .andExpect(MockMvcResultMatchers.status().isOk());
        
        System.out.println("删除用户接口测试通过");
    }

    @Test
    @Order(9)
    @DisplayName("测试参数校验失败的 code 取自枚举而非 400")
    void testParamValidationErrorUsesEnumCode() throws Exception {
        // 同一个"参数错误"语义此前有两个 code 来源：ResultCodeEnum.PARAM_ERROR 是 20002，
        // 而 GlobalExceptionHandler 直接写了 R.error(400, ...)
        String jsonRequest = "{\"username\":\"\",\"password\":\"123456\"}";
        mockMvc.perform(MockMvcRequestBuilders.post("/consumer/add")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonRequest))
                .andExpect(MockMvcResultMatchers.status().isBadRequest())
                .andExpect(MockMvcResultMatchers.jsonPath("$.success").value(false))
                .andExpect(MockMvcResultMatchers.jsonPath("$.code").value(20002))
                .andExpect(MockMvcResultMatchers.jsonPath("$.message").value(org.hamcrest.Matchers.containsString("用户名不能为空")));
    }
}

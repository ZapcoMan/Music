package com.shanzhu.music.controller;

import com.shanzhu.music.BaseTest;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

/**
 * CollectController 单元测试
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class CollectControllerTest extends BaseTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @Order(1)
    @DisplayName("测试添加收藏")
    void testAddCollect() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.post("/collect/add")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("userId", "1")
                .param("type", "0")
                .param("songId", "1"))
                .andExpect(MockMvcResultMatchers.status().isOk());
    }

    @Test
    @Order(2)
    @DisplayName("测试添加收藏-重复收藏")
    void testAddCollectDuplicate() throws Exception {
        // 先添加一次
        mockMvc.perform(MockMvcRequestBuilders.post("/collect/add")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("userId", "2")
                .param("type", "0")
                .param("songId", "2"));
        
        // 再次添加相同的收藏
        mockMvc.perform(MockMvcRequestBuilders.post("/collect/add")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("userId", "2")
                .param("type", "0")
                .param("songId", "2"))
                .andExpect(MockMvcResultMatchers.status().isOk());
    }

    @Test
    @Order(3)
    @DisplayName("测试删除收藏")
    void testDeleteCollect() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/collect/delete")
                .param("userId", "1")
                .param("songId", "1"))
                .andExpect(MockMvcResultMatchers.status().isOk());
    }

    @Test
    @Order(4)
    @DisplayName("测试查询所有收藏")
    void testAllCollect() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/collect/allCollect"))
                .andExpect(MockMvcResultMatchers.status().isOk());
    }

    @Test
    @Order(5)
    @DisplayName("测试查询用户收藏列表")
    void testCollectOfUserId() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/collect/collectOfUserId")
                .param("userId", "1"))
                .andExpect(MockMvcResultMatchers.status().isOk());
    }
}

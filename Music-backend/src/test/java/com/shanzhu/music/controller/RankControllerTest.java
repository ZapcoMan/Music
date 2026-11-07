package com.shanzhu.music.controller;

import com.shanzhu.music.BaseTest;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

/**
 * RankController 单元测试
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class RankControllerTest extends BaseTest {

    @Test
    @Order(1)
    @DisplayName("测试添加评价")
    void testAddRank() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.post("/rank/add")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("songListId", "1")
                .param("consumerId", "1")
                .param("score", "5"))
                .andExpect(MockMvcResultMatchers.status().isOk());
    }


    @Test
    @Order(2)
    @DisplayName("测试按歌单查询平均分")
    void testRankOfSongListId() throws Exception {
        // RankController 的读取接口是无路子路径的 @GetMapping，即 GET /rank
        mockMvc.perform(MockMvcRequestBuilders.get("/rank")
                .param("songListId", "1"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                // 返回的是 Integer 而非 R：断言它确实是个数字，而不是仅仅 200
                .andExpect(MockMvcResultMatchers.jsonPath("$").isNumber());
    }
}

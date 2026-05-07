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
 * RankController 单元测试
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class RankControllerTest extends BaseTest {

    @Autowired
    private MockMvc mockMvc;

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
    @DisplayName("测试查询歌单评分")
    void testRankOfSongListId() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/rank/rank")
                .param("songListId", "1"))
                .andExpect(MockMvcResultMatchers.status().isOk());
    }
}

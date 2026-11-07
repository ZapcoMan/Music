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
    @DisplayName("测试查询歌单评分")
    @Disabled("空壳用例：方法体内没有任何断言，此前仍被计为通过。需补齐断言后再启用。")
    void testRankOfSongListId() {
        // 该接口在没有评分数据时可能返回null导致500错误,在单元测试环境中跳过
    }
}

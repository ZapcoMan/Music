package com.shanzhu.music.controller;

import com.shanzhu.music.BaseTest;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

/**
 * RankController 单元测试
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class RankControllerTest extends BaseTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

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

    /**
     * 评分人身份必须来自令牌，而不是入参 consumerId。
     *
     * 此前 /rank/add 直接采用请求里的 consumerId，任何登录用户都能替任意账号打分，
     * 而评分是协同过滤与精排特征的直接输入——污染它等于污染推荐结果。
     * 这里用消费者令牌（userId=35）故意携带一个不属于它的 consumerId=999，
     * 断言落库的是 35 而不是 999。
     *
     * 歌单 id 现查而非写死：ranks 上有 (song_list_id, consumer_id) 唯一索引，
     * 种子数据已占用 35 的部分组合，写死会撞索引而失败，那与本次要验的东西无关。
     * 选题只依赖 ranks 表，被 ranks 引用过的歌单必然存在（孤儿引用已在 V8 清理）。
     */
    @Test
    @Order(3)
    @DisplayName("测试评分人取自令牌而非入参consumerId")
    void testRankRaterComesFromTokenNotParameter() throws Exception {
        Integer songListId = jdbcTemplate.queryForObject(
                "select r.song_list_id from ranks r " +
                        "where r.consumer_id <> 35 " +
                        "and not exists (select 1 from ranks r2 " +
                        "where r2.song_list_id = r.song_list_id and r2.consumer_id = 35) " +
                        "limit 1",
                Integer.class);

        mockMvc.perform(MockMvcRequestBuilders.post("/rank/add")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("songListId", String.valueOf(songListId))
                        .param("consumerId", "999")
                        .param("score", "8")
                        .header("Authorization", consumerToken()))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.success").value(true));

        Integer asTokenUser = jdbcTemplate.queryForObject(
                "select count(*) from ranks where song_list_id = ? and consumer_id = 35",
                Integer.class, songListId);
        Integer asForgedUser = jdbcTemplate.queryForObject(
                "select count(*) from ranks where song_list_id = ? and consumer_id = 999",
                Integer.class, songListId);

        Assertions.assertEquals(1, asTokenUser, "评分应按令牌身份 35 入库");
        Assertions.assertEquals(0, asForgedUser, "入参 consumerId=999 不应生效");
    }
}

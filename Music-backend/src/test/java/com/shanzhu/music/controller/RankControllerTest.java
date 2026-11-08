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
 *
 * 本轮只动路径：写接口 POST /api/v1/ranks（原 /rank/add），
 * 读接口改成歌单子资源 GET /api/v1/song-lists/{songListId}/rank（原 GET /rank?songListId=）。
 * 断言一律落到 ranks 表：本项目业务失败也返回 HTTP 200，只断 status/$.success 的用例等于没测。
 *
 * 用例各占一个"高段位"歌单 id 并在开头清场：测试库的 ranks 表没有种子行，
 * 也不像真实库那样带 (song_list_id, consumer_id) 唯一索引，
 * 借种子歌单 1、2 会被别的测试类插入的评分干扰成"计数对不上"的假失败。
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class RankControllerTest extends BaseTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private static final int LIST_FOR_WRITE = 987652;
    private static final int LIST_FOR_AVERAGE = 987654;
    private static final int LIST_FOR_EMPTY = 987653;
    private static final int LIST_FOR_FORGERY = 987651;
    private static final int LIST_FOR_ADMIN = 987650;

    /**
     * 清空该歌单的全部评分：均值与"应落库几行"这类断言只能建立在已知行数上
     */
    private void resetRanksOf(int songListId) {
        jdbcTemplate.update("delete from ranks where song_list_id = ?", songListId);
    }

    private int rankCount(Integer songListId, Integer consumerId) {
        Integer count = jdbcTemplate.queryForObject(
                "select count(*) from ranks where song_list_id = ? and consumer_id = ?",
                Integer.class, songListId, consumerId);
        return count == null ? 0 : count;
    }

    @Test
    @Order(1)
    @DisplayName("测试添加评价")
    void testAddRank() throws Exception {
        resetRanksOf(LIST_FOR_WRITE);

        consumerMvc.perform(MockMvcRequestBuilders.post("/api/v1/ranks")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("songListId", String.valueOf(LIST_FOR_WRITE))
                .param("score", "5"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.success").value(true));

        // 上一版只断 $.success 存在，连落库都没查；这里确认行确实写进去了且分数没被改写
        Assertions.assertEquals(1, rankCount(LIST_FOR_WRITE, 35), "评分应已落库");
        Assertions.assertEquals(5, jdbcTemplate.queryForObject(
                        "select score from ranks where song_list_id = ? and consumer_id = 35",
                        Integer.class, LIST_FOR_WRITE),
                "score 应等于提交值");
    }

    @Test
    @Order(2)
    @DisplayName("测试按歌单查询平均分")
    void testRankOfSongListId() throws Exception {
        // 平均分是整数除法，用两条已知评分把期望值钉死：(9 + 4) / 2 = 6。
        // 上一版只断 $.data 是数字——那时返回的是"无人评分"的默认值 5，等于没验算法
        resetRanksOf(LIST_FOR_AVERAGE);
        jdbcTemplate.update("insert into ranks (song_list_id, consumer_id, score) values (?, 401, 9)",
                LIST_FOR_AVERAGE);
        jdbcTemplate.update("insert into ranks (song_list_id, consumer_id, score) values (?, 402, 4)",
                LIST_FOR_AVERAGE);

        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/song-lists/" + LIST_FOR_AVERAGE + "/rank"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.success").value(true))
                .andExpect(MockMvcResultMatchers.jsonPath("$.data").value(6));

        // 路径变量必须真的参与统计范围：换一个歌单就该换一套数据（这里走"无人评分"分支）
        resetRanksOf(LIST_FOR_EMPTY);
        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/song-lists/" + LIST_FOR_EMPTY + "/rank"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.data").value(5));
    }

    @Test
    @Order(3)
    @DisplayName("测试无人评分的歌单返回默认分 5")
    void testRankOfSongListIdWithoutRanks() throws Exception {
        resetRanksOf(LIST_FOR_EMPTY);

        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/song-lists/" + LIST_FOR_EMPTY + "/rank"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.data").value(5));
    }

    /**
     * 评分人身份必须来自令牌，而不是入参 consumerId。
     *
     * 此前 /rank/add 直接采用请求里的 consumerId，任何登录用户都能替任意账号打分，
     * 而评分是协同过滤与精排特征的直接输入——污染它等于污染推荐结果。
     * 这里用消费者令牌（userId=35）故意携带一个不属于它的 consumerId=999，
     * 断言落库的是 35 而不是 999。
     */
    @Test
    @Order(4)
    @DisplayName("测试评分人取自令牌而非入参consumerId")
    void testRankRaterComesFromTokenNotParameter() throws Exception {
        resetRanksOf(LIST_FOR_FORGERY);

        consumerMvc.perform(MockMvcRequestBuilders.post("/api/v1/ranks")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("songListId", String.valueOf(LIST_FOR_FORGERY))
                .param("consumerId", "999")
                .param("score", "8")
                .header("Authorization", consumerToken()))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.success").value(true));

        Assertions.assertEquals(1, rankCount(LIST_FOR_FORGERY, 35), "评分应按令牌身份 35 入库");
        Assertions.assertEquals(0, rankCount(LIST_FOR_FORGERY, 999), "入参 consumerId=999 不应生效");
    }

    @Test
    @Order(5)
    @DisplayName("测试管理员令牌不能替消费者打分")
    void adminCannotWriteConsumerRank() throws Exception {
        resetRanksOf(LIST_FOR_ADMIN);

        // 评分的归属是 consumer 表的 id，admin 的 id 不在其中；
        // 角色域 fail-closed 之后这条必须是 403，而不是写出一条孤儿评分
        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/ranks")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("songListId", String.valueOf(LIST_FOR_ADMIN))
                .param("score", "5"))
                .andExpect(MockMvcResultMatchers.status().isForbidden());

        Assertions.assertEquals(0, rankCount(LIST_FOR_ADMIN, 1), "被拒的评分不应落库");
        Assertions.assertEquals(0, rankCount(LIST_FOR_ADMIN, 35), "被拒的评分不应落库");
    }
}

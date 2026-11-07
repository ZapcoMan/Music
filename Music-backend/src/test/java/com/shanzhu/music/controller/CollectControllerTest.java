package com.shanzhu.music.controller;

import com.shanzhu.music.BaseTest;
import com.shanzhu.music.entity.po.SongList;
import com.shanzhu.music.service.CollectService;
import org.springframework.beans.factory.annotation.Autowired;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

/**
 * CollectController 单元测试
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class CollectControllerTest extends BaseTest {

    @Autowired
    private CollectService collectService;

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
        mockMvc.perform(MockMvcRequestBuilders.delete("/collect/delete")
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

    @Test
    @Order(20)
    @DisplayName("测试收藏歌单可写入、可读回、可取消")
    void testSongListCollectRoundTrip() throws Exception {
        collectService.deleteByUserIdSongListId(1, 1);

        mockMvc.perform(MockMvcRequestBuilders.post("/collect/add")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("type", "1")
                .param("songListId", "1"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.success").value(true));

        java.util.List<SongList> mine = collectService.collectSongListOfUserId(1);
        org.junit.jupiter.api.Assertions.assertTrue(
                mine.stream().anyMatch(x -> x.getId().equals(1)),
                "刚收藏的歌单应出现在我的收藏歌单里，实际=" + mine.size() + " 条");

        // 重复收藏要挡住，而不是插出第二行
        mockMvc.perform(MockMvcRequestBuilders.post("/collect/add")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("type", "1")
                .param("songListId", "1"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.code").value(2));

        mockMvc.perform(MockMvcRequestBuilders.delete("/collect/delete")
                .param("songListId", "1"))
                .andExpect(MockMvcResultMatchers.status().isOk());

        collectService.collectSongListOfUserId(1);
        org.junit.jupiter.api.Assertions.assertFalse(
                collectService.collectSongListOfUserId(1).stream().anyMatch(x -> x.getId().equals(1)),
                "取消收藏后不应再出现");
    }

    @Test
    @Order(21)
    @DisplayName("测试收藏歌曲接口不传 songId 时给出参数错误而非 500")
    void testSongCollectRequiresSongId() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.post("/collect/add")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("type", "0"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.success").value(false))
                .andExpect(MockMvcResultMatchers.jsonPath("$.message").value("收藏歌曲为空"));
    }
}

package com.shanzhu.music.controller;

import com.shanzhu.music.BaseTest;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

/**
 * CommentController 单元测试
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class CommentControllerTest extends BaseTest {

    @Test
    @Order(1)
    @DisplayName("测试添加评论-歌曲评论")
    void testAddCommentSong() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.post("/comment/add")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("userId", "1")
                .param("type", "0")
                .param("songId", "1")
                .param("content", "这首歌很好听"))
                .andExpect(MockMvcResultMatchers.status().isOk());
    }

    @Test
    @Order(2)
    @DisplayName("测试添加评论-歌单评论")
    void testAddCommentSongList() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.post("/comment/add")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("userId", "1")
                .param("type", "1")
                .param("songListId", "1")
                .param("content", "这个歌单不错"))
                .andExpect(MockMvcResultMatchers.status().isOk());
    }

    @Test
    @Order(3)
    @DisplayName("测试修改评论")
    void testUpdateComment() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.post("/comment/update")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("id", "1")
                .param("userId", "1")
                .param("type", "0")
                .param("songId", "1")
                .param("content", "修改后的评论内容"))
                .andExpect(MockMvcResultMatchers.status().isOk());
    }

    @Test
    @Order(4)
    @DisplayName("测试删除评论")
    void testDeleteComment() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.delete("/comment/delete")
                .param("id", "1"))
                .andExpect(MockMvcResultMatchers.status().isOk());
    }

    @Test
    @Order(5)
    @DisplayName("测试查询评论详情")
    void testSelectByPrimaryKey() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/comment/selectByPrimaryKey")
                .param("id", "1"))
                .andExpect(MockMvcResultMatchers.status().isOk());
    }

    @Test
    @Order(6)
    @DisplayName("测试查询所有评论")
    void testAllComment() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/comment/allComment"))
                .andExpect(MockMvcResultMatchers.status().isOk());
    }

    @Test
    @Order(7)
    @DisplayName("测试查询歌曲评论")
    void testCommentOfSongId() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/comment/commentOfSongId")
                .param("songId", "1"))
                .andExpect(MockMvcResultMatchers.status().isOk());
    }

    @Test
    @Order(8)
    @DisplayName("测试查询歌单评论")
    void testCommentOfSongListId() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/comment/commentOfSongListId")
                .param("songListId", "1"))
                .andExpect(MockMvcResultMatchers.status().isOk());
    }

    @Test
    @Order(9)
    @DisplayName("测试点赞评论")
    void testLike() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.post("/comment/like")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("id", "1")
                .param("up", "10"))
                .andExpect(MockMvcResultMatchers.status().isOk());
    }

    @Test
    @Order(10)
    @DisplayName("测试获取高频词汇")
    void testGetFrequentWords() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/comment/frequent-words")
                .param("songId", "1"))
                .andExpect(MockMvcResultMatchers.status().isOk());
    }
}

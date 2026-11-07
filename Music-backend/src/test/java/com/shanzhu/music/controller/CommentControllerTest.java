package com.shanzhu.music.controller;

import com.shanzhu.music.BaseTest;
import com.shanzhu.music.entity.po.Comment;
import com.shanzhu.music.service.CommentService;
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

    @Autowired
    private CommentService commentService;

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

    @Test
    @Order(11)
    @DisplayName("测试发评论时提交的 userId 不被采纳")
    void testAddCommentIgnoresSpoofedUserId() throws Exception {
        // 以消费者身份（令牌 userId=35）提交，但请求参数谎称 userId=1
        mockMvc.perform(MockMvcRequestBuilders.post("/comment/add")
                .header("Authorization", consumerToken())
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("userId", "1")
                .param("type", "0")
                .param("songId", "1")
                .param("content", "冒充他人的评论"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.success").value(true));

        // 归属必须由令牌决定，而不是由请求里写谁就是谁
        Integer owner = null;
        for (Comment c : commentService.commentOfSongId(1)) {
            if ("冒充他人的评论".equals(c.getContent())) {
                owner = c.getUserId();
            }
        }
        Assertions.assertNotNull(owner, "评论应已落库");
        Assertions.assertEquals(35, owner, "评论归属应取自令牌，而不是请求参数里的 userId=1");
    }

    @Test
    @Order(12)
    @DisplayName("测试无法修改他人评论")
    void testUpdateOthersCommentRejected() throws Exception {
        // 先由管理员发一条评论（作者 userId=1）
        mockMvc.perform(MockMvcRequestBuilders.post("/comment/add")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("type", "0")
                .param("songId", "2")
                .param("content", "属于管理员的评论"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.success").value(true));

        Integer id = null;
        for (Comment c : commentService.commentOfSongId(2)) {
            if ("属于管理员的评论".equals(c.getContent())) {
                id = c.getId();
            }
        }
        Assertions.assertNotNull(id, "评论应已落库");

        // 换成消费者令牌（userId=35）去改它，应被拒绝且内容不变
        mockMvc.perform(MockMvcRequestBuilders.post("/comment/update")
                .header("Authorization", consumerToken())
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("id", String.valueOf(id))
                .param("type", "0")
                .param("songId", "2")
                .param("content", "被篡改的内容"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.success").value(false));

        Assertions.assertEquals("属于管理员的评论", commentService.selectByPrimaryKey(id).getContent(),
                "越权修改不应写入");
    }

    @Test
    @Order(13)
    @DisplayName("测试点赞不接受客户端给定的数值")
    void testLikeIgnoresClientUpValue() throws Exception {
        Comment target = null;
        for (Comment c : commentService.commentOfSongId(1)) {
            target = c;
            break;
        }
        Assertions.assertNotNull(target, "需要一个已存在的评论");

        int before = target.getUp() == null ? 0 : target.getUp();

        // 客户端谎报 up=99999，服务端应只 +1
        mockMvc.perform(MockMvcRequestBuilders.post("/comment/like")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("id", String.valueOf(target.getId()))
                .param("up", "99999"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.success").value(true));

        Assertions.assertEquals(before + 1, commentService.selectByPrimaryKey(target.getId()).getUp(),
                "点赞数应由服务端自增，而不是采纳请求参数");
    }
}

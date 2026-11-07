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
        consumerMvc.perform(MockMvcRequestBuilders.post("/comment/add")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("userId", "1")
                .param("type", "0")
                .param("songId", "1")
                .param("content", "这首歌很好听"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.success").exists());
    }

    @Test
    @Order(2)
    @DisplayName("测试添加评论-歌单评论")
    void testAddCommentSongList() throws Exception {
        consumerMvc.perform(MockMvcRequestBuilders.post("/comment/add")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("userId", "1")
                .param("type", "1")
                .param("songListId", "1")
                .param("content", "这个歌单不错"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.success").exists());
    }

    /**
     * 评论发出后不可修改，连作者本人也不行——所以不存在改写接口。
     * 这里断的是"入口已经关掉"这个产品口径本身：一旦有人再把 update 加回来，用例会失败。
     */
    @Test
    @Order(3)
    @DisplayName("测试评论改写入口已下线")
    void testCommentUpdateEndpointRetired() throws Exception {
        consumerMvc.perform(MockMvcRequestBuilders.post("/comment/update")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("id", "1")
                .param("type", "0")
                .param("songId", "1")
                .param("content", "修改后的评论内容"))
                .andExpect(MockMvcResultMatchers.status().isNotFound())
                .andExpect(MockMvcResultMatchers.jsonPath("$.success").value(false));
    }

    @Test
    @Order(4)
    @DisplayName("测试本人删除自己的评论")
    void testDeleteComment() throws Exception {
        // 删除要过归属校验，所以先造一条属于令牌用户(35)的评论再删它。
        // 上一版直接删种子数据 id=1（属于用户 1），拿到的是"无权删除他人评论"，
        // 而用例只断言 $.success 存在——被拒也算通过，到底删没删根本没人看。
        Comment own = new Comment();
        own.setUserId(35);
        own.setType((byte) 0);
        own.setSongId(2);
        own.setContent("本人可删除的评论");
        commentService.insert(own);
        Integer id = own.getId();
        Assertions.assertNotNull(id, "评论应已落库");

        consumerMvc.perform(MockMvcRequestBuilders.delete("/comment/delete")
                .param("id", String.valueOf(id)))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.success").value(true));

        Assertions.assertNull(commentService.selectByPrimaryKey(id), "评论应已被删除");
    }

    @Test
    @Order(5)
    @DisplayName("测试查询评论详情")
    void testSelectByPrimaryKey() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/comment/selectByPrimaryKey")
                .param("id", "1"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.success").exists());
    }

    @Test
    @Order(6)
    @DisplayName("测试查询所有评论")
    void testAllComment() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/comment/allComment"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.success").exists());
    }

    @Test
    @Order(7)
    @DisplayName("测试查询歌曲评论")
    void testCommentOfSongId() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/comment/commentOfSongId")
                .param("songId", "1"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.success").exists());
    }

    @Test
    @Order(8)
    @DisplayName("测试查询歌单评论")
    void testCommentOfSongListId() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/comment/commentOfSongListId")
                .param("songListId", "1"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.success").exists());
    }

    @Test
    @Order(9)
    @DisplayName("测试点赞评论")
    void testLike() throws Exception {
        consumerMvc.perform(MockMvcRequestBuilders.post("/comment/like")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("id", "1")
                .param("up", "10"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.success").exists());
    }

    @Test
    @Order(10)
    @DisplayName("测试获取高频词汇")
    void testGetFrequentWords() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/comment/frequent-words")
                .param("songId", "1"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.success").exists());
    }

    @Test
    @Order(11)
    @DisplayName("测试发评论时提交的 userId 不被采纳")
    void testAddCommentIgnoresSpoofedUserId() throws Exception {
        // 以消费者身份（令牌 userId=35）提交，但请求参数谎称 userId=1
        consumerMvc.perform(MockMvcRequestBuilders.post("/comment/add")
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
    @DisplayName("测试无法删除他人评论")
    void testDeleteOthersCommentRejected() throws Exception {
        // 经 service 直接造一条属于他人(userId=1)的评论。
        // 上一版想用 HTTP 造，但这条路已经走不通：/comment/add 只允许 consumer，
        // 作者必然是令牌里的 35，Admin 打它会 403 —— 造不出"他人评论"这个前提。
        Comment foreign = new Comment();
        foreign.setUserId(1);
        foreign.setType((byte) 0);
        foreign.setSongId(2);
        foreign.setContent("属于其他用户的评论");
        commentService.insert(foreign);
        Integer id = foreign.getId();
        Assertions.assertNotNull(id, "评论应已落库");

        // 换成消费者令牌（userId=35）去删它，应被拒绝且评论仍在
        consumerMvc.perform(MockMvcRequestBuilders.delete("/comment/delete")
                .param("id", String.valueOf(id)))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.success").value(false))
                .andExpect(MockMvcResultMatchers.jsonPath("$.message").value("无权删除他人评论"));

        Assertions.assertNotNull(commentService.selectByPrimaryKey(id), "越权删除不应生效");
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
        consumerMvc.perform(MockMvcRequestBuilders.post("/comment/like")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("id", String.valueOf(target.getId()))
                .param("up", "99999"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.success").value(true));

        Assertions.assertEquals(before + 1, commentService.selectByPrimaryKey(target.getId()).getUp(),
                "点赞数应由服务端自增，而不是采纳请求参数");
    }

    @Test
    @Order(14)
    @DisplayName("测试方法不匹配返回 405 而不是 500")
    void testWrongHttpMethodReturns405() throws Exception {
        // /comment/delete 是 @DeleteMapping；用 GET 打它是客户端错误，
        // 此前被全局异常处理归到 Exception 分支变成 500，日志看起来像服务端故障
        mockMvc.perform(MockMvcRequestBuilders.get("/comment/delete")
                .param("id", "1"))
                .andExpect(MockMvcResultMatchers.status().isMethodNotAllowed())
                .andExpect(MockMvcResultMatchers.jsonPath("$.success").value(false))
                .andExpect(MockMvcResultMatchers.jsonPath("$.code").value(405));
    }
}

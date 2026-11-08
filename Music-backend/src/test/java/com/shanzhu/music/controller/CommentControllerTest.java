package com.shanzhu.music.controller;

import com.shanzhu.music.BaseTest;
import com.shanzhu.music.entity.po.Comment;
import com.shanzhu.music.service.CommentService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

/**
 * CommentController 单元测试
 *
 * 两条贯穿全类的写法约定：
 * 1) 断言落到库（JdbcTemplate）而不只是响应体——本项目业务失败也返回 HTTP 200，
 *    只断 status / $.success 的用例即使什么都没做成也能通过；
 * 2) 前置数据由用例自己造——测试库的 comment 表没有种子行，原先"打 ?id=1
 *    然后断 $.success 存在"那几条拿到的是 data=null，照样绿。
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class CommentControllerTest extends BaseTest {

    @Autowired
    private CommentService commentService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    /**
     * 造一条属于给定作者的评论并返回其主键。
     * 走 service 而不是走 HTTP：写接口有角色域与限流，用它造前置数据会让"造数据"
     * 本身依赖被改造的端点，端点一坏就连坐一整片用例，定位不到根因。
     */
    private Integer newComment(Integer authorId, byte type, Integer refId, String content) {
        Comment comment = new Comment();
        comment.setUserId(authorId);
        comment.setType(type);
        if (type == 0) {
            comment.setSongId(refId);
        } else {
            comment.setSongListId(refId);
        }
        comment.setContent(content);
        commentService.insert(comment);
        Assertions.assertNotNull(comment.getId(), "评论应已落库");
        return comment.getId();
    }

    /**
     * 点赞数直接读库：列表接口带 Redis 缓存，而本机 Redis 跨测试实例共享，
     * 走缓存的断言会被并发实例清掉后读到旧值。
     */
    private int upOf(Integer id) {
        Integer up = jdbcTemplate.queryForObject("select up from comment where id = ?",
                Integer.class, id);
        return up == null ? 0 : up;
    }

    @Test
    @Order(1)
    @DisplayName("测试添加评论-歌曲评论")
    void testAddCommentSong() throws Exception {
        String content = "RESTful 改造-歌曲评论";
        consumerMvc.perform(MockMvcRequestBuilders.post("/api/v1/comments")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("type", "0")
                .param("songId", "2")
                .param("content", content))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.success").value(true));

        // 光断 success=true 不够：得确认这行真的写进 comment 且挂在了请求指定的歌曲上
        Assertions.assertEquals(1, jdbcTemplate.queryForObject(
                        "select count(*) from comment where content = ? and song_id = 2 and type = 0",
                        Integer.class, content),
                "歌曲评论应已落库并关联 song_id=2");
    }

    @Test
    @Order(2)
    @DisplayName("测试添加评论-歌单评论")
    void testAddCommentSongList() throws Exception {
        String content = "RESTful 改造-歌单评论";
        consumerMvc.perform(MockMvcRequestBuilders.post("/api/v1/comments")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("type", "1")
                .param("songListId", "2")
                .param("content", content))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.success").value(true));

        // type=1 走的是另一条分支：关联列必须是 song_list_id 且 song_id 为空，
        // 否则一条歌单评论会同时混进某首歌的评论区
        Assertions.assertEquals(1, jdbcTemplate.queryForObject(
                        "select count(*) from comment where content = ? and song_list_id = 2 " +
                                "and song_id is null and type = 1",
                        Integer.class, content),
                "歌单评论应已落库且只关联 song_list_id=2");
    }

    /**
     * 评论发出后不可修改，连作者本人也不行——所以不存在改写接口。
     *
     * 路径资源式化之后 update 有两种复活方式，这里都挡住：
     * 1) 在集合 {id} 上补 PATCH/PUT（RESTful 的默认写法）——该路径只映射了 GET 与 DELETE，
     *    所以是 405；2) 恢复旧的动词式子路径——没有这个映射，所以是 404。
     * 最后再确认真实正文一个字节都没被改过。
     */
    @Test
    @Order(3)
    @DisplayName("测试评论改写入口已下线")
    void testCommentUpdateEndpointRetired() throws Exception {
        String content = "RESTful 改造-不可改写的原文";
        Integer id = newComment(35, (byte) 0, 2, content);

        consumerMvc.perform(MockMvcRequestBuilders.patch("/api/v1/comments/" + id)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("content", "改过的正文"))
                .andExpect(MockMvcResultMatchers.status().isMethodNotAllowed())
                .andExpect(MockMvcResultMatchers.jsonPath("$.success").value(false));

        consumerMvc.perform(MockMvcRequestBuilders.post("/api/v1/comments/" + id + "/update")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("content", "改过的正文"))
                .andExpect(MockMvcResultMatchers.status().isNotFound())
                .andExpect(MockMvcResultMatchers.jsonPath("$.success").value(false));

        Assertions.assertEquals(content,
                jdbcTemplate.queryForObject("select content from comment where id = ?",
                        String.class, id),
                "评论正文不应被任何改写请求修改");
    }

    @Test
    @Order(4)
    @DisplayName("测试本人删除自己的评论")
    void testDeleteComment() throws Exception {
        // 删除要过归属校验，所以先造一条属于令牌用户(35)的评论再删它。
        // 上一版直接删种子数据 id=1（属于用户 1），拿到的是"无权删除他人评论"，
        // 而用例只断言 $.success 存在——被拒也算通过，到底删没删根本没人看。
        Integer id = newComment(35, (byte) 0, 2, "本人可删除的评论");

        consumerMvc.perform(MockMvcRequestBuilders.delete("/api/v1/comments/" + id))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.success").value(true));

        Assertions.assertNull(commentService.selectByPrimaryKey(id), "评论应已被删除");
        Assertions.assertEquals(0, jdbcTemplate.queryForObject(
                "select count(*) from comment where id = ?", Integer.class, id), "行应已从库中删除");
    }

    @Test
    @Order(5)
    @DisplayName("测试查询评论详情")
    void testSelectByPrimaryKey() throws Exception {
        // 上一版打的是 ?id=1，而 comment 表是空表：data 恒为 null，
        // 只断 $.success 存在照样绿——这条曾经什么都没测
        String content = "RESTful 改造-待查详情";
        Integer id = newComment(35, (byte) 0, 2, content);

        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/comments/" + id))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.success").value(true))
                .andExpect(MockMvcResultMatchers.jsonPath("$.data.id").value(id))
                .andExpect(MockMvcResultMatchers.jsonPath("$.data.content").value(content))
                .andExpect(MockMvcResultMatchers.jsonPath("$.data.userId").value(35));
    }

    @Test
    @Order(6)
    @DisplayName("测试查询所有评论-仅管理员")
    void testAllComment() throws Exception {
        Integer id = newComment(35, (byte) 0, 2, "RESTful 改造-全站评论列表项");

        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/comments"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.success").value(true))
                // 过滤式即使一条没中也返回空数组，用 exists 断等于没断，故用 isNotEmpty
                .andExpect(MockMvcResultMatchers.jsonPath("$.data[?(@.id == " + id + ")]")
                        .isNotEmpty());

        // 评论关联用户昵称等内容，属于跨用户数据读取，消费者令牌不该一次拉走全站
        consumerMvc.perform(MockMvcRequestBuilders.get("/api/v1/comments"))
                .andExpect(MockMvcResultMatchers.status().isForbidden());
    }

    @Test
    @Order(7)
    @DisplayName("测试查询歌曲评论子资源")
    void testCommentOfSongId() throws Exception {
        String content = "RESTful 改造-歌曲子资源评论";
        newComment(35, (byte) 0, 2, content);

        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/songs/2/comments"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.success").value(true))
                .andExpect(MockMvcResultMatchers.jsonPath("$.data[?(@.content == '" + content + "')]")
                        .isNotEmpty());

        // 本用例造的评论只挂在 song_id=2 上：若端点漏掉范围条件退化成全表，
        // 上面那句照样命中，所以再按库确认它没有别的归属
        Assertions.assertEquals(1, jdbcTemplate.queryForObject(
                        "select count(*) from comment where content = ? and song_id = 2",
                        Integer.class, content),
                "用例造的评论只属于 song_id=2");
    }

    @Test
    @Order(8)
    @DisplayName("测试查询歌单评论子资源")
    void testCommentOfSongListId() throws Exception {
        String content = "RESTful 改造-歌单子资源评论";
        newComment(35, (byte) 1, 2, content);

        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/song-lists/2/comments"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.success").value(true))
                .andExpect(MockMvcResultMatchers.jsonPath("$.data[?(@.content == '" + content + "')]")
                        .isNotEmpty());

        Assertions.assertEquals(1, jdbcTemplate.queryForObject(
                        "select count(*) from comment where content = ? and song_list_id = 2 and type = 1",
                        Integer.class, content),
                "用例造的评论只属于 song_list_id=2");
    }

    @Test
    @Order(9)
    @DisplayName("测试点赞评论")
    void testLike() throws Exception {
        // 上一版打的是 /comment/like?id=1&up=10 且只断 $.success 存在：
        // comment 表是空表，那条请求拿到的是"评论不存在"，success=false 也算过
        Integer id = newComment(35, (byte) 0, 2, "RESTful 改造-点赞目标");
        int before = upOf(id);

        consumerMvc.perform(MockMvcRequestBuilders.post("/api/v1/comments/" + id + "/likes"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.success").value(true));

        Assertions.assertEquals(before + 1, upOf(id), "点赞后 up 应恰好 +1");
    }

    @Test
    @Order(10)
    @DisplayName("测试点赞不存在的评论给出可区分的失败")
    void testLikeMissingComment() throws Exception {
        consumerMvc.perform(MockMvcRequestBuilders.post("/api/v1/comments/999999/likes"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.success").value(false))
                .andExpect(MockMvcResultMatchers.jsonPath("$.message").value("评论不存在"));
    }

    @Test
    @Order(11)
    @DisplayName("测试获取歌单高频词汇")
    void testGetFrequentWords() throws Exception {
        // 这条统计的 SQL 是 `where type = 1 and song_list_id = ?`，读的是歌单评论，
        // 所以路径挂在歌单下（旧参数名 songId 是历史误名）。
        // 词频门槛是"出现次数 >= 2"，故造两条相同内容；songListId 取 3 与本类其它用例隔离
        newComment(35, (byte) 1, 3, "好听好听");
        newComment(35, (byte) 1, 3, "好听好听");

        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/song-lists/3/frequent-words"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.success").value(true))
                .andExpect(MockMvcResultMatchers.jsonPath("$.data").isArray())
                .andExpect(MockMvcResultMatchers.jsonPath("$.data[?(@ == '好听')]").isNotEmpty());
    }

    @Test
    @Order(12)
    @DisplayName("测试发评论时提交的 userId 不被采纳")
    void testAddCommentIgnoresSpoofedUserId() throws Exception {
        String content = "RESTful 改造-冒充他人的评论";
        // 以消费者身份（令牌 userId=35）提交，但请求参数谎称 userId=1
        consumerMvc.perform(MockMvcRequestBuilders.post("/api/v1/comments")
                .header("Authorization", consumerToken())
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("userId", "1")
                .param("type", "0")
                .param("songId", "2")
                .param("content", content))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.success").value(true));

        // 归属必须由令牌决定，而不是由请求里写谁就是谁。
        // 上一版从带 Redis 缓存的 commentOfSongId(1) 里找，缓存被并发实例清掉时会找不到行
        Assertions.assertEquals(1, jdbcTemplate.queryForObject(
                        "select count(*) from comment where content = ? and user_id = 35",
                        Integer.class, content),
                "评论归属应取自令牌");
        Assertions.assertEquals(0, jdbcTemplate.queryForObject(
                        "select count(*) from comment where content = ? and user_id = 1",
                        Integer.class, content),
                "请求参数里的 userId=1 不应生效");
    }

    @Test
    @Order(13)
    @DisplayName("测试无法删除他人评论")
    void testDeleteOthersCommentRejected() throws Exception {
        // 经 service 直接造一条属于他人(userId=1)的评论。
        // 上一版想用 HTTP 造，但这条路已经走不通：POST /api/v1/comments 只允许 consumer，
        // 作者必然是令牌里的 35，Admin 打它会 403 —— 造不出"他人评论"这个前提。
        Integer id = newComment(1, (byte) 0, 2, "属于其他用户的评论");

        // 换成消费者令牌（userId=35）去删它，应被拒绝且评论仍在
        consumerMvc.perform(MockMvcRequestBuilders.delete("/api/v1/comments/" + id))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.success").value(false))
                .andExpect(MockMvcResultMatchers.jsonPath("$.message").value("无权删除他人评论"));

        Assertions.assertNotNull(commentService.selectByPrimaryKey(id), "越权删除不应生效");
        Assertions.assertEquals(1, jdbcTemplate.queryForObject(
                        "select count(*) from comment where id = ?", Integer.class, id),
                "越权删除不应生效（直接查库确认）");
    }

    @Test
    @Order(14)
    @DisplayName("测试点赞不接受客户端给定的数值")
    void testLikeIgnoresClientUpValue() throws Exception {
        // 上一版从带缓存的列表里"取第一条"当点赞对象：空表时用例直接失败，
        // 有数据时点的是别人造的行，前后值还可能被并发实例改动
        Integer id = newComment(35, (byte) 0, 2, "RESTful 改造-客户端刷点赞");
        int before = upOf(id);

        // 客户端谎报 up=99999，服务端应只 +1
        consumerMvc.perform(MockMvcRequestBuilders.post("/api/v1/comments/" + id + "/likes")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("up", "99999"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.success").value(true));

        Assertions.assertEquals(before + 1, upOf(id),
                "点赞数应由服务端自增，而不是采纳请求参数");
    }

    @Test
    @Order(15)
    @DisplayName("测试删除不存在的评论与越权可区分")
    void testDeleteMissingComment() throws Exception {
        // 归属校验下沉到 service 之后，"不存在"和"无权"是两种可分辨的失败；
        // 旧实现把它们和数据库删除失败一起塌成 data=false
        consumerMvc.perform(MockMvcRequestBuilders.delete("/api/v1/comments/999999"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.success").value(false))
                .andExpect(MockMvcResultMatchers.jsonPath("$.message").value("评论不存在"));
    }

    @Test
    @Order(16)
    @DisplayName("测试方法不匹配返回 405 而不是 500")
    void testWrongHttpMethodReturns405() throws Exception {
        // /api/v1/comments/{id}/likes 只有 @PostMapping；用 GET 打它是客户端错误，
        // 此前被全局异常处理归到 Exception 分支变成 500，日志看起来像服务端故障
        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/comments/1/likes"))
                .andExpect(MockMvcResultMatchers.status().isMethodNotAllowed())
                .andExpect(MockMvcResultMatchers.jsonPath("$.success").value(false))
                .andExpect(MockMvcResultMatchers.jsonPath("$.code").value(405));
    }
}

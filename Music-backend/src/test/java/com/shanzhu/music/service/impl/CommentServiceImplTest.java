package com.shanzhu.music.service.impl;

import com.shanzhu.music.BaseTest;
import com.shanzhu.music.entity.po.Comment;
import com.shanzhu.music.exception.BusinessException;
import com.shanzhu.music.service.CommentService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * CommentService 单元测试
 *
 * 两条写法约定：
 * 1) 不用 `if (testCommentId != null)` 包住断言——静态字段为 null 时整个用例
 *    一条断言都不执行却照样通过，这是最隐蔽的一种假绿；前置没准备好就该失败；
 * 2) 需要"列表里查得到"这类断言时，用只有本类会碰的合成 id：
 *    commentOfSongId / commentOfSongListId 带 Redis 缓存，而本机 Redis 跨测试实例共享，
 *    借种子的 song 1、歌单 1 做成员断言会被别的并发 JVM 干扰成偶发失败。
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class CommentServiceImplTest extends BaseTest {

    @Autowired
    private CommentService commentService;

    private static Integer testCommentId;

    /**
     * 测试添加评论
     */
    @Test
    @Order(1)
    @DisplayName("测试添加评论")
    void testInsert() {
        Comment comment = new Comment();
        comment.setUserId(1);
        comment.setType((byte) 0); // 0表示歌曲评论
        comment.setSongId(1);
        comment.setContent("这是一条测试评论");

        Boolean result = commentService.insert(comment);
        assertTrue(result, "添加评论应该成功");
        
        assertNotNull(comment.getId(), "评论ID不应为空");
        testCommentId = comment.getId();
        System.out.println("添加的评论ID: " + testCommentId);
    }

    /**
     * 测试查询评论
     */
    @Test
    @Order(2)
    @DisplayName("测试查询评论")
    void testSelectByPrimaryKey() {
        // 原先写成 `if (testCommentId != null) { ... }`：静态字段为 null 时一条断言都不执行，
        // 用例仍然通过——那是假绿，前置没准备好就应该失败
        assertNotNull(testCommentId, "前置评论应已由 Order(1) 落库");
        Comment comment = commentService.selectByPrimaryKey(testCommentId);
        assertNotNull(comment, "查询结果不应为空");
        assertEquals("这是一条测试评论", comment.getContent(), "评论内容应该匹配");
        assertEquals(1, comment.getUserId(), "归属应等于写入时的 userId");
        // type 是 Byte：直接和 int 字面量比会走 (Object,Object) 重载，
        // Integer(0) 与 Byte(0) 永远不相等，必须先 intValue()
        assertEquals(0, comment.getType().intValue(), "歌曲评论的 type 应为 0");
    }

    /**
     * 测试点赞由服务端自增
     */
    @Test
    @Order(3)
    @DisplayName("测试点赞由服务端按当前值自增")
    void testLike() {
        Comment before = commentService.selectByPrimaryKey(testCommentId);
        assertNotNull(before, "测试前置评论应存在");
        int up = before.getUp() == null ? 0 : before.getUp();

        assertTrue(commentService.like(testCommentId), "点赞应该成功");

        assertEquals(up + 1, commentService.selectByPrimaryKey(testCommentId).getUp().intValue(),
                "点赞数应由服务端自增，而不是采纳客户端给定的数值");
    }

    /**
     * 测试查询所有评论
     */
    @Test
    @Order(4)
    @DisplayName("测试查询所有评论")
    void testAllComment() {
        List<Comment> comments = commentService.allComment();
        assertNotNull(comments, "评论列表不应为空");
        // 只断"不是 null"的话，返回空表也算过：这里要求本类 Order(1) 造的那条真的在结果里。
        // allComment 不走 Redis 缓存，所以这条成员断言不受并发实例清缓存的影响
        assertTrue(comments.stream().anyMatch(c -> testCommentId.equals(c.getId())),
                "全量评论应包含 Order(1) 写入的那条");
    }

    /**
     * 测试查询歌曲的评论
     */
    @Test
    @Order(5)
    @DisplayName("测试查询歌曲的评论")
    void testCommentOfSongId() {
        // 用只属于本用例的合成歌曲 id：commentOfSongId 带 Redis 缓存，
        // 借种子的 song 1 做成员断言会被并发实例的缓存写入干扰
        Integer songId = 987620;
        Comment comment = new Comment();
        comment.setUserId(1);
        comment.setType((byte) 0);
        comment.setSongId(songId);
        comment.setContent("按歌曲查的评论");
        commentService.insert(comment);

        List<Comment> comments = commentService.commentOfSongId(songId);
        assertNotNull(comments, "评论列表不应为空");
        assertEquals(1, comments.size(), "该歌曲下应只有本用例造的那一条");
        assertEquals(comment.getId(), comments.get(0).getId(), "应读回刚写入的评论");
        assertEquals(songId, comments.get(0).getSongId(), "列表应按 song_id 过滤");
    }

    /**
     * 测试查询歌单的评论
     */
    @Test
    @Order(6)
    @DisplayName("测试查询歌单的评论")
    void testCommentOfSongListId() {
        Integer songListId = 987621;
        Comment comment = new Comment();
        comment.setUserId(1);
        comment.setType((byte) 1);
        comment.setSongListId(songListId);
        comment.setContent("按歌单查的评论");
        commentService.insert(comment);

        List<Comment> comments = commentService.commentOfSongListId(songListId);
        assertNotNull(comments, "评论列表不应为空");
        assertEquals(1, comments.size(), "该歌单下应只有本用例造的那一条");
        assertEquals(comment.getId(), comments.get(0).getId(), "应读回刚写入的歌单评论");
        assertNull(comments.get(0).getSongId(), "歌单评论不应带 song_id");
    }

    /**
     * 测试获取所有评论内容
     */
    @Test
    @Order(7)
    @DisplayName("测试获取所有评论内容")
    void testGetAllComments() {
        // 这条 SQL 是 `select content from comment where type = 1`，只收歌单评论；
        // 只断"不是 null"等于什么都没测，这里要求 Order(6) 造的歌单评论内容真的在里面
        List<String> comments = commentService.getAllComments();
        assertNotNull(comments, "评论内容列表不应为空");
        assertTrue(comments.contains("按歌单查的评论"), "应包含 type=1 的评论内容");
        assertFalse(comments.contains("这是一条测试评论"), "type=0 的歌曲评论不该出现");
    }

    /**
     * 测试获取高频词汇
     */
    @Test
    @Order(8)
    @DisplayName("测试获取高频词汇只统计该歌单的评论")
    void testGetFrequentWords() {
        // 词频门槛是"出现次数 >= 2"，所以造两条相同内容；
        // 再给另一个歌单造一条更高的频次，用来证明统计范围确实按歌单切开了
        Integer songListId = 987622;
        Integer otherSongListId = 987623;
        newCommentOfSongList(songListId, "好听好听");
        newCommentOfSongList(songListId, "好听好听");
        newCommentOfSongList(otherSongListId, "难听难听");
        newCommentOfSongList(otherSongListId, "难听难听");
        newCommentOfSongList(otherSongListId, "难听难听");

        List<String> mine = commentService.getFrequentWords(String.valueOf(songListId));
        assertNotNull(mine, "高频词汇列表不应为空");
        assertTrue(mine.contains("好听"), "本歌单重复出现的词应入选，实际=" + mine);
        assertFalse(mine.contains("难听"), "别的歌单的词频不该混进来");

        List<String> others = commentService.getFrequentWords(String.valueOf(otherSongListId));
        assertTrue(others.contains("难听"), "另一个歌单应统计到自己的高频词，实际=" + others);
        assertFalse(others.contains("好听"), "不该混入本歌单的词频");
    }

    /**
     * 造一条歌单评论（type=1），供上面两条统计类用例使用
     */
    private void newCommentOfSongList(Integer songListId, String content) {
        Comment comment = new Comment();
        comment.setUserId(1);
        comment.setType((byte) 1);
        comment.setSongListId(songListId);
        comment.setContent(content);
        commentService.insert(comment);
    }

    /**
     * 测试删除评论
     */
    @Test
    @Order(9)
    @DisplayName("测试删除评论")
    void testDelete() {
        assertNotNull(testCommentId, "前置评论应已落库");

        Boolean result = commentService.delete(testCommentId);
        assertTrue(result, "删除评论应该成功");

        // 验证删除
        Comment deletedComment = commentService.selectByPrimaryKey(testCommentId);
        assertNull(deletedComment, "删除后查询应该返回null");
        assertFalse(commentService.allComment().stream()
                        .anyMatch(c -> testCommentId.equals(c.getId())),
                "删除后全量列表里也不应再有这条");
    }

    /**
     * 测试删除接口的归属校验
     *
     * "评论不存在"与"无权删除"必须是两种可区分的失败：此前 Controller 把它们和
     * 数据库删除失败一起塌成同一个 false，前端只能显示一句模糊的"删除失败"。
     */
    @Test
    @Order(10)
    @DisplayName("测试 deleteBy 区分越权、不存在与成功")
    void testDeleteByOwnership() {
        Comment foreign = new Comment();
        foreign.setUserId(1);
        foreign.setType((byte) 0);
        foreign.setSongId(1);
        foreign.setContent("属于用户1的评论");
        commentService.insert(foreign);
        Integer id = foreign.getId();
        assertNotNull(id, "评论应已落库");

        BusinessException denied = assertThrows(BusinessException.class,
                () -> commentService.deleteBy(id, 2, "consumer"),
                "非本人非管理员删除他人评论应被拒");
        assertEquals("无权删除他人评论", denied.getMessage());
        assertNotNull(commentService.selectByPrimaryKey(id), "被拒时不应删除");

        assertTrue(commentService.deleteBy(id, 9, "admin"), "管理员可清理违规评论");
        assertNull(commentService.selectByPrimaryKey(id), "管理员删除后应查不到");

        BusinessException notFound = assertThrows(BusinessException.class,
                () -> commentService.deleteBy(-1, 2, "consumer"));
        assertEquals("评论不存在", notFound.getMessage());
    }
}

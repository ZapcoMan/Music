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
        if (testCommentId != null) {
            Comment comment = commentService.selectByPrimaryKey(testCommentId);
            assertNotNull(comment, "查询结果不应为空");
            assertEquals("这是一条测试评论", comment.getContent(), "评论内容应该匹配");
            System.out.println("查询到的评论: " + comment.getContent());
        }
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
        System.out.println("评论总数: " + comments.size());
    }

    /**
     * 测试查询歌曲的评论
     */
    @Test
    @Order(5)
    @DisplayName("测试查询歌曲的评论")
    void testCommentOfSongId() {
        List<Comment> comments = commentService.commentOfSongId(1);
        assertNotNull(comments, "评论列表不应为空");
        System.out.println("歌曲ID=1的评论数量: " + comments.size());
    }

    /**
     * 测试查询歌单的评论
     */
    @Test
    @Order(6)
    @DisplayName("测试查询歌单的评论")
    void testCommentOfSongListId() {
        List<Comment> comments = commentService.commentOfSongListId(1);
        assertNotNull(comments, "评论列表不应为空");
        System.out.println("歌单ID=1的评论数量: " + comments.size());
    }

    /**
     * 测试获取所有评论内容
     */
    @Test
    @Order(7)
    @DisplayName("测试获取所有评论内容")
    void testGetAllComments() {
        List<String> comments = commentService.getAllComments();
        assertNotNull(comments, "评论内容列表不应为空");
        System.out.println("所有评论内容数量: " + comments.size());
    }

    /**
     * 测试获取高频词汇
     */
    @Test
    @Order(8)
    @DisplayName("测试获取高频词汇")
    void testGetFrequentWords() {
        List<String> words = commentService.getFrequentWords("1");
        assertNotNull(words, "高频词汇列表不应为空");
        System.out.println("高频词汇数量: " + words.size());
    }

    /**
     * 测试删除评论
     */
    @Test
    @Order(9)
    @DisplayName("测试删除评论")
    void testDelete() {
        if (testCommentId != null) {
            Boolean result = commentService.delete(testCommentId);
            assertTrue(result, "删除评论应该成功");

            // 验证删除
            Comment deletedComment = commentService.selectByPrimaryKey(testCommentId);
            assertNull(deletedComment, "删除后查询应该返回null");
            System.out.println("评论已删除,ID: " + testCommentId);
        }
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

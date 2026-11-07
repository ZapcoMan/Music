package com.shanzhu.music.controller;

import com.shanzhu.music.annotation.RateLimit;
import com.shanzhu.music.annotation.RequireUserType;
import com.shanzhu.music.common.R;
import com.shanzhu.music.entity.po.Comment;
import com.shanzhu.music.service.CommentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 评论 控制层
 */
@Slf4j
@RestController
@RequestMapping("/comment")
@RequireUserType({"consumer", "admin"})
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;

    /**
     * 添加评论
     *
     * @param type       评论类型（0歌曲1歌单）
     * @param songId     歌曲id
     * @param songListId 歌单id
     * @param content    评论内容
     * @return 添加结果
     */
    @RateLimit(key = "comment:add:", timeWindow = 60, maxRequests = 10, message = "评论发表过于频繁，请稍后再试")
    @PostMapping(value = "/add")
    @RequireUserType("consumer")
    public R<Void> addComment(@RequestAttribute("userId") Integer userId,
                              Byte type, Integer songId, Integer songListId, String content) {
        // type 是 Byte，前端漏传时与 0 比较会触发拆箱 NPE
        if (type == null) {
            return R.error("评论类型不能为空");
        }

        // 身份只取令牌：原先 userId 由请求参数带入，任何人传别人的 id 就能替别人发评论
        Comment comment = new Comment();
        comment.setUserId(userId);
        comment.setType(type);
        comment.setContent(content);

        if (type == 0) {
            comment.setSongId(songId);
        } else {
            comment.setSongListId(songListId);
        }

        if (commentService.insert(comment)) {
            return R.ok("评论成功");
        }

        return R.error("评论失败");
    }

    /**
     * 修改评论
     *
     * @param id         主键
     * @param type       评论类型（0歌曲1歌单）
     * @param songId     歌曲id
     * @param songListId 歌单列表id
     * @param content    评论内容
     * @return 评论结果
     */
    @RateLimit(key = "comment:update:", timeWindow = 60, maxRequests = 20, message = "评论修改过于频繁，请稍后再试")
    @PostMapping(value = "/update")
    @RequireUserType({"consumer", "admin"})
    public R<Void> updateComment(@RequestAttribute("userId") Integer userId,
                           @RequestAttribute("userType") String userType,
                           Integer id, Byte type, Integer songId, Integer songListId, String content) {
        Comment origin = commentService.selectByPrimaryKey(id);
        if (origin == null) {
            return R.error("评论不存在");
        }
        // 归属校验：非本人且非管理员不得改写，
        // 原先把 userId 当成普通参数，提交别人的 id 就能把评论"改到"别人名下
        if (!userId.equals(origin.getUserId()) && !"admin".equals(userType)) {
            log.warn("评论修改被拒 - 令牌用户={}, 评论作者={}, commentId={}", userId, origin.getUserId(), id);
            return R.error("无权修改他人评论");
        }
        //保存到评论的对象中
        Comment comment = new Comment();
        comment.setId(id);
        comment.setUserId(origin.getUserId());
        comment.setType(type);
        comment.setSongId(songId);
        comment.setSongListId(songListId);
        comment.setContent(content);

        //更新成功
        if (commentService.update(comment)) {
            return R.ok("修改成功");
        }

        return R.error("修改失败");
    }

    /**
     * 删除评论
     *
     * @param id 评论id
     * @return 删除结果
     */
    @RateLimit(key = "comment:delete:", timeWindow = 60, maxRequests = 20, message = "评论删除过于频繁，请稍后再试")
    @DeleteMapping(value = "/delete")
    @RequireUserType({"consumer", "admin"})
    public R<Boolean> deleteComment(@RequestAttribute("userId") Integer userId,
                                 @RequestAttribute("userType") String userType,
                                 Integer id) {
        Comment origin = commentService.selectByPrimaryKey(id);
        if (origin == null) {
            return R.error("评论不存在");
        }
        if (!userId.equals(origin.getUserId()) && !"admin".equals(userType)) {
            log.warn("评论删除被拒 - 令牌用户={}, 评论作者={}, commentId={}", userId, origin.getUserId(), id);
            return R.error("无权删除他人评论");
        }
        // 原先"不存在""无权""删除失败"三种情况全塌成同一个 false，调用方无从分辨；
        // 借统一返回结构之机拆开，成功路径仍返回 data=true
        return R.ok("删除成功", commentService.delete(id));
    }

    /**
     * 通过主键查询评论
     *
     * @param id 评论主键
     * @return 评论
     */
    @GetMapping(value = "/selectByPrimaryKey")
    public R<Comment> selectByPrimaryKey(Integer id) {
        return R.ok(commentService.selectByPrimaryKey(id));
    }

    /**
     * 查询所有评论
     *
     * @return 所有评论
     */
    @GetMapping(value = "/allComment")
    // 这条此前只要求"已登录"，任何消费者令牌都能一次拉走全站评论。评论关联用户昵称等内容，
    // 属于跨用户数据读取，不只是性能问题，故补 admin 限定。
    @RequireUserType("admin")
    public R<List<Comment>> allComment() {
        return R.ok(commentService.allComment());
    }

    /**
     * 查询某个歌曲下的所有评论
     *
     * @param songId 歌曲id
     * @return 评论列表
     */
    @GetMapping(value = "/commentOfSongId")
    public R<List<Comment>> commentOfSongId(Integer songId) {
        return R.ok(commentService.commentOfSongId(songId));
    }

    /**
     * 查询某个歌单下的所有评论
     *
     * @param songListId 歌单id
     * @return 评论列表
     */
    @GetMapping(value = "/commentOfSongListId")
    public R<List<Comment>> commentOfSongListId(Integer songListId) {
        return R.ok(commentService.commentOfSongListId(songListId));
    }

    /**
     * 给某个评论点赞
     *
     * @param id 评论id
     * @return 结果
     */
    @RateLimit(key = "comment:like:", timeWindow = 60, maxRequests = 30, message = "点赞过于频繁，请稍后再试")
    @PostMapping(value = "/like")
    @RequireUserType("consumer")
    public R<Void> like(Integer id) {
        Comment origin = commentService.selectByPrimaryKey(id);
        if (origin == null) {
            return R.error("评论不存在");
        }
        // up 原先直接取请求参数，客户端传多少就涨多少（一次请求即可刷到任意数值）。
        // 现按当前值自增，前端仍传 up 字段但服务端不再采纳。
        int current = origin.getUp() == null ? 0 : origin.getUp();
        //保存到评论的对象中
        Comment comment = new Comment();
        comment.setId(id);
        comment.setUp(current + 1);

        //更新成功
        if (commentService.update(comment)) {
            return R.ok("点赞成功");
        }

        return R.error("点赞失败");
    }
    @GetMapping("/frequent-words")
    public R<List<String>> getFrequentWords(String songId) {

        log.info("id = " + songId);
        return R.ok("",commentService.getFrequentWords(songId));

    }
}

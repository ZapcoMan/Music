package com.shanzhu.music.controller;

import com.shanzhu.music.annotation.RateLimit;
import com.shanzhu.music.common.R;
import com.shanzhu.music.entity.po.Comment;
import com.shanzhu.music.service.CommentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
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
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;

    /**
     * 添加评论
     *
     * @param userId     用户id
     * @param type       评论类型（0歌曲1歌单）
     * @param songId     歌曲id
     * @param songListId 歌单id
     * @param content    评论内容
     * @return 添加结果
     */
    @RateLimit(key = "comment:add:", timeWindow = 60, maxRequests = 10, message = "评论发表过于频繁，请稍后再试")
    @PostMapping(value = "/add")
    public R addComment(Integer userId, Byte type, Integer songId, Integer songListId, String content) {
        // type 是 Byte，前端漏传时与 0 比较会触发拆箱 NPE
        if (type == null) {
            return R.error("评论类型不能为空");
        }

        //保存到评论的对象中
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
     * @param userId     用户id
     * @param type       评论类型（0歌曲1歌单）
     * @param songId     歌曲id
     * @param songListId 歌单列表id
     * @param content    评论内容
     * @return 评论结果
     */
    @RateLimit(key = "comment:update:", timeWindow = 60, maxRequests = 20, message = "评论修改过于频繁，请稍后再试")
    @PostMapping(value = "/update")
    public R updateComment(Integer id, Integer userId, Byte type, Integer songId, Integer songListId, String content) {
        //保存到评论的对象中
        Comment comment = new Comment();
        comment.setId(id);
        comment.setUserId(userId);
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
    public Boolean deleteComment(Integer id) {
        return commentService.delete(id);
    }

    /**
     * 通过主键查询评论
     *
     * @param id 评论主键
     * @return 评论
     */
    @GetMapping(value = "/selectByPrimaryKey")
    public Comment selectByPrimaryKey(Integer id) {
        return commentService.selectByPrimaryKey(id);
    }

    /**
     * 查询所有评论
     *
     * @return 所有评论
     */
    @GetMapping(value = "/allComment")
    public List<Comment> allComment() {
        return commentService.allComment();
    }

    /**
     * 查询某个歌曲下的所有评论
     *
     * @param songId 歌曲id
     * @return 评论列表
     */
    @GetMapping(value = "/commentOfSongId")
    public List<Comment> commentOfSongId(Integer songId) {
        return commentService.commentOfSongId(songId);
    }

    /**
     * 查询某个歌单下的所有评论
     *
     * @param songListId 歌单id
     * @return 评论列表
     */
    @GetMapping(value = "/commentOfSongListId")
    public List<Comment> commentOfSongListId(Integer songListId) {
        return commentService.commentOfSongListId(songListId);
    }

    /**
     * 给某个评论点赞
     *
     * @param id 评论id
     * @param up 点赞数
     * @return 结果
     */
    @RateLimit(key = "comment:like:", timeWindow = 60, maxRequests = 30, message = "点赞过于频繁，请稍后再试")
    @PostMapping(value = "/like")
    public R like(Integer id, Integer up) {
        //保存到评论的对象中
        Comment comment = new Comment();
        comment.setId(id);
        comment.setUp(up);

        //更新成功
        if (commentService.update(comment)) {
            return R.ok("点赞成功");
        }

        return R.error("点赞失败");
    }
    @GetMapping("/frequent-words")
    public R getFrequentWords(String songId) {

        log.info("id = " + songId);
        return R.ok("",commentService.getFrequentWords(songId));

    }
}

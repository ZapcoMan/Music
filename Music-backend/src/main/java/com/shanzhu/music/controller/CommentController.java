package com.shanzhu.music.controller;

import com.shanzhu.music.annotation.RateLimit;
import com.shanzhu.music.annotation.RequireUserType;
import com.shanzhu.music.common.R;
import com.shanzhu.music.entity.dto.CommentRequest;
import com.shanzhu.music.entity.po.Comment;
import com.shanzhu.music.service.CommentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 评论 控制层
 *
 * 本类的方法分散在三个资源域下：评论自身（/api/v1/comments）、评论挂在其下的歌曲
 * （/api/v1/songs/{songId}/...）与歌单（/api/v1/song-lists/{songListId}/comments）。
 * 因此不再声明类级 @RequestMapping，改为每个方法写绝对路径——
 * 硬套一个 /api/v1/comment 前缀只会让 URL 与资源归属说的不是一回事。
 * 角色域仍留在类级作兜底：方法级 @RequireUserType 优先级更高，不会被这里放宽。
 */
@Slf4j
@RestController
@RequireUserType({"consumer", "admin"})
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;

    /**
     * 添加评论
     *
     * 评论发出后即为不可变内容：本类没有改写接口，service 与持久层也没有更新正文的方法。
     * 原先挂在 POST /api/v1/comment/update 的入口（含"本人或管理员可改"的归属校验）已整体下线，
     * 因为产品口径是"一旦发布，连作者自己也不能修改"。要撤回就删了重发。
     *
     * @param type       评论类型（0歌曲1歌单）
     * @param songId     歌曲id
     * @param songListId 歌单id
     * @param content    评论内容
     * @return 添加结果
     */
    @RateLimit(key = "comment:add:", timeWindow = 60, maxRequests = 10, message = "评论发表过于频繁，请稍后再试")
    @PostMapping(value = "/api/v1/comments")
    @RequireUserType("consumer")
    public R<Void> addComment(@RequestAttribute("userId") Integer userId,
                              @Valid CommentRequest request) {
        // 身份只取令牌：原先 userId 由请求参数带入，任何人传别人的 id 就能替别人发评论。
        // 类型区间、正文非空与长度、type 与目标 id 的配对，都已在绑定阶段由
        // jakarta validation 完成；不通过时走 GlobalExceptionHandler 的
        // BindException 分支，返回 400 与具体原因，不再是"评论失败"这种无信息串。
        Comment comment = new Comment();
        comment.setUserId(userId);
        comment.setType(request.getType());
        comment.setContent(request.getContent());

        if (request.getType() == 0) {
            comment.setSongId(request.getSongId());
        } else {
            comment.setSongListId(request.getSongListId());
        }

        if (commentService.insert(comment)) {
            return R.ok("评论成功");
        }

        return R.error("评论失败");
    }

    /**
     * 删除评论
     *
     * @param id 评论id
     * @return 删除结果
     */
    @RateLimit(key = "comment:delete:", timeWindow = 60, maxRequests = 20, message = "评论删除过于频繁，请稍后再试")
    @DeleteMapping(value = "/api/v1/comments/{id}")
    @RequireUserType({"consumer", "admin"})
    public R<Boolean> deleteComment(@RequestAttribute("userId") Integer userId,
                                    @RequestAttribute("userType") String userType,
                                    @PathVariable("id") Integer id) {
        // 归属校验下沉到 service：原先 Controller 先查再比再删，
        // "不存在""无权""删除失败"三种情况全塌成同一个 false，调用方无从分辨；
        // 现在由 service 用 BusinessException 区分前两种，成功路径仍返回 data=true
        return R.ok("删除成功", commentService.deleteBy(id, userId, userType));
    }

    /**
     * 通过主键查询评论
     *
     * @param id 评论主键
     * @return 评论
     */
    @GetMapping(value = "/api/v1/comments/{id}")
    public R<Comment> selectByPrimaryKey(@PathVariable("id") Integer id) {
        return R.ok(commentService.selectByPrimaryKey(id));
    }

    /**
     * 查询所有评论
     *
     * @return 所有评论
     */
    @GetMapping(value = "/api/v1/comments")
    // 这条此前只要求"已登录"，任何消费者令牌都能一次拉走全站评论。评论关联用户昵称等内容，
    // 属于跨用户数据读取，不只是性能问题，故补 admin 限定。
    @RequireUserType("admin")
    public R<List<Comment>> allComment() {
        return R.ok(commentService.allComment());
    }

    /**
     * 查询某个歌曲下的所有评论
     *
     * 评论挂在歌曲之下，URL 用歌曲资源表达这个从属关系，songId 不再走查询串。
     * 本类因此不保留 /api/v1/comment 类前缀（见类注释）。
     *
     * @param songId 歌曲id
     * @return 评论列表
     */
    @GetMapping(value = "/api/v1/songs/{songId}/comments")
    public R<List<Comment>> commentOfSongId(@PathVariable("songId") Integer songId) {
        return R.ok(commentService.commentOfSongId(songId));
    }

    /**
     * 查询某个歌单下的所有评论
     *
     * @param songListId 歌单id
     * @return 评论列表
     */
    @GetMapping(value = "/api/v1/song-lists/{songListId}/comments")
    public R<List<Comment>> commentOfSongListId(@PathVariable("songListId") Integer songListId) {
        return R.ok(commentService.commentOfSongListId(songListId));
    }

    /**
     * 给某个评论点赞
     *
     * @param id 评论id
     * @return 结果
     */
    @RateLimit(key = "comment:like:", timeWindow = 60, maxRequests = 30, message = "点赞过于频繁，请稍后再试")
    @PostMapping(value = "/api/v1/comments/{id}/likes")
    @RequireUserType("consumer")
    public R<Void> like(@PathVariable("id") Integer id) {
        // up 原先直接取请求参数，客户端传多少就涨多少（一次请求即可刷到任意数值）。
        // 现按当前值自增，前端仍传 up 字段但服务端不再采纳；
        // 存在性校验、原子自增与缓存清理都在 service 里
        if (commentService.like(id)) {
            return R.ok("点赞成功");
        }
        return R.error("点赞失败");
    }
    /**
     * 查询某张歌单评论里的高频词
     *
     * 路径按歌单而不是歌曲表达，因为持久层这条统计的 SQL 就是
     * `where type = 1 and song_list_id = #{songListId}`：读的是歌单评论，
     * 旧参数名 songId 是历史误名，照它写成 /songs/{songId}/... 会让 URL 说假话。
     * 参数仍按 String 交给 service：那是持久层既有签名，本轮只做路径改造，
     * 不在这里顺手改参数类型。
     */
    @GetMapping("/api/v1/song-lists/{songListId}/frequent-words")
    public R<List<String>> getFrequentWords(@PathVariable("songListId") String songListId) {

        log.info("songListId = " + songListId);
        return R.ok("",commentService.getFrequentWords(songListId));

    }
}

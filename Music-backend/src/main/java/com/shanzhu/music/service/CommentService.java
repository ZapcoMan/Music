package com.shanzhu.music.service;

import com.shanzhu.music.entity.po.Comment;

import java.util.List;

/**
 * 评论 服务层
 *
 * 评论正文发出后不可变，含作者本人：本接口不提供改写正文的方法，
 * 持久层也只有 insert / delete / 点赞自增。要撤回就删了重发。
 */
public interface CommentService {

    /**
     * 添加评论
     *
     * @param comment 评论内容
     * @return 添加结果
     */
    Boolean insert(Comment comment);

    /**
     * 给评论点赞（按当前值原子自增，不接受客户端给定的数值）
     *
     * @param id 评论id
     * @return 自增结果
     */
    Boolean like(Integer id);

    /**
     * 删除评论（校验归属）
     *
     * @param id           评论id
     * @param operatorId   令牌中的用户id
     * @param operatorType 令牌中的角色
     * @return 删除结果
     */
    Boolean deleteBy(Integer id, Integer operatorId, String operatorType);

    /**
     * 删除评论
     *
     * @param id 评论id
     * @return 删除结果
     */
    Boolean delete(Integer id);

    /**
     * 通过主键查询评论
     *
     * @param id 评论主键
     * @return 评论
     */
    Comment selectByPrimaryKey(Integer id);

    /**
     * 查询所有评论
     *
     * @return 所有评论
     */
    List<Comment> allComment();

    /**
     * 查询某个歌曲下的所有评论
     *
     * @param songId 歌曲id
     * @return 评论列表
     */
    List<Comment> commentOfSongId(Integer songId);

    /**
     * 查询某个歌单下的所有评论
     *
     * @param songListId 歌单id
     * @return 评论列表
     */
    List<Comment> commentOfSongListId(Integer songListId);




    public List<String> getAllComments();
    public List<String> getFrequentWords(String id);
}

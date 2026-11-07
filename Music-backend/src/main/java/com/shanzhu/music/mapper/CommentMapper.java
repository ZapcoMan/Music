package com.shanzhu.music.mapper;

import com.shanzhu.music.entity.po.Comment;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 评论 持久层
 */
@Mapper
public interface CommentMapper {

    /**
     * 添加评论
     *
     * @param comment 评论内容
     * @return 添加结果
     */
    Integer insert(Comment comment);

    /**
     * 修改评论
     *
     * @param comment 评论内容
     * @return 修改结果
     */
    Integer update(Comment comment);

    /**
     * 删除评论
     *
     * @param id 评论id
     * @return 删除结果
     */
    Integer delete(Integer id);

    /**
     * 删除某首歌曲下的全部评论（删除歌曲时级联清理）
     *
     * @param songId 歌曲id
     * @return 影响行数
     */
    Integer deleteBySongId(@Param("songId") Integer songId);

    /**
     * 删除某歌单下的全部评论（删除歌单时级联清理）
     *
     * @param songListId 歌单id
     * @return 影响行数
     */
    Integer deleteBySongListId(@Param("songListId") Integer songListId);

    /**
     * 删除某用户发布的全部评论（删除用户时级联清理）
     *
     * @param userId 用户id
     * @return 影响行数
     */
    Integer deleteByUserId(@Param("userId") Integer userId);

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


    List<String> selectAllCommentsBySongListId(String songListId);

    List<String> selectAllComments();
}


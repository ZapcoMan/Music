package com.shanzhu.music.service;

import com.shanzhu.music.entity.po.Collect;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 收藏 服务层
 */
public interface CollectService {

    /**
     * 添加收藏
     *
     * @param collect 收藏信息
     * @return 结果
     */
    Boolean insert(Collect collect);

    /**
     * 删除收藏
     *
     * @param id 收藏id
     * @return 结果
     */
    Boolean delete(Integer id);

    /**
     * 删除收藏
     *
     * @param userId 用户id
     * @param songId 歌曲id
     * @return 删除结果
     */
    Boolean deleteByUserIdSongId(Integer userId, Integer songId);

    /**
     * 查询所有收藏
     *
     * @return 收藏列表
     */
    List<Collect> allCollect();

    /**
     * 查询某个用户的收藏列表
     *
     * @param userId 用户id
     * @return 收藏列表
     */
    List<Collect> collectOfUserId(Integer userId);

    /**
     * 查询某个用户是否已经收藏了某个歌曲
     *
     * @param userId 用户id
     * @param songId 歌曲id
     * @return 是否收藏
     */
    Boolean existSongId(@Param("userId") Integer userId, @Param("songId") Integer songId);

    /**
     * 判断当前用户是否已收藏某歌单
     *
     * @param userId     用户ID
     * @param songListId 歌单ID
     * @return 是否存在
     */
    Boolean existSongListId(Integer userId, Integer songListId);

    /**
     * 查询当前用户收藏的歌单
     *
     * @param userId 用户ID
     * @return 歌单列表
     */
    List<com.shanzhu.music.entity.po.SongList> collectSongListOfUserId(Integer userId);

    /**
     * 取消某歌单收藏
     *
     * @param userId     用户ID
     * @param songListId 歌单ID
     * @return 结果
     */
    Boolean deleteByUserIdSongListId(Integer userId, Integer songListId);
}

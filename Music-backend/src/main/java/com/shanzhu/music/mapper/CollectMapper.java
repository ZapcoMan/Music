package com.shanzhu.music.mapper;

import com.shanzhu.music.entity.po.Collect;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 收藏 持久层
 */
@Mapper
public interface CollectMapper {

    /**
     * 添加收藏
     *
     * @param collect 收藏信息
     * @return 结果
     */
    Integer insert(Collect collect);

    /**
     * 删除收藏
     *
     * @param id id
     * @return 结果
     */
    Integer delete(Integer id);

    /**
     * 删除收藏
     *
     * @param userId 用户id
     * @param songId 歌曲id
     * @return 删除结果
     */
    Integer deleteByUserIdSongId(@Param("userId") Integer userId, @Param("songId") Integer songId);

    /**
     * 删除指向某首歌曲的全部收藏（删除歌曲时级联清理，避免残留无效收藏）
     *
     * @param songId 歌曲id
     * @return 影响行数
     */
    Integer deleteBySongId(@Param("songId") Integer songId);

    /**
     * 按歌手批量清理收藏记录（N+1 消除）
     */
    Integer deleteBySingerId(@Param("singerId") Integer singerId);

    /**
     * 删除某用户的全部收藏（删除用户时级联清理）
     *
     * @param userId 用户id
     * @return 影响行数
     */
    Integer deleteByUserId(@Param("userId") Integer userId);

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
    Integer existSongId(@Param("userId") Integer userId, @Param("songId") Integer songId);

    /**
     * 查询用户收藏的歌曲ID列表（用于去重）
     *
     * @param userId 用户id
     * @return 歌曲ID列表
     */
    List<Integer> selectSongIdsByUserId(@Param("userId") Integer userId);

    /**
     * 根据用户ID查询收藏的歌曲列表（返回完整的Song对象）
     *
     * @param userId 用户id
     * @return 歌曲列表
     */
    List<com.shanzhu.music.entity.po.Song> selectByUserId(@Param("userId") Integer userId);

    /**
     * 统计歌曲被收藏的次数
     *
     * @param songId 歌曲id
     * @return 收藏次数
     */
    Integer countCollectsBySongId(@Param("songId") Integer songId);

    /**
     * 判断用户是否已收藏某歌单
     *
     * @param userId     用户ID
     * @param songListId 歌单ID
     * @return 记录数
     */
    Integer existSongListId(@Param("userId") Integer userId, @Param("songListId") Integer songListId);

    /**
     * 查询用户收藏的歌单
     *
     * @param userId 用户ID
     * @return 歌单列表
     */
    List<com.shanzhu.music.entity.po.SongList> selectSongListsByUserId(@Param("userId") Integer userId);

    /**
     * 按用户与歌单删除收藏
     *
     * @param userId     用户ID
     * @param songListId 歌单ID
     * @return 删除条数
     */
    Integer deleteByUserIdSongListId(@Param("userId") Integer userId, @Param("songListId") Integer songListId);

    /**
     * 批量统计多首歌曲被收藏的次数，用于替代逐首查询
     *
     * @param songIds 歌曲id列表，调用方需保证非空
     * @return 每项含 songId / collectCount
     */
    List<java.util.Map<String, Object>> countCollectsBySongIds(@Param("songIds") List<Integer> songIds);

    /**
     * 统计歌手的所有歌曲被某用户收藏的总次数
     *
     * @param singerId 歌手id
     * @return 收藏次数
     */
    Integer countCollectsBySingerId(@Param("singerId") Integer singerId);

}

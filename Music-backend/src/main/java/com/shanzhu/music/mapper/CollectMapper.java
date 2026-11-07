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

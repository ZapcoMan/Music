package com.shanzhu.music.mapper;

import com.shanzhu.music.entity.po.ListSong;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 歌单里面歌曲 持久层
 */
@Mapper
public interface ListSongMapper {

    /**
     * 给歌单添加歌曲
     *
     * @param listSong 歌单信息
     * @return 添加结果
     */
    Integer insert(ListSong listSong);

    /**
     * 修改歌单里面歌曲
     *
     * @param listSong 歌单里面歌曲信息
     * @return 结果
     */
    Integer update(ListSong listSong);

    /**
     * 给歌单删除歌曲
     *
     * @param id 歌曲id
     * @return 结果
     */
    Integer delete(Integer id);

    /**
     * 删除歌单里的歌曲
     *
     * @param songId     歌曲id
     * @param songListId 歌单id
     * @return 删除结果
     */
    Integer deleteBySongIdAndSongListId(Integer songId, Integer songListId);

    /**
     * 删除某歌单包含的全部歌曲关联（删除歌单时级联清理）
     *
     * @param songListId 歌单id
     * @return 影响行数
     */
    Integer deleteBySongListId(@Param("songListId") Integer songListId);

    /**
     * 删除某歌曲在各歌单中的全部关联（删除歌曲时级联清理）
     *
     * @param songId 歌曲id
     * @return 影响行数
     */
    Integer deleteBySongId(@Param("songId") Integer songId);

    /**
     * 按歌手批量清理歌单关联：删除整位歌手时避免逐首歌建键删除（N+1）
     */
    Integer deleteBySingerId(@Param("singerId") Integer singerId);

    /**
     * 根据主键查询歌单里面歌曲
     *
     * @param id 歌曲id
     * @return 歌曲列表
     */
    ListSong selectByPrimaryKey(Integer id);

    /**
     * 根据歌单id查询歌曲
     *
     * @param songListId 歌单id
     * @return 歌曲列表
     */
    List<ListSong> listSongOfSongListId(Integer songListId);

}

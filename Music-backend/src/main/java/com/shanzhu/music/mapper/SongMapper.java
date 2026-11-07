package com.shanzhu.music.mapper;

import com.shanzhu.music.entity.po.Song;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 歌曲 持久层
 */
@Mapper
public interface SongMapper {

    /**
     * 添加歌曲
     *
     * @param song 歌曲信息
     * @return 结果
     */
    Integer insert(Song song);

    /**
     * 更新歌曲
     *
     * @param song 歌曲信息
     * @return 结果
     */
    Integer update(Song song);

    /**
     * 删除歌曲
     *
     * @param id 歌曲id
     * @return 删除结果
     */
    Integer delete(Integer id);

    /**
     * 根据歌曲id查询歌曲对象
     *
     * @param id 歌曲id
     * @return 歌曲
     */
    Song selectByPrimaryKey(Integer id);

    /**
     * 查询所有歌曲
     *
     * @return 歌曲列表
     */
    List<Song> allSong();

    /**
     * 带安全上限的全量歌曲查询，供不经分页的全量视图使用
     */
    List<Song> allSongLimited();

    /**
     * 根据歌手名字精确查询歌曲
     *
     * @param name 歌手名
     * @return 歌曲列表
     */
    List<Song> songOfName(String name);

    /**
     * 根据歌手名字模糊查询歌曲
     *
     * @param name 歌手名称
     * @return 歌曲列表
     */
    List<Song> likeSongOfName(String name);

    /**
     * 根据歌手id查询歌曲
     *
     * @param singerId 歌手id
     * @return 歌曲列表
     */
    List<Song> songOfSingerId(Integer singerId);

    /**
     * 召回专用：按歌手取候选歌曲，不返回 lyric 正文
     *
     * @param singerId 歌手id
     * @return 歌曲列表
     */
    List<Song> songOfSingerIdForRecall(Integer singerId);

    /**
     * 召回专用：按歌名模糊取候选歌曲，不返回 lyric 正文
     *
     * @param name 歌名模糊串
     * @return 歌曲列表
     */
    List<Song> likeSongOfNameForRecall(String name);

    /**
     * 搜索历史召回：按歌单名称模糊匹配，取歌单内的歌曲
     *
     * @param title 歌单名称（含通配符）
     * @return 歌曲列表，只含召回所需列
     */
    List<Song> likeSongOfListTitleForRecall(@Param("title") String title);

    /**
     * 搜索历史召回：按专辑名（song.introduction）模糊匹配
     *
     * @param introduction 专辑名（含通配符）
     * @return 歌曲列表，只含召回所需列
     */
    List<Song> likeSongOfIntroductionForRecall(@Param("introduction") String introduction);



    List<Song> getSongsByUserIdNotInOtherUser(@Param("userId") int userId, @Param("otherUserId") int otherUserId);
    List<Song> getCollectByUserID(@Param("userId") int userId);

    /**
     * 获取用户已播放的歌曲ID列表（去重用）
     */
    List<Integer> getPlayedSongIdsByUserId(@Param("userId") int userId);

    /**
     * 查询热门歌曲（按播放量排序）
     *
     * 相邻的 getSongsBySimilarTags 与 getSongsByStyle 已删除：两者都用 ORDER BY RAND()，
     * 且全仓（含测试）没有任何 Java 调用点，属于只会随数据量增长变慢、却永远不会被发现的
     * 死代码。getHotSongs 有 PopularRecall 调用，保留。
     */
    List<Song> getHotSongs(@Param("limit") int limit);

    /**
     * 批量查询歌曲（解决 N+1 问题）
     *
     * @param songIds 歌曲ID列表
     * @return 歌曲列表
     */
    List<Song> selectBatchByIds(@Param("songIds") List<Integer> songIds);
}

package com.shanzhu.music.service;

import com.github.pagehelper.PageInfo;
import com.shanzhu.music.entity.po.Song;

import java.util.List;

/**
 * 歌曲 服务层
 */
public interface SongService {

    /**
     * 添加歌曲
     *
     * @param song 歌曲信息
     * @return 结果
     */
    Boolean insert(Song song);

    /**
     * 修改歌曲
     *
     * @param song 歌曲信息
     * @return 删除结果
     */
    Boolean update(Song song);

    /**
     * 删除歌曲
     *
     * @param id 歌曲id
     * @return 删除结果
     */
    Boolean delete(Integer id);

    /**
     * 按歌手批量删除其全部歌曲及所有引用（供删除歌手做级联）
     *
     * @param singerId 歌手id
     * @return 被删除的歌曲数量
     */
    Integer deleteBySingerId(Integer singerId);

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
     * 分页查询所有歌曲
     *
     * @param pageNum 页码
     * @param pageSize 每页大小
     * @return 分页结果
     */
    PageInfo<Song> allSongByPage(int pageNum, int pageSize);
}

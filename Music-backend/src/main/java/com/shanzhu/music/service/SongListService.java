package com.shanzhu.music.service;

import com.github.pagehelper.PageInfo;
import com.shanzhu.music.entity.po.Song;
import com.shanzhu.music.entity.po.SongList;

import java.util.List;


/**
 * 歌单 服务层
 */
public interface SongListService {

    /**
     * 增加歌单
     *
     * @param songList 歌单信息
     * @return 结果
     */
    Boolean insert(SongList songList);

    /**
     * 修改歌单
     *
     * @param songList 歌单信息
     * @return 结果
     */
    Boolean update(SongList songList);

    /**
     * 删除歌单
     *
     * @param id 歌单id
     * @return 删除结果
     */
    Boolean delete(Integer id);

    /**
     * 查询歌单
     *
     * @param id 歌单id
     * @return 歌单
     */
    SongList selectByPrimaryKey(Integer id);

    /**
     * 查询所有歌单
     *
     * @return 歌单列表
     */
    List<SongList> allSongList();

    /**
     * 根据标题精确查询歌单列表
     *
     * @param title 标题
     * @return 歌单列表
     */
    List<SongList> songListOfTitle(String title);

    /**
     * 根据标题模糊查询歌单列表
     *
     * @param title 标题
     * @return 歌单列表
     */
    List<SongList> likeTitle(String title);

    /**
     * 根据风格模糊查询歌单列表
     *
     * @param style 歌单风格
     * @return 歌单列表
     */
    List<SongList> likeStyle(String style);

     List<Song> queryRecommendedSongById(String id);
    
    /**
     * 分页查询所有歌单
     *
     * @param pageNum 页码
     * @param pageSize 每页大小
     * @return 分页结果
     */
    PageInfo<SongList> allSongListByPage(int pageNum, int pageSize);
}

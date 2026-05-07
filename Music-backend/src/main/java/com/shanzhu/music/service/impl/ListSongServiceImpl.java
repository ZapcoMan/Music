package com.shanzhu.music.service.impl;

import com.shanzhu.music.entity.po.ListSong;
import com.shanzhu.music.mapper.ListSongMapper;
import com.shanzhu.music.service.ListSongService;
import com.shanzhu.music.utils.RedisUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 歌单里面歌曲 服务层实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ListSongServiceImpl implements ListSongService {

    private final ListSongMapper listSongMapper;
    private final RedisUtils redisUtils;
    
    // Redis缓存键前缀
    private static final String LIST_SONG_CACHE_KEY = "music:list_song:";
    private static final long CACHE_EXPIRE_HOURS = 24; // 缓存过期时间24小时

    /**
     * 给歌单添加歌曲（清除缓存）
     *
     * @param listSong 歌单信息
     * @return 添加结果
     */
    @Override
    public Boolean insert(ListSong listSong) {
        Boolean result = listSongMapper.insert(listSong) > 0;
        if (result) {
            redisUtils.deleteObject(LIST_SONG_CACHE_KEY + listSong.getSongListId());
            log.info("歌单添加歌曲，清除缓存，songListId={}", listSong.getSongListId());
        }
        return result;
    }

    /**
     * 修改歌单里歌曲（清除缓存）
     *
     * @param listSong 歌单里歌曲
     * @return 结果
     */
    @Override
    public Boolean update(ListSong listSong) {
        Boolean result = listSongMapper.update(listSong) > 0;
        if (result) {
            redisUtils.deleteObject(LIST_SONG_CACHE_KEY + listSong.getSongListId());
            log.info("修改歌单歌曲，清除缓存，songListId={}", listSong.getSongListId());
        }
        return result;
    }

    /**
     * 删除歌单里歌曲（清除缓存）
     *
     * @param id 歌曲id
     * @return 结果
     */
    @Override
    public Boolean delete(Integer id) {
        ListSong listSong = listSongMapper.selectByPrimaryKey(id);
        Boolean result = listSongMapper.delete(id) > 0;
        if (result && listSong != null) {
            redisUtils.deleteObject(LIST_SONG_CACHE_KEY + listSong.getSongListId());
            log.info("删除歌单歌曲，清除缓存，songListId={}", listSong.getSongListId());
        }
        return result;
    }

    /**
     * 删除歌单里的歌曲（清除缓存）
     *
     * @param songId     歌曲id
     * @param songListId 歌单id
     * @return 删除结果
     */
    @Override
    public Boolean deleteBySongIdAndSongListId(Integer songId, Integer songListId) {
        Boolean result = listSongMapper.deleteBySongIdAndSongListId(songId, songListId) > 0;
        if (result) {
            redisUtils.deleteObject(LIST_SONG_CACHE_KEY + songListId);
            log.info("删除歌单歌曲，清除缓存，songListId={}", songListId);
        }
        return result;
    }

    /**
     * 查询歌单里的歌曲
     *
     * @param id 歌曲id
     * @return 结果
     */
    @Override
    public ListSong selectByPrimaryKey(Integer id) {
        return listSongMapper.selectByPrimaryKey(id);
    }

    /**
     * 根据歌单id查询歌曲（带缓存）
     *
     * @param songListId 歌单id
     * @return 歌曲列表
     */
    @Override
    public List<ListSong> listSongOfSongListId(Integer songListId) {
        String cacheKey = LIST_SONG_CACHE_KEY + songListId;
        
        Object cachedData = redisUtils.getCacheObject(cacheKey);
        if (cachedData != null) {
            log.info("从Redis缓存获取歌单歌曲列表，songListId={}", songListId);
            return (List<ListSong>) cachedData;
        }
        
        log.info("从数据库查询歌单歌曲列表，songListId={}", songListId);
        List<ListSong> listSongs = listSongMapper.listSongOfSongListId(songListId);
        
        redisUtils.setCacheObject(cacheKey, listSongs, CACHE_EXPIRE_HOURS, TimeUnit.HOURS);
        
        return listSongs;
    }

}

package com.shanzhu.music.service.impl;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.shanzhu.music.entity.po.Song;
import com.shanzhu.music.entity.po.SongList;
import com.shanzhu.music.mapper.SongListMapper;
import com.shanzhu.music.mapper.SongMapper;
import com.shanzhu.music.service.SongListService;
import com.shanzhu.music.utils.RedisUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 歌单 服务层实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SongListServiceImpl implements SongListService {

    private final SongListMapper songListMapper;
    private final SongMapper songMapper;
    private final RedisUtils redisUtils;
    
    // Redis缓存键前缀
    private static final String SONG_LIST_CACHE_KEY = "music:songlist:";
    private static final String ALL_SONG_LIST_KEY = SONG_LIST_CACHE_KEY + "all";
    private static final long CACHE_EXPIRE_HOURS = 24; // 缓存过期时间24小时

    /**
     * 增加歌单（清除缓存）
     *
     * @param songList 歌单信息
     * @return 结果
     */
    @Override
    public Boolean insert(SongList songList) {
        Boolean result = songListMapper.insert(songList) > 0;
        // 插入成功后清除缓存
        if (result) {
            redisUtils.deleteObject(ALL_SONG_LIST_KEY);
            log.info("新增歌单，清除缓存");
        }
        return result;
    }

    /**
     * 修改歌单（清除缓存）
     *
     * @param songList 歌单信息
     * @return 结果
     */
    @Override
    public Boolean update(SongList songList) {
        Boolean result = songListMapper.update(songList) > 0;
        // 更新成功后清除缓存
        if (result) {
            redisUtils.deleteObject(ALL_SONG_LIST_KEY);
            // 清除单个歌单缓存
            redisUtils.deleteObject(SONG_LIST_CACHE_KEY + songList.getId());
            log.info("更新歌单，清除缓存");
        }
        return result;
    }

    /**
     * 删除歌单（清除缓存）
     *
     * @param id 歌单id
     * @return 删除结果
     */
    @Override
    public Boolean delete(Integer id) {
        Boolean result = songListMapper.delete(id) > 0;
        // 删除成功后清除缓存
        if (result) {
            redisUtils.deleteObject(ALL_SONG_LIST_KEY);
            redisUtils.deleteObject(SONG_LIST_CACHE_KEY + id);
            log.info("删除歌单，清除缓存");
        }
        return result;
    }

    /**
     * 查询歌单（带缓存）
     *
     * @param id 歌单id
     * @return 歌单
     */
    @Override
    public SongList selectByPrimaryKey(Integer id) {
        String cacheKey = SONG_LIST_CACHE_KEY + id;
        
        // 先从Redis缓存中获取
        Object cachedData = redisUtils.getCacheObject(cacheKey);
        if (cachedData != null) {
            log.info("从Redis缓存获取歌单数据，id={}", id);
            return (SongList) cachedData;
        }
        
        // 缓存未命中，从数据库查询
        log.info("从数据库查询歌单数据，id={}", id);
        SongList songList = songListMapper.selectByPrimaryKey(id);
        
        // 存入Redis缓存，设置24小时过期
        if (songList != null) {
            redisUtils.setCacheObject(cacheKey, songList, CACHE_EXPIRE_HOURS, TimeUnit.HOURS);
        }
        
        return songList;
    }

    /**
     * 查询所有歌单（带缓存）
     *
     * @return 歌单列表
     */
    @Override
    public List<SongList> allSongList() {
        // 先从Redis缓存中获取
        Object cachedData = redisUtils.getCacheObject(ALL_SONG_LIST_KEY);
        if (cachedData != null) {
            log.info("从Redis缓存获取所有歌单数据");
            return (List<SongList>) cachedData;
        }
        
        // 缓存未命中，从数据库查询
        log.info("从数据库查询所有歌单数据");
        List<SongList> songList = songListMapper.allSongList();
        
        // 存入Redis缓存，设置24小时过期
        redisUtils.setCacheObject(ALL_SONG_LIST_KEY, songList, CACHE_EXPIRE_HOURS, TimeUnit.HOURS);
        
        return songList;
    }

    /**
     * 根据标题精确查询歌单列表
     *
     * @param title 标题
     * @return 歌单列表
     */
    @Override
    public List<SongList> songListOfTitle(String title) {
        return songListMapper.songListOfTitle(title);
    }

    /**
     * 根据标题模糊查询歌单列表
     *
     * @param title 标题
     * @return 歌单列表
     */
    @Override
    public List<SongList> likeTitle(String title) {
        return songListMapper.likeTitle(title);
    }

    /**
     * 根据风格模糊查询歌单列表
     *
     * @param style 歌单风格
     * @return 歌单列表
     */
    @Override
    public List<SongList> likeStyle(String style) {
        return songListMapper.likeStyle(style);
    }

    @Override
    public List<Song> queryRecommendedSongById(String id) {
        int ids = Integer.parseInt(id);
        log.info("ids : {}",ids);
        List<Integer> similarUserIds = songListMapper.findSimilarUsers(ids);
        log.info("similarUserIds = {}", similarUserIds.toString());
        List<Song> songsByUserIdNotInOtherUser = new ArrayList<>();

        for (Integer similarUserId : similarUserIds) {
             songsByUserIdNotInOtherUser = songMapper.getSongsByUserIdNotInOtherUser(ids, similarUserId);
        }

        return songsByUserIdNotInOtherUser;
    }
    
    /**
     * 分页查询所有歌单
     *
     * @param pageNum 页码
     * @param pageSize 每页大小
     * @return 分页结果
     */
    @Override
    public PageInfo<SongList> allSongListByPage(int pageNum, int pageSize) {
        // 开启分页
        PageHelper.startPage(pageNum, pageSize);
        // 执行查询
        List<SongList> list = songListMapper.allSongList();
        // 封装分页结果
        return new PageInfo<>(list);
    }

}

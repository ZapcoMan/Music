package com.shanzhu.music.service.impl;
import com.shanzhu.music.common.CacheKeys;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.shanzhu.music.entity.po.Song;
import com.shanzhu.music.mapper.CollectMapper;
import com.shanzhu.music.mapper.CommentMapper;
import com.shanzhu.music.mapper.ListSongMapper;
import com.shanzhu.music.mapper.SongMapper;
import com.shanzhu.music.service.SongService;
import com.shanzhu.music.utils.RedisUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 歌曲 服务层实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SongServiceImpl implements SongService {

    private final SongMapper songMapper;
    private final ListSongMapper listSongMapper;
    private final CollectMapper collectMapper;
    private final CommentMapper commentMapper;
    private final RedisUtils redisUtils;
    
    // Redis缓存键前缀
    private static final String SONG_CACHE_KEY = CacheKeys.SONG;
    private static final String ALL_SONG_KEY = SONG_CACHE_KEY + "all";
    private static final String SONG_BY_SINGER_KEY = SONG_CACHE_KEY + "singer:";
    private static final long CACHE_EXPIRE_HOURS = 24; // 缓存过期时间24小时

    /**
     * 添加歌曲（延时双删缓存）
     *
     * @param song 歌曲信息
     * @return 结果
     */
    @Override
    public Boolean insert(Song song) {
        Boolean result = songMapper.insert(song) > 0;
        if (result) {
            // 使用延时双删策略
            redisUtils.deleteWithDelay(ALL_SONG_KEY);
            evictSingerCache(song.getSingerId(), song.getId());
            log.info("新增歌曲，延时双删缓存");
        }
        return result;
    }

    /**
     * 更新歌曲（延时双删缓存）
     *
     * @param song 歌曲信息
     * @return 结果
     */
    @Override
    public Boolean update(Song song) {
        Boolean result = songMapper.update(song) > 0;
        if (result) {
            // 使用延时双删策略
            redisUtils.deleteWithDelay(ALL_SONG_KEY);
            redisUtils.deleteWithDelay(SONG_CACHE_KEY + song.getId());
            evictSingerCache(song.getSingerId(), song.getId());
            log.info("更新歌曲，延时双删缓存");
        }
        return result;
    }

    /**
     * 删除歌曲（延时双删缓存）
     *
     * @param id 歌曲id
     * @return 删除结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean delete(Integer id) {
        Song song = songMapper.selectByPrimaryKey(id);
        Boolean result = songMapper.delete(id) > 0;
        if (result) {
            // 级联清理引用该歌曲的关联数据。残留会让歌单列表显示不存在的歌曲、
            // 热门榜与协同过滤继续统计一首已删除的歌
            listSongMapper.deleteBySongId(id);
            collectMapper.deleteBySongId(id);
            commentMapper.deleteBySongId(id);
            log.info("删除歌曲 {}，已级联清理歌单关联、收藏与评论", id);
        }
        if (result && song != null) {
            // 使用延时双删策略
            redisUtils.deleteWithDelay(ALL_SONG_KEY);
            redisUtils.deleteWithDelay(SONG_CACHE_KEY + id);
            evictSingerCache(song.getSingerId(), song.getId());
            log.info("删除歌曲，延时双删缓存");
        }
        return result;
    }

    /**
     * 失效歌手维度的歌曲缓存。
     * Controller 组装的 Song 常常不带 singerId，直接拼接会得到
     * music:song:singer:null，真正的歌手维度缓存从未被清理，只能等 24 小时自然过期；
     * 这里在 singerId 缺失时按歌曲 id 回查一次，取不到则明确告警而不是静默拼出无效键。
     */
    private void evictSingerCache(Integer singerId, Integer songId) {
        Integer resolvedSingerId = singerId;
        if (resolvedSingerId == null && songId != null) {
            Song persisted = songMapper.selectByPrimaryKey(songId);
            if (persisted != null) {
                resolvedSingerId = persisted.getSingerId();
            }
        }
        if (resolvedSingerId == null) {
            log.warn("歌曲 {} 无法确定所属歌手，跳过歌手维度缓存失效", songId);
            return;
        }
        redisUtils.deleteWithDelay(SONG_BY_SINGER_KEY + resolvedSingerId);
    }

    /**
     * 根据歌曲id查询歌曲对象（带缓存）
     *
     * @param id 歌曲id
     * @return 歌曲
     */
    @Override
    public Song selectByPrimaryKey(Integer id) {
        String cacheKey = SONG_CACHE_KEY + id;
        
        Object cachedData = redisUtils.getCacheObject(cacheKey);
        if (cachedData != null) {
            log.info("从Redis缓存获取歌曲数据，id={}", id);
            return (Song) cachedData;
        }
        
        log.info("从数据库查询歌曲数据，id={}", id);
        Song song = songMapper.selectByPrimaryKey(id);
        
        if (song != null) {
            redisUtils.setCacheObject(cacheKey, song, CACHE_EXPIRE_HOURS, TimeUnit.HOURS);
        }
        
        return song;
    }

    /**
     * 查询所有歌曲（带缓存）
     *
     * @return 歌曲列表
     */
    @Override
    public List<Song> allSong() {
        Object cachedData = redisUtils.getCacheObject(ALL_SONG_KEY);
        if (cachedData != null) {
            log.info("从Redis缓存获取所有歌曲数据");
            return (List<Song>) cachedData;
        }
        
        log.info("从数据库查询所有歌曲数据");
        List<Song> songs = songMapper.allSong();
        
        redisUtils.setCacheObject(ALL_SONG_KEY, songs, CACHE_EXPIRE_HOURS, TimeUnit.HOURS);
        
        return songs;
    }

    /**
     * 根据歌手名字精确查询歌曲
     *
     * @param name 歌手名
     * @return 歌曲列表
     */
    @Override
    public List<Song> songOfName(String name) {
        return songMapper.songOfName(name);
    }

    /**
     * 根据歌手名字模糊查询歌曲
     *
     * @param name 歌手名称
     * @return 歌曲列表
     */
    @Override
    public List<Song> likeSongOfName(String name) {
        return songMapper.likeSongOfName("%" + name + "%");
    }


    /**
     * 根据歌手id查询歌曲（带缓存）
     *
     * @param singerId 歌手id
     * @return 歌曲列表
     */
    @Override
    public List<Song> songOfSingerId(Integer singerId) {
        String cacheKey = SONG_BY_SINGER_KEY + singerId;
        
        Object cachedData = redisUtils.getCacheObject(cacheKey);
        if (cachedData != null) {
            log.info("从Redis缓存获取歌手歌曲列表，singerId={}", singerId);
            return (List<Song>) cachedData;
        }
        
        log.info("从数据库查询歌手歌曲列表，singerId={}", singerId);
        List<Song> songs = songMapper.songOfSingerId(singerId);
        
        redisUtils.setCacheObject(cacheKey, songs, CACHE_EXPIRE_HOURS, TimeUnit.HOURS);
        
        return songs;
    }
    
    /**
     * 分页查询所有歌曲
     *
     * @param pageNum 页码
     * @param pageSize 每页大小
     * @return 分页结果
     */
    @Override
    public PageInfo<Song> allSongByPage(int pageNum, int pageSize) {
        // 开启分页
        PageHelper.startPage(pageNum, pageSize);
        // 执行查询
        List<Song> list = songMapper.allSong();
        // 封装分页结果
        return new PageInfo<>(list);
    }

}

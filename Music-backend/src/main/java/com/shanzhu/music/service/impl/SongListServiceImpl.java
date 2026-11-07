package com.shanzhu.music.service.impl;
import com.shanzhu.music.common.CacheKeys;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.shanzhu.music.entity.po.Song;
import com.shanzhu.music.entity.po.SongList;
import com.shanzhu.music.mapper.SongListMapper;
import com.shanzhu.music.service.SongListService;
import com.shanzhu.music.service.ranking.RankingService;
import com.shanzhu.music.utils.ParseUtils;
import com.shanzhu.music.utils.RedisUtils;
import lombok.RequiredArgsConstructor;
import com.shanzhu.music.service.ranking.RecommendationCache;
import lombok.extern.slf4j.Slf4j;
import com.shanzhu.music.mapper.CommentMapper;
import com.shanzhu.music.mapper.ListSongMapper;
import com.shanzhu.music.mapper.RankMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 歌单 服务层实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SongListServiceImpl implements SongListService {

    private final SongListMapper songListMapper;
    private final ListSongMapper listSongMapper;
    private final CommentMapper commentMapper;
    private final RankMapper rankMapper;
    private final RedisUtils redisUtils;
    private final RankingService rankingService;
    private final RecommendationCache recommendationCache;
    
    // Redis缓存键前缀
    private static final String SONG_LIST_CACHE_KEY = CacheKeys.SONG_LIST;
    private static final String ALL_SONG_LIST_KEY = SONG_LIST_CACHE_KEY + "all";
    private static final long CACHE_EXPIRE_HOURS = 24; // 缓存过期时间24小时

    /**
     * 增加歌单（延时双删缓存）
     *
     * @param songList 歌单信息
     * @return 结果
     */
    @Override
    public Boolean insert(SongList songList) {
        Boolean result = songListMapper.insert(songList) > 0;
        // 插入成功后使用延时双删清除缓存
        if (result) {
            redisUtils.deleteWithDelay(ALL_SONG_LIST_KEY);
            log.info("新增歌单，延时双删缓存");
        }
        return result;
    }

    /**
     * 修改歌单（延时双删缓存）
     *
     * @param songList 歌单信息
     * @return 结果
     */
    @Override
    public Boolean update(SongList songList) {
        Boolean result = songListMapper.update(songList) > 0;
        // 更新成功后使用延时双删清除缓存
        if (result) {
            redisUtils.deleteWithDelay(ALL_SONG_LIST_KEY);
            // 清除单个歌单缓存
            redisUtils.deleteWithDelay(SONG_LIST_CACHE_KEY + songList.getId());
            log.info("更新歌单，延时双删缓存");
        }
        return result;
    }

    /**
     * 删除歌单（延时双删缓存）
     *
     * @param id 歌单id
     * @return 删除结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean delete(Integer id) {
        Boolean result = songListMapper.delete(id) > 0;
        if (result) {
            // 级联清理：残留关联会让歌单里出现不存在的歌曲、评论挂在已消失的歌单上，
            // 而 ranks 中的历史评分仍会参与协同过滤相似度与均值统计
            listSongMapper.deleteBySongListId(id);
            commentMapper.deleteBySongListId(id);
            rankMapper.deleteBySongListId(id);
            log.info("删除歌单 {}，已级联清理关联歌曲、评论与评分", id);
        }
        // 删除成功后使用延时双删清除缓存
        if (result) {
            redisUtils.deleteWithDelay(ALL_SONG_LIST_KEY);
            redisUtils.deleteWithDelay(SONG_LIST_CACHE_KEY + id);
            log.info("删除歌单，延时双删缓存");
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
        Integer userId = ParseUtils.toIntegerOrNull(id);
        if (userId == null) {
            log.warn("推荐请求的用户ID不合法，返回空列表，id={}", id);
            return List.of();
        }

        // 1. 命中缓存直接返回；空列表代表"已确认暂无结果"，不再重跑全链路
        List<Song> cached = recommendationCache.get(userId);
        if (cached != null) {
            log.info("命中用户 {} 的推荐缓存，返回 {} 条", userId, cached.size());
            return cached;
        }

        log.info("缓存未命中，开始为用户 {} 生成完整推荐（召回+排序）", userId);

        // 2. 使用完整排序服务生成推荐（召回 → 特征 → 排序）
        List<Song> result = rankingService.recommend(userId, 10);

        // 3. 空结果同样缓存（短 TTL），否则无推荐结果的用户每次请求都要重跑召回与统计
        recommendationCache.put(userId, result);

        log.info("生成 {} 首推荐歌曲", result.size());
        return result;
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

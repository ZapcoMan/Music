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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

//import ArrayList;
//import List;
//import concurrent.TimeUnit;

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
    private static final String RECOMMENDATION_CACHE_KEY = "music:recommendation:user:";
    private static final long CACHE_EXPIRE_HOURS = 24; // 缓存过期时间24小时
    private static final long RECOMMENDATION_CACHE_HOURS = 6; // 推荐结果缓存6小时

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
    public Boolean delete(Integer id) {
        Boolean result = songListMapper.delete(id) > 0;
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
        int userId = Integer.parseInt(id);
        String cacheKey = RECOMMENDATION_CACHE_KEY + userId;
        
        // 1. 尝试从缓存中获取推荐结果
        Object cachedData = redisUtils.getCacheObject(cacheKey);
        if (cachedData != null) {
            log.info("从Redis缓存获取用户 {} 的推荐结果", userId);
            return (List<Song>) cachedData;
        }
        
        log.info("缓存未命中，开始为用户 {} 生成推荐", userId);
        
        // 2. 获取相似用户及其相似度分数
        List<Map<String, Object>> similarUsersWithScore = songListMapper.findSimilarUsersWithScore(userId);
        
        if (similarUsersWithScore == null || similarUsersWithScore.isEmpty()) {
            log.info("未找到相似用户，返回空推荐列表");
            return new ArrayList<>();
        }
        
        log.info("找到 {} 个相似用户", similarUsersWithScore.size());
        
        // 3. 使用Map存储歌曲及其加权得分
        Map<Integer, Double> songWeightMap = new HashMap<>();
        Map<Integer, Song> songMap = new HashMap<>();
        
        // 4. 遍历每个相似用户，根据相似度加权累加
        for (Map<String, Object> userWithScore : similarUsersWithScore) {
            Integer similarUserId = ((Number) userWithScore.get("userId")).intValue();
            Double similarity = ((Number) userWithScore.get("similarity")).doubleValue();
            
            log.debug("处理相似用户: {}, 相似度: {}", similarUserId, similarity);
            
            // 获取该相似用户收藏但当前用户未收藏的歌曲
            List<Song> songs = songMapper.getSongsByUserIdNotInOtherUser(userId, similarUserId);
            
            if (songs != null && !songs.isEmpty()) {
                for (Song song : songs) {
                    // 累加加权得分：相似度 * 基础分(1分)
                    double currentWeight = songWeightMap.getOrDefault(song.getId(), 0.0);
                    songWeightMap.put(song.getId(), currentWeight + similarity);
                    
                    // 保存歌曲对象（只保存一次）
                    if (!songMap.containsKey(song.getId())) {
                        songMap.put(song.getId(), song);
                    }
                }
            }
        }
        
        // 5. 按加权得分降序排序
        List<Map.Entry<Integer, Double>> sortedSongs = new ArrayList<>(songWeightMap.entrySet());
        sortedSongs.sort((e1, e2) -> Double.compare(e2.getValue(), e1.getValue()));
        
        // 6. 构建最终结果列表（最多返回10首）
        List<Song> result = new ArrayList<>();
        int limit = Math.min(sortedSongs.size(), 10);
        
        for (int i = 0; i < limit; i++) {
            Integer songId = sortedSongs.get(i).getKey();
            Double weight = sortedSongs.get(i).getValue();
            Song song = songMap.get(songId);
            
            if (song != null) {
                log.debug("推荐歌曲: {}, 权重: {}", song.getName(), weight);
                result.add(song);
            }
        }
        
        // 7. 将推荐结果存入缓存，设置6小时过期
        if (!result.isEmpty()) {
            redisUtils.setCacheObject(cacheKey, result, RECOMMENDATION_CACHE_HOURS, TimeUnit.HOURS);
            log.info("用户 {} 的推荐结果已缓存，有效期 {} 小时", userId, RECOMMENDATION_CACHE_HOURS);
        }
        
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

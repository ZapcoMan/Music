package com.shanzhu.music.service.impl;
import com.shanzhu.music.common.CacheKeys;

import com.shanzhu.music.entity.po.Collect;
import com.shanzhu.music.mapper.CollectMapper;
import com.shanzhu.music.service.CollectService;
import com.shanzhu.music.service.ranking.RecommendationCache;
import com.shanzhu.music.utils.RedisUtils;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 收藏 服务层实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CollectServiceImpl implements CollectService {

    private final CollectMapper collectMapper;
    private final RedisUtils redisUtils;
    private final RecommendationCache recommendationCache;
    
    // Redis缓存键前缀
    private static final String COLLECT_CACHE_KEY = CacheKeys.COLLECT;

    /**
     * 歌单收藏与歌曲收藏分键，避免两种列表互相覆盖缓存
     */
    private static final String COLLECT_SONG_LIST_CACHE_KEY = CacheKeys.COLLECT + "songlist:user:";
    private static final long CACHE_EXPIRE_HOURS = 12; // 缓存过期时间12小时

    /**
     * 添加收藏（延时双删缓存）
     *
     * @param collect 收藏信息
     * @return 结果
     */
    @Override
    public Boolean insert(Collect collect) {
        Boolean result = collectMapper.insert(collect) > 0;
        if (result) {
            Integer userId = collect.getUserId();
            // 使用延时双删策略清除用户收藏列表缓存
            redisUtils.deleteWithDelay(COLLECT_CACHE_KEY + "user:" + userId);
            // 歌单收藏与歌曲收藏是两个缓存键，写入时都要失效，
            // 否则收藏新歌单后"我的收藏歌单"仍会返回旧列表
            redisUtils.deleteWithDelay(COLLECT_SONG_LIST_CACHE_KEY + userId);
            // 让该用户的推荐缓存失效，下次请求按新偏好重新生成
            recommendationCache.evict(userId);
            log.info("新增收藏，清除收藏和推荐缓存，userId={}", userId);
        }
        return result;
    }

    /**
     * 删除收藏（延时双删缓存）
     *
     * @param id 收藏id
     * @return 结果
     */
    @Override
    public Boolean delete(Integer id) {
        // 先按主键取该行所属用户，再去删——删完就查不到了。
        // 原先的实现是遍历 allCollect() 找匹配 id，而那条查询带 LIMIT 1000：
        // 收藏行数一旦超过上限，目标行就不在返回集里，userId 保持 null，
        // 下面的缓存失效与推荐 evict 会被静默跳过——表现为收藏删了但列表与
        // 推荐最长 12 小时仍带着已删项。现在只回一列 user_id，
        // 既不全表扫描，也不受那个上限影响。
        Integer userId = collectMapper.selectUserIdById(id);
        
        Boolean result = collectMapper.delete(id) > 0;
        if (result && userId != null) {
            // 使用延时双删策略清除缓存
            redisUtils.deleteWithDelay(COLLECT_CACHE_KEY + "user:" + userId);
            // 让该用户的推荐缓存失效，下次请求按新偏好重新生成
            recommendationCache.evict(userId);
            log.info("删除收藏，清除收藏和推荐缓存，userId={}", userId);
        }
        return result;
    }

    /**
     * 删除收藏（延时双删缓存）
     *
     * @param userId 用户id
     * @param songId 歌曲id
     * @return 删除结果
     */
    @Override
    public Boolean deleteByUserIdSongId(Integer userId, Integer songId) {
        Boolean result = collectMapper.deleteByUserIdSongId(userId, songId) > 0;
        if (result) {
            // 使用延时双删策略清除缓存
            redisUtils.deleteWithDelay(COLLECT_CACHE_KEY + "user:" + userId);
            // 让该用户的推荐缓存失效，下次请求按新偏好重新生成
            recommendationCache.evict(userId);
            log.info("删除收藏，清除收藏和推荐缓存，userId={}", userId);
        }
        return result;
    }

    /**
     * 查询所有收藏
     *
     * @return 收藏列表
     */
    @Override
    public List<Collect> allCollect() {
        return collectMapper.allCollect();
    }

    /**
     * 查询某个用户的收藏列表（带缓存）
     *
     * @param userId 用户id
     * @return 收藏列表
     */
    @Override
    public List<Collect> collectOfUserId(Integer userId) {
        String cacheKey = COLLECT_CACHE_KEY + "user:" + userId;
        
        Object cachedData = redisUtils.getCacheObject(cacheKey);
        if (cachedData != null) {
            log.info("从Redis缓存获取用户收藏列表，userId={}", userId);
            return (List<Collect>) cachedData;
        }
        
        log.info("从数据库查询用户收藏列表，userId={}", userId);
        List<Collect> collects = collectMapper.collectOfUserId(userId);
        
        redisUtils.setCacheObject(cacheKey, collects, CACHE_EXPIRE_HOURS, TimeUnit.HOURS);
        
        return collects;
    }

    /**
     * 查询某个用户是否已经收藏了某个歌曲
     *
     * @param userId 用户id
     * @param songId 歌曲id
     * @return 是否收藏
     */
    @Override
    public Boolean existSongListId(Integer userId, Integer songListId) {
        return collectMapper.existSongListId(userId, songListId) > 0;
    }

    @Override
    public List<com.shanzhu.music.entity.po.SongList> collectSongListOfUserId(Integer userId) {
        String cacheKey = COLLECT_SONG_LIST_CACHE_KEY + userId;
        Object cached = redisUtils.getCacheObject(cacheKey);
        if (cached != null) {
            log.info("从Redis缓存获取用户收藏歌单，userId={}", userId);
            return (List<com.shanzhu.music.entity.po.SongList>) cached;
        }
        List<com.shanzhu.music.entity.po.SongList> list = collectMapper.selectSongListsByUserId(userId);
        redisUtils.setCacheObject(cacheKey, list, CACHE_EXPIRE_HOURS, TimeUnit.HOURS);
        return list;
    }

    @Override
    public Boolean deleteByUserIdSongListId(Integer userId, Integer songListId) {
        Boolean result = collectMapper.deleteByUserIdSongListId(userId, songListId) > 0;
        if (result) {
            redisUtils.deleteWithDelay(COLLECT_CACHE_KEY + "user:" + userId);
            redisUtils.deleteWithDelay(COLLECT_SONG_LIST_CACHE_KEY + userId);
            log.info("取消收藏歌单，延时双删缓存 - userId={}, songListId={}", userId, songListId);
        }
        return result;
    }

    @Override
    public Boolean existSongId(Integer userId, Integer songId) {
        return collectMapper.existSongId(userId, songId) > 0;
    }
}

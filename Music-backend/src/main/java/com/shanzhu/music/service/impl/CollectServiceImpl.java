package com.shanzhu.music.service.impl;

import com.shanzhu.music.entity.po.Collect;
import com.shanzhu.music.mapper.CollectMapper;
import com.shanzhu.music.service.CollectService;
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
    
    // Redis缓存键前缀
    private static final String COLLECT_CACHE_KEY = "music:collect:";
    private static final long CACHE_EXPIRE_HOURS = 12; // 缓存过期时间12小时

    /**
     * 添加收藏（清除缓存）
     *
     * @param collect 收藏信息
     * @return 结果
     */
    @Override
    public Boolean insert(Collect collect) {
        Boolean result = collectMapper.insert(collect) > 0;
        if (result) {
            // 清除用户收藏列表缓存
            redisUtils.deleteObject(COLLECT_CACHE_KEY + "user:" + collect.getUserId());
            log.info("新增收藏，清除缓存，userId={}", collect.getUserId());
        }
        return result;
    }

    /**
     * 删除收藏（清除缓存）
     *
     * @param id 收藏id
     * @return 结果
     */
    @Override
    public Boolean delete(Integer id) {
        // 先查询所有用户的收藏，找到对应的userId再清除缓存
        List<Collect> allCollects = collectMapper.allCollect();
        Integer userId = null;
        for (Collect collect : allCollects) {
            if (collect.getId().equals(id)) {
                userId = collect.getUserId();
                break;
            }
        }
        
        Boolean result = collectMapper.delete(id) > 0;
        if (result && userId != null) {
            redisUtils.deleteObject(COLLECT_CACHE_KEY + "user:" + userId);
            log.info("删除收藏，清除缓存，userId={}", userId);
        }
        return result;
    }

    /**
     * 删除收藏（清除缓存）
     *
     * @param userId 用户id
     * @param songId 歌曲id
     * @return 删除结果
     */
    @Override
    public Boolean deleteByUserIdSongId(Integer userId, Integer songId) {
        Boolean result = collectMapper.deleteByUserIdSongId(userId, songId) > 0;
        if (result) {
            redisUtils.deleteObject(COLLECT_CACHE_KEY + "user:" + userId);
            log.info("删除收藏，清除缓存，userId={}", userId);
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
    public Boolean existSongId(Integer userId, Integer songId) {
        return collectMapper.existSongId(userId, songId) > 0;
    }
}

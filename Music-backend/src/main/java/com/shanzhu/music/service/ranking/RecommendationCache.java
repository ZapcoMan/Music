package com.shanzhu.music.service.ranking;
import com.shanzhu.music.common.CacheKeys;

import com.shanzhu.music.entity.po.Song;
import com.shanzhu.music.utils.RedisUtils;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

/**
 * 用户推荐结果的缓存读写。
 *
 * 单独成组件而不是留在 SongListServiceImpl 内部，是因为收藏、搜索历史等写入方同样需要
 * 让推荐缓存失效；若它们反向注入 SongListService，会与歌单服务形成循环依赖。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RecommendationCache {

    private static final String KEY_PREFIX = CacheKeys.RECOMMENDATION;

    /** 有结果时的基准 TTL（分钟） */
    private static final long TTL_BASE_MINUTES = 6 * 60;
    /** TTL 随机抖动上限（分钟），避免同一批用户的缓存同时失效造成请求洪峰 */
    private static final long TTL_JITTER_MINUTES = 30;
    /** 空结果的短 TTL（分钟）：既挡住反复空跑全链路，又不会让新行为长期不可见 */
    private static final long EMPTY_TTL_MINUTES = 5;

    private final RedisUtils redisUtils;

    private String key(Integer userId) {
        return KEY_PREFIX + userId;
    }

    /**
     * 读取缓存。
     *
     * @return null 表示未命中；返回空列表表示"已确认该用户暂无推荐结果"，调用方不应再生成
     */
    public List<Song> get(Integer userId) {
        if (userId == null) {
            return null;
        }
        String cacheKey = key(userId);
        Object cached;
        try {
            cached = redisUtils.getCacheObject(cacheKey);
        } catch (Exception e) {
            // 兼容历史遗留的不可读格式（此前直接缓存裸集合，读取必抛异常）：
            // 删除该键并按未命中重新生成，而不是让异常冒泡成接口 500
            log.warn("推荐缓存反序列化失败，删除键 {} 并按未命中重新生成：{}", cacheKey, e.getMessage());
            redisUtils.deleteObject(cacheKey);
            return null;
        }
        if (cached instanceof CacheEntry entry) {
            return entry.getSongs() == null ? new ArrayList<>() : entry.getSongs();
        }
        return null;
    }

    /**
     * 写入缓存。空结果同样写入，但用短 TTL 占位。
     */
    public void put(Integer userId, List<Song> result) {
        if (userId == null || result == null) {
            return;
        }

        if (result.isEmpty()) {
            redisUtils.setCacheObject(key(userId), new CacheEntry(new ArrayList<Song>()),
                    EMPTY_TTL_MINUTES, TimeUnit.MINUTES);
            log.info("用户 {} 无推荐结果，写入 {} 分钟空值占位", userId, EMPTY_TTL_MINUTES);
            return;
        }

        long timeout = TTL_BASE_MINUTES + ThreadLocalRandom.current().nextLong(TTL_JITTER_MINUTES);
        redisUtils.setCacheObject(key(userId), new CacheEntry(result), timeout, TimeUnit.MINUTES);
        log.info("用户 {} 的推荐结果已缓存 {} 分钟", userId, timeout);
    }

    /**
     * 让某位用户的推荐缓存失效。收藏或搜索偏好变化后调用，
     * 避免偏好已经改变、推荐结果却要等数小时才更新。
     */
    public void evict(Integer userId) {
        if (userId == null) {
            return;
        }
        redisUtils.deleteObject(key(userId));
    }

    /**
     * 缓存值包装。
     *
     * 不能直接缓存 List<Song>：RedisConfig 用的是 Jackson 数组式 default typing，
     * 当根节点是集合时，类型 ID 只写在每个元素上、外层集合自身没有，
     * 读取按 Object 解析就会抛
     * "Unexpected token (START_ARRAY), expected VALUE_STRING: need ... type id"。
     * 包一层对象后根节点带类型 ID，读写可以正常往返。
     */
    @Data
    @NoArgsConstructor
    public static class CacheEntry {

        private List<Song> songs;

        CacheEntry(List<Song> songs) {
            this.songs = songs;
        }
    }
}

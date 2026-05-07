package com.shanzhu.music.utils;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * Redis工具类
 */
@Component
@RequiredArgsConstructor
public class RedisUtils {

    private final RedisTemplate<String, Object> redisTemplate;

    /**
     * 缓存基本的对象，Integer、String、实体类等
     *
     * @param key   缓存的键值
     * @param value 缓存的值
     */
    public void setCacheObject(final String key, final Object value) {
        redisTemplate.opsForValue().set(key, value);
    }

    /**
     * 缓存基本的对象，并设置过期时间
     *
     * @param key      缓存的键值
     * @param value    缓存的值
     * @param timeout  时间
     * @param timeUnit 时间颗粒度
     */
    public void setCacheObject(final String key, final Object value, final Long timeout, final TimeUnit timeUnit) {
        redisTemplate.opsForValue().set(key, value, timeout, timeUnit);
    }

    /**
     * 获得缓存的基本对象
     *
     * @param key 缓存键值
     * @return 缓存键值对应的数据
     */
    public Object getCacheObject(final String key) {
        return redisTemplate.opsForValue().get(key);
    }

    /**
     * 删除单个对象
     *
     * @param key 缓存键值
     */
    public void deleteObject(final String key) {
        redisTemplate.delete(key);
    }

    /**
     * 判断key是否存在
     *
     * @param key 键
     * @return true 存在 false不存在
     */
    public Boolean hasKey(final String key) {
        return redisTemplate.hasKey(key);
    }

    /**
     * 设置过期时间
     *
     * @param key      缓存的键值
     * @param timeout  时间
     * @param timeUnit 时间颗粒度
     * @return
     */
    public Boolean expire(final String key, final Long timeout, final TimeUnit timeUnit) {
        return redisTemplate.expire(key, timeout, timeUnit);
    }

    /**
     * 获得缓存的基本对象的剩余生存时间
     *
     * @param key 缓存键值
     * @return 剩余生存时间（秒）
     */
    public Long getExpire(final String key) {
        return redisTemplate.getExpire(key, TimeUnit.SECONDS);
    }
}

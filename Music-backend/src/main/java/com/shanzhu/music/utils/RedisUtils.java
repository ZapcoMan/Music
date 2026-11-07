package com.shanzhu.music.utils;

import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Redis工具类
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RedisUtils {

    private final RedisTemplate<String, Object> redisTemplate;

    /**
     * 延时双删第二次删除所用的共享调度池。
     *
     * 此前每次调用都 new Thread(...).start()：全项目 25 处 deleteWithDelay 调用点里，
     * 删除一个歌手要对它名下每首歌各调一次，批量删 100 首歌就是 300 个裸线程同时
     * sleep 三秒——线程数完全不受控，没有线程名也没有拒绝策略，出问题只能靠 thread dump 猜。
     * 固定 2 线程足够：这些任务不占 CPU，只在到期时发一条 DEL。
     */
    private final ScheduledExecutorService delaySweep = Executors.newScheduledThreadPool(2, new ThreadFactory() {
        private final AtomicInteger seq = new AtomicInteger();

        @Override
        public Thread newThread(Runnable runnable) {
            Thread thread = new Thread(runnable, "cache-delay-delete-" + seq.incrementAndGet());
            thread.setDaemon(true);
            return thread;
        }
    });

    @PreDestroy
    void shutdownDelaySweep() {
        delaySweep.shutdown();
    }

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

    /**
     * 延时双删策略 - 先删除缓存，再更新数据库，最后再次删除缓存
     * 用于解决高并发场景下的缓存与数据库不一致问题
     *
     * @param key           缓存键值
     * @param delaySeconds  第二次删除的延迟时间（秒），默认3秒
     */
    public void deleteWithDelay(final String key, final long delaySeconds) {
        // 第一次删除：立即删除缓存
        redisTemplate.delete(key);
        log.debug("延时双删 - 第一次删除缓存: {}", key);
        
        // 第二次删除交给共享调度池，并且要等事务提交之后再排期。
        //
        // 原先是在 @Transactional 方法内部直接起线程 sleep 三秒：事务一旦超过三秒，
        // 第二次删除就在提交之前跑完了，其后其他请求仍可能把提交前的旧数据读回缓存，
        // 双删等于没删；反过来事务回滚时，这次删除又纯属多余。
        // 挂到 afterCommit 上，排期起点才是"数据真正可见"的时刻，双删的语义才立得住。
        Runnable secondDelete = () -> delaySweep.schedule(() -> {
            try {
                redisTemplate.delete(key);
                log.debug("延时双删 - 第二次删除缓存: {}", key);
            } catch (Exception e) {
                log.error("延时双删第二次删除失败: {}", key, e);
            }
        }, delaySeconds, TimeUnit.SECONDS);

        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    secondDelete.run();
                }
            });
        } else {
            secondDelete.run();
        }
    }

    /**
     * 延时双删策略 - 使用默认延迟时间3秒
     *
     * @param key 缓存键值
     */
    public void deleteWithDelay(final String key) {
        deleteWithDelay(key, 3);
    }
}

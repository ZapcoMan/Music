package com.shanzhu.music.service.impl;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.shanzhu.music.entity.po.Consumer;
import com.shanzhu.music.mapper.ConsumerMapper;
import com.shanzhu.music.service.ConsumerService;
import com.shanzhu.music.utils.PasswordUtils;
import com.shanzhu.music.utils.RedisUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 前台用户相关 服务层实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ConsumerServiceImpl implements ConsumerService {

    private final ConsumerMapper consumerMapper;
    private final RedisUtils redisUtils;
    
    // Redis缓存键前缀
    private static final String CONSUMER_CACHE_KEY = "music:consumer:";
    private static final long CACHE_EXPIRE_HOURS = 24; // 缓存过期时间24小时

    /**
     * 添加前台用户（密码加密）
     *
     * @param consumer 前台用户信息
     * @return 结果
     */
    @Override
    public Boolean insert(Consumer consumer) {
        // 对密码进行BCrypt加密
        if (consumer.getPassword() != null && !consumer.getPassword().isEmpty()) {
            consumer.setPassword(PasswordUtils.encode(consumer.getPassword()));
        }
        return consumerMapper.insert(consumer) > 0;
    }

    /**
     * 修改前台用户（密码加密）
     *
     * @param consumer 前台用户信息
     * @return 结果
     */
    @Override
    public Boolean update(Consumer consumer) {
        // 如果密码不为空，则加密
        if (consumer.getPassword() != null && !consumer.getPassword().isEmpty()) {
            consumer.setPassword(PasswordUtils.encode(consumer.getPassword()));
        }
        return consumerMapper.update(consumer) > 0;
    }

    /**
     * 删除前端用户（清除缓存）
     *
     * @param id 用户id
     * @return 结果
     */
    @Override
    public Boolean delete(Integer id) {
        Consumer consumer = consumerMapper.selectByPrimaryKey(id);
        Boolean result = consumerMapper.delete(id) > 0;
        if (result && consumer != null) {
            redisUtils.deleteObject(CONSUMER_CACHE_KEY + id);
            redisUtils.deleteObject(CONSUMER_CACHE_KEY + "username:" + consumer.getUsername());
            log.info("删除用户，清除缓存");
        }
        return result;
    }

    /**
     * 根据主键查询整个对象（带缓存）
     *
     * @param id 主键id
     * @return 前台用户
     */
    @Override
    public Consumer selectByPrimaryKey(Integer id) {
        String cacheKey = CONSUMER_CACHE_KEY + id;
        
        Object cachedData = redisUtils.getCacheObject(cacheKey);
        if (cachedData != null) {
            log.info("从Redis缓存获取用户数据，id={}", id);
            return (Consumer) cachedData;
        }
        
        log.info("从数据库查询用户数据，id={}", id);
        Consumer consumer = consumerMapper.selectByPrimaryKey(id);
        
        if (consumer != null) {
            redisUtils.setCacheObject(cacheKey, consumer, CACHE_EXPIRE_HOURS, TimeUnit.HOURS);
        }
        
        return consumer;
    }

    /**
     * 查询所有前台用户
     *
     * @return 前台用户
     */
    @Override
    public List<Consumer> allConsumer() {
        return consumerMapper.allConsumer();
    }

    /**
     * 查看密码是否正确（BCrypt验证，带缓存）
     *
     * @param username 用户名
     * @param password 明文密码
     * @return 结果
     */
    @Override
    public Boolean verifyPassword(String username, String password) {
        // 先查询用户获取加密后的密码（带缓存）
        Consumer consumer = getByUsername(username);
        if (consumer == null) {
            return false;
        }
        // 使用BCrypt验证明文密码与加密密码是否匹配
        return PasswordUtils.matches(password, consumer.getPassword());
    }

    /**
     * 根据账号查询（带缓存）
     *
     * @param username 账号名
     * @return 前端用户
     */
    @Override
    public Consumer getByUsername(String username) {
        String cacheKey = CONSUMER_CACHE_KEY + "username:" + username;
        
        Object cachedData = redisUtils.getCacheObject(cacheKey);
        if (cachedData != null) {
            log.info("从Redis缓存获取用户数据，username={}", username);
            return (Consumer) cachedData;
        }
        
        log.info("从数据库查询用户数据，username={}", username);
        Consumer consumer = consumerMapper.getByUsername(username);
        
        if (consumer != null) {
            redisUtils.setCacheObject(cacheKey, consumer, CACHE_EXPIRE_HOURS, TimeUnit.HOURS);
        }
        
        return consumer;
    }
    
    /**
     * 分页查询所有前台用户
     *
     * @param pageNum 页码
     * @param pageSize 每页大小
     * @return 分页结果
     */
    @Override
    public PageInfo<Consumer> allConsumerByPage(int pageNum, int pageSize) {
        // 开启分页
        PageHelper.startPage(pageNum, pageSize);
        // 执行查询
        List<Consumer> list = consumerMapper.allConsumer();
        // 封装分页结果
        return new PageInfo<>(list);
    }

}

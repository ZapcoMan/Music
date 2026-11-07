package com.shanzhu.music.service.impl;
import com.shanzhu.music.common.CacheKeys;

import cn.hutool.core.date.DatePattern;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.shanzhu.music.entity.dto.ConsumerRequest;
import com.shanzhu.music.entity.po.Consumer;
import com.shanzhu.music.exception.BusinessException;
import com.shanzhu.music.mapper.CollectMapper;
import com.shanzhu.music.mapper.CommentMapper;
import com.shanzhu.music.mapper.ConsumerMapper;
import com.shanzhu.music.mapper.RankMapper;
import com.shanzhu.music.service.ConsumerService;
import com.shanzhu.music.utils.PasswordUtils;
import com.shanzhu.music.utils.RedisUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
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
    private final CollectMapper collectMapper;
    private final CommentMapper commentMapper;
    private final RankMapper rankMapper;
    private final RedisUtils redisUtils;
    
    // Redis缓存键前缀
    private static final String CONSUMER_CACHE_KEY = CacheKeys.CONSUMER;
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
     * 注册新用户（查重 + 生日解析 + 密码加密）
     *
     * @param request 注册请求
     * @return 已落库的用户
     */
    @Override
    public Consumer register(ConsumerRequest request) {
        // 查重问数据库，不问缓存：按 username 存的用户缓存有 24 小时寿命，
        // 账号删除或改名缓存滞后时，注册会对一个其实可用的用户名误报"已存在"。
        // 唯一性判断必须由权威存储给出。
        if (consumerMapper.getByUsername(request.getUsername()) != null) {
            throw new BusinessException("用户名已存在");
        }
        Consumer consumer = toConsumer(request);
        // 头像由上传接口单独维护，注册时先占空串，与原 Controller 行为一致
        consumer.setAvator("");
        if (!insert(consumer)) {
            throw new BusinessException("添加失败");
        }
        return consumer;
    }

    /**
     * 修改本人资料
     *
     * @param request       资料请求
     * @param currentUserId 令牌中的用户id
     * @return 修改结果
     */
    @Override
    public Boolean updateProfile(ConsumerRequest request, Integer currentUserId) {
        // 以令牌身份为准：原实现按 username 反查用户，任何人提交他人用户名即可改其资料
        Consumer existConsumer = selectByPrimaryKey(currentUserId);
        if (existConsumer == null) {
            throw new BusinessException("用户不存在");
        }
        Consumer consumer = toConsumer(request);
        consumer.setId(currentUserId);
        // 用户名是登录凭据，不允许通过本接口改名
        consumer.setUsername(existConsumer.getUsername());
        return update(consumer);
    }

    /**
     * 注册/资料请求转持久层对象。
     *
     * 库里 birth 是 date，前端传的是 yyyy-MM-dd 字符串，这段转换原先在 ConsumerController
     * 的 add 与 update 里各写一遍，SingerController 还有两份同款；漏改一处就会出现
     * "注册能存生日、改资料把生日丢掉"这类只在半边生效的差异。
     */
    private Consumer toConsumer(ConsumerRequest request) {
        Consumer consumer = new Consumer();
        consumer.setUsername(request.getUsername());
        consumer.setPassword(request.getPassword());
        consumer.setSex(request.getSex());
        consumer.setPhoneNum(request.getPhoneNum());
        consumer.setEmail(request.getEmail());
        consumer.setBirth(parseBirth(request.getBirth()));
        consumer.setIntroduction(request.getIntroduction());
        consumer.setLocation(request.getLocation());
        return consumer;
    }

    /**
     * 前端生日字符串转 Date；空串与 null 一律按"未填写"处理成 null。
     */
    private static Date parseBirth(String birth) {
        return StrUtil.isBlank(birth) ? null : DateUtil.parse(birth, DatePattern.NORM_DATE_PATTERN);
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
        Boolean result = consumerMapper.update(consumer) > 0;
        if (result) {
            // 两份缓存都要清：按 id 的那份决定资料页显示什么，按 username 的那份是登录时
            // 取密码的入口。此前 update 完全没碰缓存，改完密码后旧哈希还能再活 24 小时，
            // 用户会看到"新密码登不进去、旧密码仍然能用"
            redisUtils.deleteObject(CONSUMER_CACHE_KEY + consumer.getId());
            redisUtils.deleteObject(CONSUMER_CACHE_KEY + "username:" + consumer.getUsername());
            log.info("修改用户资料，清除 id 与 username 两份缓存");
        }
        return result;
    }

    /**
     * 删除前端用户（清除缓存）
     *
     * @param id 用户id
     * @return 结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean delete(Integer id) {
        Consumer consumer = consumerMapper.selectByPrimaryKey(id);
        Boolean result = consumerMapper.delete(id) > 0;
        if (result) {
            // 级联清理：残留收藏会让热门榜与协同过滤统计一个已不存在的用户，
            // 残留评分会继续参与相似度计算，残留评论则显示为无名用户
            collectMapper.deleteByUserId(id);
            commentMapper.deleteByUserId(id);
            rankMapper.deleteByConsumerId(id);
            log.info("删除用户 {}，已级联清理收藏、评论与评分", id);
        }
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
     * 查看密码是否正确（BCrypt验证）
     *
     * 这条必须读库，不能走 getByUsername 的缓存：Consumer.password 上有 @JsonIgnore，
     * 而缓存值就是 Jackson 序列化的结果，所以缓存里的用户没有密码字段。命中缓存后
     * PasswordUtils.matches 会拿 null 去比对，用户表现为"第一次能登录、之后就登不进去"。
     * 凭据本来也不该停在共享缓存里。
     *
     * @param username 账号名
     * @param password 明文密码
     * @return 结果
     */
    @Override
    public Boolean verifyPassword(String username, String password) {
        Consumer consumer = consumerMapper.getByUsername(username);
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

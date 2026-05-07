package com.shanzhu.music.service.impl;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.shanzhu.music.entity.po.Consumer;
import com.shanzhu.music.mapper.ConsumerMapper;
import com.shanzhu.music.service.ConsumerService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 前台用户相关 服务层实现类
 */
@Service
@RequiredArgsConstructor
public class ConsumerServiceImpl implements ConsumerService {

    private final ConsumerMapper consumerMapper;

    /**
     * 添加前台用户
     *
     * @param consumer 前台用户信息
     * @return 结果
     */
    @Override
    public Boolean insert(Consumer consumer) {
        return consumerMapper.insert(consumer) > 0;
    }

    /**
     * 修改前台用户
     *
     * @param consumer 前台用户信息
     * @return 结果
     */
    @Override
    public Boolean update(Consumer consumer) {
        return consumerMapper.update(consumer) > 0;
    }

    /**
     * 删除前端用户
     *
     * @param id 用户id
     * @return 结果
     */
    @Override
    public Boolean delete(Integer id) {
        return consumerMapper.delete(id) > 0;
    }

    /**
     * 根据主键查询整个对象
     *
     * @param id 主键id
     * @return 前台用户
     */
    @Override
    public Consumer selectByPrimaryKey(Integer id) {
        return consumerMapper.selectByPrimaryKey(id);
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
     * 查看密码是否正确
     *
     * @param username 用户名
     * @param password 密码
     * @return 结果
     */
    @Override
    public Boolean verifyPassword(String username, String password) {
        return consumerMapper.verifyPassword(username, password) > 0;
    }

    /**
     * 根据账号查询
     *
     * @param username 账号名
     * @return 前端用户
     */
    @Override
    public Consumer getByUsername(String username) {
        return consumerMapper.getByUsername(username);
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

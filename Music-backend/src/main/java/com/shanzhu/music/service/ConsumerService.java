package com.shanzhu.music.service;

import com.github.pagehelper.PageInfo;
import com.shanzhu.music.entity.dto.ConsumerProfileRequest;
import com.shanzhu.music.entity.dto.ConsumerRequest;
import com.shanzhu.music.entity.po.Consumer;

import java.util.List;

/**
 * 前台用户相关 服务层
 */
public interface ConsumerService {

    /**
     * 添加前台用户
     *
     * @param consumer 前台用户信息
     * @return 结果
     */
    Boolean insert(Consumer consumer);

    /**
     * 注册新用户（查重 + 生日解析 + 密码加密）
     *
     * @param request 注册请求
     * @return 已落库的用户，id 由自增主键回填，供审计日志记录操作对象
     */
    Consumer register(ConsumerRequest request);

    /**
     * 修改本人资料（身份只认令牌，用户名作为登录凭据不可改）
     *
     * @param request       资料请求
     * @param currentUserId 令牌中的用户id
     * @return 修改结果
     */
    Boolean updateProfile(ConsumerProfileRequest request, Integer currentUserId);

    /**
     * 更新前端用户
     *
     * @param consumer 前端用户
     * @return 结果
     */
    Boolean update(Consumer consumer);

    /**
     * 删除前端用户
     *
     * @param id 用户id
     * @return 结果
     */
    Boolean delete(Integer id);

    /**
     * 根据主键查询整个对象
     *
     * @param id 主键id
     * @return 前台用户
     */
    Consumer selectByPrimaryKey(Integer id);

    /**
     * 查看密码是否正确
     *
     * @param username 用户名
     * @param password 密码
     * @return 结果
     */
    Boolean verifyPassword(String username, String password);

    /**
     * 根据账号查询
     *
     * @param username 账号名
     * @return 前端用户
     */
    Consumer getByUsername(String username);
    
    /**
     * 分页查询所有前台用户
     *
     * @param pageNum 页码
     * @param pageSize 每页大小
     * @return 分页结果
     */
    PageInfo<Consumer> allConsumerByPage(int pageNum, int pageSize, String keyword);
}

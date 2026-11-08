package com.shanzhu.music.mapper;

import com.shanzhu.music.entity.po.Consumer;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 前台用户相关 持久层
 */
@Mapper
public interface ConsumerMapper {

    /**
     * 添加前台用户
     *
     * @param consumer 前台用户信息
     * @return 结果
     */
    Integer insert(Consumer consumer);

    /**
     * 修改前台用户
     *
     * @param consumer 前台用户信息
     * @return 结果
     */
    Integer update(Consumer consumer);

    /**
     * 删除
     *
     * @param id 前台用户id
     * @return 结果
     */
    Integer delete(Integer id);

    /**
     * 根据主键查询整个对象
     *
     * @param id 主键id
     * @return 前台用户
     */
    Consumer selectByPrimaryKey(Integer id);

    /**
     * 按关键词模糊查询前台用户，供管理端筛选与分页共用。
     * keyword 为空时不加 WHERE，等价于全量查询（分页由 PageHelper 在这一条上生效）。
     *
     * @param keyword 账号、电话或邮箱的模糊词
     * @return 匹配的用户列表
     */
    List<Consumer> selectByKeyword(@Param("keyword") String keyword);

    /**
     * 根据账号查询
     *
     * @param username 账户名
     * @return 前台用户
     */
    Consumer getByUsername(String username);

}

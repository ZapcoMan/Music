package com.shanzhu.music.mapper;

import com.shanzhu.music.entity.po.Admin;
import org.apache.ibatis.annotations.Mapper;

/**
 * 管理员 持久层
 */
@Mapper
public interface AdminMapper {

    /**
     * 验证密码是否正确（旧方法，保留兼容）
     *
     * @param username 用户名
     * @param password 密码
     * @return 结果
     */
    Integer verifyPassword(String username, String password);

    /**
     * 根据用户名查询管理员
     *
     * @param name 用户名
     * @return 管理员信息
     */
    Admin getByName(String name);

}

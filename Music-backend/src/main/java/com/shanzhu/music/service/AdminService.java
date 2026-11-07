package com.shanzhu.music.service;

import com.shanzhu.music.entity.po.Admin;

/**
 * 后台管理 服务层
 */
public interface AdminService {

    /**
     * 验证密码是否正确
     *
     * @param username 用户名
     * @param password 密码
     * @return 验证结果
     */
    Boolean verifyPassword(String username, String password);

    /**
     * 按账号名查询管理员。
     * 存在于此而非让 Controller 直接调 AdminMapper：分层越界会让控制器
     * 同时持有 service 与 mapper 两条数据通路，改动时极易只改一侧。
     *
     * @param username 账号名
     * @return 管理员，不存在时返回 null
     */
    Admin getByName(String username);

}

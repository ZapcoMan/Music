package com.shanzhu.music.service.impl;

import com.shanzhu.music.entity.po.Admin;
import com.shanzhu.music.mapper.AdminMapper;
import com.shanzhu.music.service.AdminService;
import com.shanzhu.music.utils.PasswordUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 后台管理 服务层实现
 */
@Service
@RequiredArgsConstructor
public class AdminServiceImpl implements AdminService {

    private final AdminMapper adminMapper;

    /**
     * 验证密码是否正确（BCrypt验证）
     *
     * @param username 用户名
     * @param password 明文密码
     * @return 验证结果
     */
    @Override
    public Boolean verifyPassword(String username, String password) {
        // 先查询管理员获取加密后的密码
        Admin admin = adminMapper.getByName(username);
        if (admin == null) {
            return false;
        }
        // 使用BCrypt验证明文密码与加密密码是否匹配
        return PasswordUtils.matches(password, admin.getPassword());
    }

}

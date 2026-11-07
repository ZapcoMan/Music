package com.shanzhu.music.service.impl;

import com.shanzhu.music.BaseTest;
import com.shanzhu.music.utils.PasswordUtils;import com.shanzhu.music.entity.po.Admin;
import com.shanzhu.music.service.AdminService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;

import static org.junit.jupiter.api.Assertions.*;

/**
 * AdminService 单元测试
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class AdminServiceImplTest extends BaseTest {

    @Autowired
    private AdminService adminService;



    @Test
    @Order(1)
    @DisplayName("测试管理员口令哈希机制")
    void testLoginWithCorrectPassword() {
        // schema.sql 里那个种子哈希的明文没有记录在仓库任何位置，
        // 因此"用已知明文能登录 admin"这一支在无凭据环境下无法断言 ——
        // 上一版用例按注释猜是 123456，实测不成立。
        // 这里只断言能确证的两件事：哈希机制自洽、错误口令一定被拒。
        String hash = PasswordUtils.encode("Probe#2026");
        org.junit.jupiter.api.Assertions.assertTrue(
                PasswordUtils.matches("Probe#2026", hash), "同一明文的 BCrypt 校验应通过");
        org.junit.jupiter.api.Assertions.assertFalse(
                PasswordUtils.matches("另一个口令", hash), "不同明文应校验失败");
        org.junit.jupiter.api.Assertions.assertFalse(
                adminService.verifyPassword("admin", "随便填的口令"), "错误口令不应通过");
        // AdminMapper 只有 verifyPassword 与 getByName，没有任何写入方法，
        // 因此也无法在测试里构造一个已知口令的管理员账号
    }


    @Test
    @Order(2)
    @DisplayName("测试管理员口令校验-错误")
    void testLoginWithWrongPassword() {
        org.junit.jupiter.api.Assertions.assertFalse(
                adminService.verifyPassword("admin", "绝不对的口令"), "错误口令不应通过");
        // 不存在的账号同样要返回 false，而不是抛异常
        org.junit.jupiter.api.Assertions.assertFalse(
                adminService.verifyPassword("no_such_admin", "123456"), "不存在的账号不应通过");
    }

    /**
     * 测试管理员登录 - 不存在的用户
     */
    @Test
    @Order(3)
    @DisplayName("测试管理员登录-不存在的用户")
    void testLoginWithNonExistentUser() {
        String username = "nonexistent";
        String password = "123456";
        
        Boolean result = adminService.verifyPassword(username, password);
        assertFalse(result, "不存在的用户登录应该失败");
    }


    @Test
    @Order(3)
    @DisplayName("测试按账号名查询管理员")
    void testSelectByPrimaryKey() {
        // AdminService 没有 selectByPrimaryKey，本次改造后统一走 getByName，
        // 这里断言的就是那条真实存在的路径
        Admin admin = adminService.getByName("admin");
        org.junit.jupiter.api.Assertions.assertNotNull(admin, "种子数据里应有 admin");
        org.junit.jupiter.api.Assertions.assertNotNull(admin.getId(), "查回的对象应带主键");
        org.junit.jupiter.api.Assertions.assertNull(adminService.getByName("no_such_admin"),
                "查不到应返回 null 而不是抛异常");
    }



}

package com.shanzhu.music.service.impl;

import com.shanzhu.music.BaseTest;
import com.shanzhu.music.utils.PasswordUtils;import com.shanzhu.music.entity.po.Consumer;
import com.shanzhu.music.service.ConsumerService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ConsumerService 单元测试
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ConsumerServiceImplTest extends BaseTest {

    @Autowired
    private ConsumerService consumerService;

    private BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private static Integer testUserId;

    /**
     * 测试用户注册
     */
    @Test
    @Order(1)
    @DisplayName("测试用户注册")
    void testInsert() {
        Consumer consumer = new Consumer();
        consumer.setUsername("testuser2026");
        consumer.setPassword(passwordEncoder.encode("123456"));
        consumer.setSex((byte) 1);
        consumer.setPhoneNum("13800138001");
        consumer.setEmail("test2026@example.com");
        consumer.setLocation("深圳");
        consumer.setIntroduction("测试用户");
        consumer.setAvator("/userImages/test.jpg");

        Boolean result = consumerService.insert(consumer);
        assertTrue(result, "用户注册应该成功");
        
        assertNotNull(consumer.getId(), "用户ID不应为空");
        testUserId = consumer.getId();
        System.out.println("注册用户ID: " + testUserId);
    }



    @Test
    @Order(2)
    @DisplayName("测试前台用户口令校验")
    void testLoginWithCorrectPassword() {
        // 自己用项目的加密工具写入已知明文，不依赖种子哈希的来历
        Consumer probe = new Consumer();
        probe.setUsername("pwd_probe_user");
        // 传明文而非预先加密的哈希：ConsumerServiceImpl.insert 自己会做一次 BCrypt。
        // 上一版在这里传了 encode 的结果，于是被加密两次，校验必然失败 ——
        // 这条断言顺带把"insert 只加密一次"这个契约锁住（正是 #52 毁号问题的反面）
        probe.setPassword("Probe#2026");
        probe.setSex((byte) 1);
        probe.setAvator("");
        consumerService.insert(probe);
        try {
            org.junit.jupiter.api.Assertions.assertTrue(
                    consumerService.verifyPassword("pwd_probe_user", "Probe#2026"), "正确口令应通过");
            org.junit.jupiter.api.Assertions.assertFalse(
                    consumerService.verifyPassword("pwd_probe_user", "错的口令"), "错误口令不应通过");
            org.junit.jupiter.api.Assertions.assertFalse(
                    consumerService.verifyPassword("no_such_user", "Probe#2026"), "账号不存在不应通过");
        } finally {
            consumerService.delete(probe.getId());
        }
    }

    /**
     * 测试缓存命中后的口令校验
     *
     * Consumer.password 上有 @JsonIgnore（防凭据外泄），但值序列化用的就是 Jackson，
     * 于是整体缓存进 Redis 的那个对象里根本没有密码字段。verifyPassword 若走带缓存的
     * getByUsername，第二次登录就是拿 null 去做 BCrypt —— 用户表现为"第一次能登、
     * 之后再也登不进去"。上一版用例先试正确口令、再试错误口令，恰好把这条路径绕开了。
     */
    @Test
    @Order(6)
    @DisplayName("测试缓存命中后口令校验仍然通过")
    void testVerifyPasswordAfterCacheWarm() {
        Consumer probe = new Consumer();
        probe.setUsername("cache_probe_user");
        probe.setPassword("Cache#2026");
        probe.setSex((byte) 1);
        probe.setAvator("");
        consumerService.insert(probe);
        try {
            assertTrue(consumerService.verifyPassword("cache_probe_user", "Cache#2026"),
                    "首次校验（读库并回填缓存）应通过");
            assertTrue(consumerService.verifyPassword("cache_probe_user", "Cache#2026"),
                    "缓存命中后仍应通过：口令校验不能依赖缓存里的密码字段");
        } finally {
            consumerService.delete(probe.getId());
        }
    }

    /**
     * 测试用户登录 - 错误密码
     */
    @Test
    @Order(3)
    @DisplayName("测试用户登录-错误密码")
    void testLoginWithWrongPassword() {
        String username = "testuser";
        String password = "wrongpassword";
        
        Boolean result = consumerService.verifyPassword(username, password);
        assertFalse(result, "使用错误密码登录应该失败");
    }

    /**
     * 测试用户登录 - 不存在的用户
     */
    @Test
    @Order(4)
    @DisplayName("测试用户登录-不存在的用户")
    void testLoginWithNonExistentUser() {
        String username = "nonexistentuser";
        String password = "123456";
        
        Boolean result = consumerService.verifyPassword(username, password);
        assertFalse(result, "不存在的用户登录应该失败");
    }

    /**
     * 测试查询用户
     *
     * 原先查的是硬编码 id=1，而测试库的 consumer 表是空的——它能通过全靠上一轮跑完
     * 遗留在 Redis 里的 music:consumer:1。缓存对齐之后这条立刻暴露，改为查本类注册的账号。
     */
    @Test
    @Order(5)
    @DisplayName("测试查询用户")
    void testSelectByPrimaryKey() {
        assertNotNull(testUserId, "前置注册用户应已存在");
        Consumer consumer = consumerService.selectByPrimaryKey(testUserId);
        assertNotNull(consumer, "查询结果不应为空");
        assertEquals("testuser2026", consumer.getUsername(), "按 id 查回的应是注册用户本人");
    }


    @Test
    @Order(3)
    @DisplayName("测试修改用户资料真的落库")
    void testUpdate() {
        Consumer probe = new Consumer();
        probe.setUsername("update_probe_user");
        probe.setPassword("123456");
        probe.setSex((byte) 1);
        probe.setAvator("");
        consumerService.insert(probe);
        try {
            Consumer upd = new Consumer();
            upd.setId(probe.getId());
            upd.setEmail("probe@example.com");
            upd.setLocation("测试城市");
            org.junit.jupiter.api.Assertions.assertTrue(consumerService.update(upd), "更新应返回成功");

            Consumer back = consumerService.selectByPrimaryKey(probe.getId());
            org.junit.jupiter.api.Assertions.assertEquals("probe@example.com", back.getEmail(), "邮箱应已写入");
            org.junit.jupiter.api.Assertions.assertEquals("测试城市", back.getLocation(), "地区应已写入");
        } finally {
            consumerService.delete(probe.getId());
        }
    }

    /**
     * 测试删除用户
     */
    @Test
    @Order(7)
    @DisplayName("测试删除用户")
    void testDelete() {
        if (testUserId != null) {
            Boolean result = consumerService.delete(testUserId);
            assertTrue(result, "删除用户应该成功");
            
            Consumer deletedUser = consumerService.selectByPrimaryKey(testUserId);
            assertNull(deletedUser, "删除后查询应该返回null");
            System.out.println("用户已删除,ID: " + testUserId);
        }
    }

    /**
     * 测试根据用户名查询
     *
     * 原先按 "testuser" 查，而库里从来没有这个账号——它能通过全靠缓存里上一轮的残留。
     * 这里也不能复用 testuser2026：@Order(7) 的删除用例先于本条执行，那个人已经没了。
     * 所以自建探针、自查自删，用例之间不再共享生命周期。
     */
    @Test
    @Order(8)
    @DisplayName("测试根据用户名查询")
    void testGetByUsername() {
        String username = "lookup_probe_user";
        Consumer probe = new Consumer();
        probe.setUsername(username);
        probe.setPassword("Lookup#2026");
        probe.setSex((byte) 1);
        probe.setAvator("");
        consumerService.insert(probe);
        try {
            Consumer found = consumerService.getByUsername(username);
            assertNotNull(found, "查询结果不应为空");
            assertEquals(username, found.getUsername(), "用户名应该匹配");
            assertEquals(probe.getId(), found.getId(), "按账号查回的应是刚注册的那一行");
        } finally {
            consumerService.delete(probe.getId());
        }
    }
}

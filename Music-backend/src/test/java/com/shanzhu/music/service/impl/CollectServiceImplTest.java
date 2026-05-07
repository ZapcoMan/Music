package com.shanzhu.music.service.impl;

import com.shanzhu.music.BaseTest;
import com.shanzhu.music.entity.po.Collect;
import com.shanzhu.music.service.CollectService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * CollectService 单元测试
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class CollectServiceImplTest extends BaseTest {

    @Autowired
    private CollectService collectService;

    private static Integer testCollectId;

    /**
     * 测试添加收藏
     */
    @Test
    @Order(1)
    @DisplayName("测试添加收藏")
    void testInsert() {
        Collect collect = new Collect();
        collect.setUserId(1);
        collect.setType((byte) 0); // 0表示收藏歌曲
        collect.setSongId(1);

        Boolean result = collectService.insert(collect);
        assertTrue(result, "添加收藏应该成功");
        
        assertNotNull(collect.getId(), "收藏ID不应为空");
        testCollectId = collect.getId();
        System.out.println("添加的收藏ID: " + testCollectId);
    }

    /**
     * 测试查询所有收藏
     */
    @Test
    @Order(2)
    @DisplayName("测试查询所有收藏")
    void testAllCollect() {
        List<Collect> collects = collectService.allCollect();
        assertNotNull(collects, "收藏列表不应为空");
        System.out.println("收藏总数: " + collects.size());
    }

    /**
     * 测试查询用户的收藏列表
     */
    @Test
    @Order(3)
    @DisplayName("测试查询用户的收藏列表")
    void testCollectOfUserId() {
        List<Collect> collects = collectService.collectOfUserId(1);
        assertNotNull(collects, "收藏列表不应为空");
        System.out.println("用户ID=1的收藏数量: " + collects.size());
    }

    /**
     * 测试检查用户是否已收藏某首歌曲
     */
    @Test
    @Order(4)
    @DisplayName("测试检查用户是否已收藏某首歌曲")
    void testExistSongId() {
        Boolean exists = collectService.existSongId(1, 1);
        assertNotNull(exists, "查询结果不应为空");
        System.out.println("用户1是否收藏歌曲1: " + exists);
    }

    /**
     * 测试删除收藏(通过ID)
     */
    @Test
    @Order(5)
    @DisplayName("测试删除收藏-通过ID")
    void testDeleteById() {
        if (testCollectId != null) {
            Boolean result = collectService.delete(testCollectId);
            assertTrue(result, "删除收藏应该成功");
            System.out.println("收藏已删除,ID: " + testCollectId);
        }
    }

    /**
     * 测试删除收藏(通过用户ID和歌曲ID)
     */
    @Test
    @Order(6)
    @DisplayName("测试删除收藏-通过用户ID和歌曲ID")
    void testDeleteByUserIdSongId() {
        // 先添加一个测试收藏
        Collect collect = new Collect();
        collect.setUserId(1);
        collect.setType((byte) 0);
        collect.setSongId(2);
        collectService.insert(collect);
        
        Boolean result = collectService.deleteByUserIdSongId(1, 2);
        assertTrue(result, "删除收藏应该成功");
        System.out.println("通过用户ID和歌曲ID删除收藏测试通过");
    }
}

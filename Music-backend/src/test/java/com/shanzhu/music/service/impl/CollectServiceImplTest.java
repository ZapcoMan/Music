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
 *
 * 写法约定（与 CommentServiceImplTest 同源）：
 * 1) 不用 `if (testCollectId != null)` 包住断言——静态字段为 null 时一条断言都不执行
 *    却照样通过，那是假绿；
 * 2) 成员断言放在 allCollect() 这条不走 Redis 的路径上。collectOfUserId /
 *    collectSongListOfUserId 带缓存，而本机 Redis 跨测试实例共享，
 *    并发跑测试时"结果里有没有我刚插的那行"取决于别人什么时候清过键。
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
        // 先清掉可能已存在的同一组合，否则 existSongId 为真时根本走不到"新插入"这一步，
        // 而"删掉之后再查应不存在"也会因为别的测试类留的行而不可判定
        collectService.deleteByUserIdSongId(1, 1);

        Collect collect = new Collect();
        collect.setUserId(1);
        collect.setType((byte) 0); // 0表示收藏歌曲
        collect.setSongId(1);

        Boolean result = collectService.insert(collect);
        assertTrue(result, "添加收藏应该成功");

        assertNotNull(collect.getId(), "收藏ID不应为空");
        testCollectId = collect.getId();
        // 插入后必须查得到：只断"影响行数 > 0"的话，写错表/写错用户也能通过
        assertTrue(collectService.allCollect().stream()
                        .anyMatch(c -> testCollectId.equals(c.getId())),
                "新增收藏应能被读回");
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
        // 只断"不是 null"等于没测：空表也是非 null。要求 Order(1) 造的那行确实在结果里
        assertTrue(collects.stream().anyMatch(c -> testCollectId.equals(c.getId())),
                "全量收藏应包含 Order(1) 写入的那条");
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
        assertFalse(collects.isEmpty(), "该用户至少应有 Order(1) 造的一条收藏");
        // 按用户过滤是这条接口的全部意义：混进别人的收藏就是越权读取
        assertTrue(collects.stream().allMatch(c -> Integer.valueOf(1).equals(c.getUserId())),
                "返回的收藏应全部属于 userId=1");
    }

    /**
     * 测试检查用户是否已收藏某首歌曲
     */
    @Test
    @Order(4)
    @DisplayName("测试检查用户是否已收藏某首歌曲")
    void testExistSongId() {
        // existSongId 是直接查库的布尔判断（收藏去重就靠它），两种结果都要钉住：
        // 只断"结果不是 null"时，恒真和恒假都能通过
        assertTrue(collectService.existSongId(1, 1), "已收藏的歌曲应判为存在");
        assertFalse(collectService.existSongId(1, -1), "不存在的收藏组合应判为不存在");
        assertFalse(collectService.existSongId(-1, 1), "别的用户不该被算成已收藏");
    }

    /**
     * 测试删除收藏(通过ID)
     */
    @Test
    @Order(5)
    @DisplayName("测试删除收藏-通过ID")
    void testDeleteById() {
        assertNotNull(testCollectId, "前置收藏应已由 Order(1) 落库");

        Boolean result = collectService.delete(testCollectId);
        assertTrue(result, "删除收藏应该成功");

        List<Collect> rest = collectService.allCollect();
        assertFalse(rest.stream().anyMatch(c -> testCollectId.equals(c.getId())),
                "删除后全量列表里不应再有这条");
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
        assertTrue(collectService.existSongId(1, 2), "前置收藏应已落库");

        Boolean result = collectService.deleteByUserIdSongId(1, 2);
        assertTrue(result, "删除收藏应该成功");

        // 上一版到此就结束了：删的到底是不是这一行、有没有顺手删掉别人的，都没验
        assertFalse(collectService.existSongId(1, 2), "该用户对该歌曲的收藏应已消失");
        assertNotNull(collect.getId(), "被删的应是本用例刚插入的那条");
        assertFalse(collectService.allCollect().stream()
                        .anyMatch(c -> collect.getId().equals(c.getId())),
                "按 (userId, songId) 删除应精确命中那一行，全量列表里也不该再有它");
    }
}

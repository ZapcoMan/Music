package com.shanzhu.music.service.impl;

import com.shanzhu.music.BaseTest;
import com.shanzhu.music.entity.po.ListSong;
import com.shanzhu.music.service.ListSongService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ListSongService 单元测试
 *
 * 与其它两个 *ServiceImplTest 同源的写法约定：不用 `if (testListSongId != null)`
 * 包住断言（静态字段为 null 时一条断言都不执行却照样通过，那是假绿），
 * 并且每次改动库之后都把库读回来验一次，而不是只看"影响行数 > 0"。
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ListSongServiceImplTest extends BaseTest {

    @Autowired
    private ListSongService listSongService;

    private static Integer testListSongId;

    /**
     * 测试给歌单添加歌曲
     */
    @Test
    @Order(1)
    @DisplayName("测试给歌单添加歌曲")
    void testInsert() {
        ListSong listSong = new ListSong();
        listSong.setSongId(1);
        listSong.setSongListId(1);

        Boolean result = listSongService.insert(listSong);
        assertTrue(result, "添加歌曲到歌单应该成功");

        assertNotNull(listSong.getId(), "记录ID不应为空");
        testListSongId = listSong.getId();
        // 关联表没有唯一约束，同一条 (song, list) 重复插入就是多行；
        // 这里把两个键都验一遍，确认写入的确实是"这首歌进这张歌单"
        ListSong stored = listSongService.selectByPrimaryKey(testListSongId);
        assertNotNull(stored, "关联行应能被读回");
        assertEquals(1, stored.getSongId(), "song_id 应等于提交值");
        assertEquals(1, stored.getSongListId(), "song_list_id 应等于提交值");
    }

    /**
     * 测试查询记录
     */
    @Test
    @Order(2)
    @DisplayName("测试查询记录")
    void testSelectByPrimaryKey() {
        assertNotNull(testListSongId, "前置关联行应已由 Order(1) 落库");
        ListSong listSong = listSongService.selectByPrimaryKey(testListSongId);
        assertNotNull(listSong, "查询结果不应为空");
        assertEquals(testListSongId, listSong.getId(), "应读回本用例造的那一行");
    }

    /**
     * 测试修改歌单里的歌曲
     */
    @Test
    @Order(3)
    @DisplayName("测试修改歌单里的歌曲")
    void testUpdate() {
        assertNotNull(testListSongId, "前置关联行应已落库");
        ListSong listSong = new ListSong();
        listSong.setId(testListSongId);
        listSong.setSongId(2);

        Boolean result = listSongService.update(listSong);
        assertTrue(result, "修改记录应该成功");

        // 验证修改：改的是这一行，且歌单归属没被顺手改掉
        ListSong updatedListSong = listSongService.selectByPrimaryKey(testListSongId);
        assertEquals(2, updatedListSong.getSongId(), "歌曲ID应该已修改");
        assertEquals(1, updatedListSong.getSongListId(), "更新不该改动所属歌单");
    }

    /**
     * 测试根据歌单ID查询歌曲
     */
    @Test
    @Order(4)
    @DisplayName("测试根据歌单ID查询歌曲")
    void testListSongOfSongListId() {
        List<ListSong> listSongs = listSongService.listSongOfSongListId(1);
        assertNotNull(listSongs, "歌曲列表不应为空");
        // 只断"不是 null"等于没测：空表也非 null。要求本用例造的那条在结果里
        assertTrue(listSongs.stream().anyMatch(x -> testListSongId.equals(x.getId())),
                "按歌单查询应包含 Order(1) 建立、Order(3) 改过的关联");
        assertTrue(listSongs.stream().allMatch(x -> Integer.valueOf(1).equals(x.getSongListId())),
                "结果应按 song_list_id 过滤，不该混进别的歌单");
    }

    /**
     * 测试删除记录(通过ID)
     */
    @Test
    @Order(5)
    @DisplayName("测试删除记录-通过ID")
    void testDeleteById() {
        assertNotNull(testListSongId, "前置关联行应已落库");

        Boolean result = listSongService.delete(testListSongId);
        assertTrue(result, "删除记录应该成功");

        assertNull(listSongService.selectByPrimaryKey(testListSongId), "删除后应查不到该行");
    }

    /**
     * 测试删除记录(通过歌曲ID和歌单ID)
     */
    @Test
    @Order(6)
    @DisplayName("测试删除记录-通过歌曲ID和歌单ID")
    void testDeleteBySongIdAndSongListId() {
        // 先添加一个测试记录
        ListSong listSong = new ListSong();
        listSong.setSongId(3);
        listSong.setSongListId(1);
        listSongService.insert(listSong);
        assertTrue(listSongService.listSongOfSongListId(1).stream()
                        .anyMatch(x -> Integer.valueOf(3).equals(x.getSongId())),
                "前置关联应已落库");

        Boolean result = listSongService.deleteBySongIdAndSongListId(3, 1);
        assertTrue(result, "删除记录应该成功");

        // 删除条件必须同时含 song_id 与 song_list_id：这里确认这首歌真的不在该歌单里了，
        // 而该歌单剩下的行不受影响
        assertFalse(listSongService.listSongOfSongListId(1).stream()
                        .anyMatch(x -> Integer.valueOf(3).equals(x.getSongId())),
                "按 (songId, songListId) 删除后不应再查到");
        assertNotNull(listSong.getId(), "被删的应是本用例刚插入的那一行");
        assertNull(listSongService.selectByPrimaryKey(listSong.getId()), "该行应已从库中删除");
    }
}

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
        System.out.println("添加的记录ID: " + testListSongId);
    }

    /**
     * 测试查询记录
     */
    @Test
    @Order(2)
    @DisplayName("测试查询记录")
    void testSelectByPrimaryKey() {
        if (testListSongId != null) {
            ListSong listSong = listSongService.selectByPrimaryKey(testListSongId);
            assertNotNull(listSong, "查询结果不应为空");
            System.out.println("查询到的记录 - 歌曲ID: " + listSong.getSongId() + ", 歌单ID: " + listSong.getSongListId());
        }
    }

    /**
     * 测试修改歌单里的歌曲
     */
    @Test
    @Order(3)
    @DisplayName("测试修改歌单里的歌曲")
    void testUpdate() {
        if (testListSongId != null) {
            ListSong listSong = new ListSong();
            listSong.setId(testListSongId);
            listSong.setSongId(2);

            Boolean result = listSongService.update(listSong);
            assertTrue(result, "修改记录应该成功");

            // 验证修改
            ListSong updatedListSong = listSongService.selectByPrimaryKey(testListSongId);
            assertEquals(2, updatedListSong.getSongId(), "歌曲ID应该已修改");
            System.out.println("修改后的歌曲ID: " + updatedListSong.getSongId());
        }
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
        System.out.println("歌单ID=1的歌曲数量: " + listSongs.size());
    }

    /**
     * 测试删除记录(通过ID)
     */
    @Test
    @Order(5)
    @DisplayName("测试删除记录-通过ID")
    void testDeleteById() {
        if (testListSongId != null) {
            Boolean result = listSongService.delete(testListSongId);
            assertTrue(result, "删除记录应该成功");
            System.out.println("记录已删除,ID: " + testListSongId);
        }
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
        
        Boolean result = listSongService.deleteBySongIdAndSongListId(3, 1);
        assertTrue(result, "删除记录应该成功");
        System.out.println("通过歌曲ID和歌单ID删除记录测试通过");
    }
}

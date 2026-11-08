package com.shanzhu.music.service.impl;

import com.github.pagehelper.PageInfo;
import com.shanzhu.music.BaseTest;
import com.shanzhu.music.entity.po.SongList;
import com.shanzhu.music.service.SongListService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * SongListService 单元测试
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class SongListServiceImplTest extends BaseTest {

    @Autowired
    private SongListService songListService;

    private static Integer testSongListId;

    /**
     * 测试添加歌单
     */
    @Test
    @Order(1)
    @DisplayName("测试添加歌单")
    void testInsert() {
        SongList songList = new SongList();
        songList.setTitle("测试歌单");
        songList.setPic("/img/songListPic/test.jpg");
        songList.setIntroduction("这是一个测试歌单");
        songList.setStyle("流行");

        Boolean result = songListService.insert(songList);
        assertTrue(result, "添加歌单应该成功");
        
        assertNotNull(songList.getId(), "歌单ID不应为空");
        testSongListId = songList.getId();
        System.out.println("添加的歌单ID: " + testSongListId);
    }

    /**
     * 测试查询歌单
     */
    @Test
    @Order(2)
    @DisplayName("测试查询歌单")
    void testSelectByPrimaryKey() {
        assertNotNull(testSongListId, "测试歌单ID不应为空");
        
        SongList songList = songListService.selectByPrimaryKey(testSongListId);
        assertNotNull(songList, "查询结果不应为空");
        assertEquals("测试歌单", songList.getTitle(), "歌单标题应该匹配");
        System.out.println("查询到的歌单: " + songList.getTitle());
    }

    /**
     * 测试修改歌单
     */
    @Test
    @Order(3)
    @DisplayName("测试修改歌单")
    void testUpdate() {
        assertNotNull(testSongListId, "测试歌单ID不应为空");
        
        SongList songList = new SongList();
        songList.setId(testSongListId);
        songList.setTitle("修改后的歌单");
        songList.setIntroduction("修改后的简介");

        Boolean result = songListService.update(songList);
        assertTrue(result, "修改歌单应该成功");

        // 验证修改是否生效
        SongList updatedSongList = songListService.selectByPrimaryKey(testSongListId);
        assertEquals("修改后的歌单", updatedSongList.getTitle(), "歌单标题应该已修改");
        System.out.println("修改后的歌单: " + updatedSongList.getTitle());
    }

    /**
     * 测试查询所有歌单
     */
    @Test
    @Order(4)
    @DisplayName("测试查询所有歌单")
    void testAllSongList() {
        List<SongList> songLists = songListService.allSongList();
        assertNotNull(songLists, "歌单列表不应为空");
        assertTrue(songLists.size() >= 2, "至少应该有2个测试歌单");
        System.out.println("歌单总数: " + songLists.size());
    }

    /**
     * 测试分页查询歌单
     */
    @Test
    @Order(5)
    @DisplayName("测试分页查询歌单")
    void testAllSongListByPage() {
        PageInfo<SongList> pageInfo = songListService.allSongListByPage(1, 1);
        assertNotNull(pageInfo, "分页结果不应为空");
        assertTrue(pageInfo.getTotal() >= 2, "总记录数应该至少2条");
        assertEquals(1, pageInfo.getSize(), "每页大小应该是1");
        assertTrue(pageInfo.getList().size() <= 1, "当前页数据不应超过1条");
        System.out.println("分页查询 - 总数: " + pageInfo.getTotal() + ", 当前页: " + pageInfo.getPageNum());
    }

    /**
     * 测试根据标题精确查询
     */
    @Test
    @Order(6)
    @DisplayName("测试根据标题精确查询")
    void testSongListOfTitle() {
        // 自带数据，避免依赖会被前面删除用例改动的共享种子
        SongList probe = new SongList();
        probe.setTitle("华语经典");
        probe.setPic("/img/probe.png");
        probe.setIntroduction("用例探针");
        probe.setStyle("华语");
        songListService.insert(probe);
        try {
            List<SongList> songLists = songListService.songListOfTitle("华语经典");
            assertNotNull(songLists, "查询结果不应为空");
            assertTrue(songLists.size() >= 1, "应该至少找到1个'华语经典'歌单");
            System.out.println("精确查询结果数: " + songLists.size());
        } finally {
            songListService.delete(probe.getId());
        }
    }

    /**
     * 测试根据标题模糊查询
     */
    @Test
    @Order(7)
    @DisplayName("测试根据标题模糊查询")
    void testLikeTitle() {
        SongList probe = new SongList();
        probe.setTitle("流行探针歌单");
        probe.setPic("/img/probe.png");
        probe.setIntroduction("用例探针");
        probe.setStyle("流行");
        songListService.insert(probe);
        try {
            List<SongList> songLists = songListService.likeTitle("流行");
            assertNotNull(songLists, "查询结果不应为空");
            assertTrue(songLists.size() >= 1, "应该至少找到1个包含'流行'的歌单");
            System.out.println("模糊查询结果数: " + songLists.size());
        } finally {
            songListService.delete(probe.getId());
        }
    }

    /**
     * 测试根据风格查询
     */
    @Test
    @Order(8)
    @DisplayName("测试根据风格查询")
    void testSongListOfStyle() {
        // 测试库的歌单表是空的，只断 notNull 等于永远为真；自建一条风格探针才有可断的东西
        // SongListService 中对应的方法是 likeStyle，模糊模式已由 service 负责拼接
        SongList probe = new SongList();
        probe.setTitle("风格探针歌单");
        probe.setStyle("华语");
        probe.setIntroduction("用例探针");
        songListService.insert(probe);
        try {
            List<SongList> songLists = songListService.likeStyle("华语");
            assertTrue(songLists.stream().anyMatch(item -> probe.getId().equals(item.getId())),
                    "按风格模糊查询应命中刚创建的探针歌单");
        } finally {
            songListService.delete(probe.getId());
        }
    }

    /**
     * 测试删除歌单
     */
    @Test
    @Order(9)
    @DisplayName("测试删除歌单")
    void testDelete() {
        assertNotNull(testSongListId, "测试歌单ID不应为空");
        
        Boolean result = songListService.delete(testSongListId);
        assertTrue(result, "删除歌单应该成功");

        // 验证删除是否生效
        SongList deletedSongList = songListService.selectByPrimaryKey(testSongListId);
        assertNull(deletedSongList, "删除后查询应该返回null");
        System.out.println("歌单已删除,ID: " + testSongListId);
    }

    /**
     * 测试删除不存在的歌单
     */
    @Test
    @Order(10)
    @DisplayName("测试删除不存在的歌单")
    void testDeleteNonExistent() {
        Boolean result = songListService.delete(99999);
        assertFalse(result, "删除不存在的歌单应该返回false");
    }
}

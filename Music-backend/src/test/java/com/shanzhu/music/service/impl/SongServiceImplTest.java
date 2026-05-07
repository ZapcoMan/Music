package com.shanzhu.music.service.impl;

import com.github.pagehelper.PageInfo;
import com.shanzhu.music.BaseTest;
import com.shanzhu.music.entity.po.Song;
import com.shanzhu.music.service.SongService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * SongService 单元测试
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class SongServiceImplTest extends BaseTest {

    @Autowired
    private SongService songService;

    private static Integer testSongId;

    /**
     * 测试添加歌曲
     */
    @Test
    @Order(1)
    @DisplayName("测试添加歌曲")
    void testInsert() {
        Song song = new Song();
        song.setSingerId(1);
        song.setName("测试歌曲");
        song.setIntroduction("这是一首测试歌曲");
        song.setPic("/img/songPic/test.jpg");
        song.setUrl("/song/test.mp3");

        Boolean result = songService.insert(song);
        assertTrue(result, "添加歌曲应该成功");
        
        assertNotNull(song.getId(), "歌曲ID不应为空");
        testSongId = song.getId();
        System.out.println("添加的歌曲ID: " + testSongId);
    }

    /**
     * 测试查询歌曲
     */
    @Test
    @Order(2)
    @DisplayName("测试查询歌曲")
    void testSelectByPrimaryKey() {
        assertNotNull(testSongId, "测试歌曲ID不应为空");
        
        Song song = songService.selectByPrimaryKey(testSongId);
        assertNotNull(song, "查询结果不应为空");
        assertEquals("测试歌曲", song.getName(), "歌曲名称应该匹配");
        System.out.println("查询到的歌曲: " + song.getName());
    }

    /**
     * 测试修改歌曲
     */
    @Test
    @Order(3)
    @DisplayName("测试修改歌曲")
    void testUpdate() {
        assertNotNull(testSongId, "测试歌曲ID不应为空");
        
        Song song = new Song();
        song.setId(testSongId);
        song.setName("修改后的歌曲");
        song.setIntroduction("修改后的简介");

        Boolean result = songService.update(song);
        assertTrue(result, "修改歌曲应该成功");

        // 验证修改是否生效
        Song updatedSong = songService.selectByPrimaryKey(testSongId);
        assertEquals("修改后的歌曲", updatedSong.getName(), "歌曲名称应该已修改");
        System.out.println("修改后的歌曲: " + updatedSong.getName());
    }

    /**
     * 测试查询所有歌曲
     */
    @Test
    @Order(4)
    @DisplayName("测试查询所有歌曲")
    void testAllSong() {
        List<Song> songs = songService.allSong();
        assertNotNull(songs, "歌曲列表不应为空");
        assertTrue(songs.size() >= 3, "至少应该有3个测试歌曲");
        System.out.println("歌曲总数: " + songs.size());
    }

    /**
     * 测试分页查询歌曲
     */
    @Test
    @Order(5)
    @DisplayName("测试分页查询歌曲")
    void testAllSongByPage() {
        PageInfo<Song> pageInfo = songService.allSongByPage(1, 2);
        assertNotNull(pageInfo, "分页结果不应为空");
        assertTrue(pageInfo.getTotal() >= 3, "总记录数应该至少3条");
        assertEquals(2, pageInfo.getSize(), "每页大小应该是2");
        assertTrue(pageInfo.getList().size() <= 2, "当前页数据不应超过2条");
        System.out.println("分页查询 - 总数: " + pageInfo.getTotal() + ", 当前页: " + pageInfo.getPageNum());
    }

    /**
     * 测试根据歌手ID查询歌曲
     */
    @Test
    @Order(6)
    @DisplayName("测试根据歌手ID查询歌曲")
    void testSongOfSingerId() {
        List<Song> songs = songService.songOfSingerId(1);
        assertNotNull(songs, "查询结果不应为空");
        System.out.println("歌手ID=1的歌曲数量: " + songs.size());
    }

    /**
     * 测试根据歌曲名精确查询
     */
    @Test
    @Order(7)
    @DisplayName("测试根据歌曲名精确查询")
    void testSongOfName() {
        List<Song> songs = songService.songOfName("七里香");
        assertNotNull(songs, "查询结果不应为空");
        assertTrue(songs.size() >= 1, "应该至少找到1首'七里香'");
        System.out.println("精确查询结果数: " + songs.size());
    }

    /**
     * 测试根据歌曲名模糊查询
     */
    @Test
    @Order(8)
    @DisplayName("测试根据歌曲名模糊查询")
    void testLikeSongOfName() {
        List<Song> songs = songService.likeSongOfName("%青花%");
        assertNotNull(songs, "查询结果不应为空");
        assertTrue(songs.size() >= 1, "应该至少找到1首包含'青花'的歌曲");
        System.out.println("模糊查询结果数: " + songs.size());
    }

    /**
     * 测试删除歌曲
     */
    @Test
    @Order(9)
    @DisplayName("测试删除歌曲")
    void testDelete() {
        assertNotNull(testSongId, "测试歌曲ID不应为空");
        
        Boolean result = songService.delete(testSongId);
        assertTrue(result, "删除歌曲应该成功");

        // 验证删除是否生效
        Song deletedSong = songService.selectByPrimaryKey(testSongId);
        assertNull(deletedSong, "删除后查询应该返回null");
        System.out.println("歌曲已删除,ID: " + testSongId);
    }

    /**
     * 测试删除不存在的歌曲
     */
    @Test
    @Order(10)
    @DisplayName("测试删除不存在的歌曲")
    void testDeleteNonExistent() {
        Boolean result = songService.delete(99999);
        assertFalse(result, "删除不存在的歌曲应该返回false");
    }
}

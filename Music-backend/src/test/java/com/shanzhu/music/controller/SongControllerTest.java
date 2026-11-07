package com.shanzhu.music.controller;

import com.shanzhu.music.BaseTest;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

/**
 * SongController 单元测试
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class SongControllerTest extends BaseTest {

    @Test
    @Order(1)
    @DisplayName("测试添加歌曲")
    void testAddSong() throws Exception {
        // 创建一个模拟的文件对象
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test.mp3",
                "audio/mpeg",
                "test audio content".getBytes()
        );

        mockMvc.perform(MockMvcRequestBuilders.multipart("/song/add")
                .file(file)
                .param("singerId", "1")
                .param("name", "测试歌曲")
                .param("introduction", "这是一首测试歌曲")
                .param("pic", "/img/songPic/test.jpg")
                .param("lyric", "[00:00.00]测试歌词"))
                .andExpect(MockMvcResultMatchers.status().isOk());
    }

    @Test
    @Order(2)
    @DisplayName("测试根据歌手id查询歌曲")
    void testSongOfSingerId() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/song/singer/detail")
                .param("singerId", "1"))
                .andExpect(MockMvcResultMatchers.status().isOk());
    }

    @Test
    @Order(3)
    @DisplayName("测试修改歌曲")
    void testUpdateSong() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.post("/song/update")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("id", "1")
                .param("name", "修改后的歌曲名")
                .param("introduction", "修改后的简介")
                .param("lyric", "修改后的歌词"))
                .andExpect(MockMvcResultMatchers.status().isOk());
    }

    @Test
    @Order(4)
    @DisplayName("测试删除歌曲")
    void testDeleteSong() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.delete("/song/delete")
                .param("id", "1"))
                .andExpect(MockMvcResultMatchers.status().isOk());
    }

    @Test
    @Order(5)
    @DisplayName("测试更新歌曲图片")
    @Disabled("空壳用例：方法体内没有任何断言，此前仍被计为通过。需补齐断言后再启用。")
    void testUpdateSongPic() {
        // 文件上传测试需要真实文件系统支持,在单元测试环境中跳过
    }

    @Test
    @Order(6)
    @DisplayName("测试更新歌曲文件")
    @Disabled("空壳用例：方法体内没有任何断言，此前仍被计为通过。需补齐断言后再启用。")
    void testUpdateSongUrl() {
        // 文件上传测试需要真实文件系统支持,在单元测试环境中跳过
    }

    @Test
    @Order(7)
    @DisplayName("测试查询歌曲详情")
    void testDetail() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/song/detail")
                .param("songId", "1"))
                .andExpect(MockMvcResultMatchers.status().isOk());
    }

    @Test
    @Order(8)
    @DisplayName("测试根据歌曲名精确查询")
    void testSongOfSongName() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/song/songOfSongName")
                .param("songName", "泡沫"))
                .andExpect(MockMvcResultMatchers.status().isOk());
    }

    @Test
    @Order(9)
    @DisplayName("测试根据歌曲名模糊查询")
    void testLikeSongOfName() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/song/likeSongOfName")
                .param("songName", "泡沫"))
                .andExpect(MockMvcResultMatchers.status().isOk());
    }

    @Test
    @Order(10)
    @DisplayName("测试查询所有歌曲")
    void testAllSong() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/song/allSong"))
                .andExpect(MockMvcResultMatchers.status().isOk());
    }

    @Test
    @Order(11)
    @DisplayName("测试分页查询所有歌曲")
    void testAllSongByPage() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/song/allSongByPage")
                .param("pageNum", "1")
                .param("pageSize", "10"))
                .andExpect(MockMvcResultMatchers.status().isOk());
    }
}

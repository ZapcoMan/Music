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
    @Order(30)
    @DisplayName("测试上传可执行文件被拒绝")
    void testUpdateSongPic() throws Exception {
        // 只断言 200 的旧写法等于没测：本项目业务失败也返回 200。
        // 这里断言的是 #22 那批补上的扩展名白名单确实生效 ——
        // 校验在落盘之前，所以不会产生垃圾文件
        byte[] bytes = new byte[]{0x4d, 0x5a, 0x01, 0x02};
        mockMvc.perform(MockMvcRequestBuilders.multipart("/song/updateSongPic")
                .file(new MockMultipartFile("file", "evil.exe", "application/octet-stream", bytes))
                .param("id", "1"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.success").value(false));
    }


    @Test
    @Order(31)
    @DisplayName("测试上传可执行文件被拒绝")
    void testUpdateSongUrl() throws Exception {
        // 只断言 200 的旧写法等于没测：本项目业务失败也返回 200。
        // 这里断言的是 #22 那批补上的扩展名白名单确实生效 ——
        // 校验在落盘之前，所以不会产生垃圾文件
        byte[] bytes = new byte[]{0x4d, 0x5a, 0x01, 0x02};
        mockMvc.perform(MockMvcRequestBuilders.multipart("/song/updateSongUrl")
                .file(new MockMultipartFile("file", "evil.exe", "application/octet-stream", bytes))
                .param("id", "1"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.success").value(false));
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

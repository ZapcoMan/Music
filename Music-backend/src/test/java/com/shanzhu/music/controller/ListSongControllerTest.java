package com.shanzhu.music.controller;

import com.shanzhu.music.BaseTest;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

/**
 * ListSongController 单元测试
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ListSongControllerTest extends BaseTest {

    @Test
    @Order(1)
    @DisplayName("测试给歌单添加歌曲")
    void testAddListSong() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.post("/listSong/add")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("songId", "1")
                .param("songListId", "1"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.success").exists());
    }

    @Test
    @Order(2)
    @DisplayName("测试根据歌单id查询歌曲")
    void testDetail() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/listSong/detail")
                .param("songListId", "1"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.success").exists());
    }

    @Test
    @Order(3)
    @DisplayName("测试删除歌单里的歌曲")
    void testDelete() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.delete("/listSong/delete")
                .param("songId", "1")
                .param("songListId", "1"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.success").exists());
    }
}

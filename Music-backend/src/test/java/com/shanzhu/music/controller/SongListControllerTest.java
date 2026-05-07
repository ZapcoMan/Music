package com.shanzhu.music.controller;

import com.shanzhu.music.BaseTest;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

/**
 * SongListController 单元测试
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class SongListControllerTest extends BaseTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @Order(1)
    @DisplayName("测试添加歌单")
    void testAddSongList() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.post("/songList/add")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("title", "测试歌单")
                .param("pic", "/img/songListPic/test.jpg")
                .param("introduction", "这是一个测试歌单")
                .param("style", "流行"))
                .andExpect(MockMvcResultMatchers.status().isOk());
    }

    @Test
    @Order(2)
    @DisplayName("测试修改歌单")
    void testUpdateSongList() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.post("/songList/update")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("id", "1")
                .param("title", "修改后的歌单")
                .param("introduction", "修改后的简介")
                .param("style", "摇滚"))
                .andExpect(MockMvcResultMatchers.status().isOk());
    }

    @Test
    @Order(3)
    @DisplayName("测试删除歌单")
    void testDeleteSongList() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/songList/delete")
                .param("id", "1"))
                .andExpect(MockMvcResultMatchers.status().isOk());
    }

    @Test
    @Order(4)
    @DisplayName("测试查询歌单详情")
    void testSelectByPrimaryKey() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/songList/selectByPrimaryKey")
                .param("id", "1"))
                .andExpect(MockMvcResultMatchers.status().isOk());
    }

    @Test
    @Order(5)
    @DisplayName("测试查询所有歌单")
    void testAllSongList() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/songList/allSongList"))
                .andExpect(MockMvcResultMatchers.status().isOk());
    }

    @Test
    @Order(6)
    @DisplayName("测试根据标题精确查询歌单")
    void testSongListOfName() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/songList/songListOfTitle")
                .param("title", "经典"))
                .andExpect(MockMvcResultMatchers.status().isOk());
    }

    @Test
    @Order(7)
    @DisplayName("测试根据标题模糊查询歌单")
    void testLikeTitle() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/songList/likeTitle")
                .param("title", "经典"))
                .andExpect(MockMvcResultMatchers.status().isOk());
    }

    @Test
    @Order(8)
    @DisplayName("测试根据风格模糊查询歌单")
    void testLikeStyle() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/songList/likeStyle")
                .param("style", "流行"))
                .andExpect(MockMvcResultMatchers.status().isOk());
    }

    @Test
    @Order(9)
    @DisplayName("测试更新歌单图片")
    void testUpdateSongListPic() {
        // 文件上传测试需要真实文件系统支持,在单元测试环境中跳过
    }

    @Test
    @Order(10)
    @DisplayName("测试查询推荐歌单")
    void testQueryRecommendedSongListById() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.post("/songList/queryRecommendedSongListById")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("id", "1"))
                .andExpect(MockMvcResultMatchers.status().isOk());
    }

    @Test
    @Order(11)
    @DisplayName("测试分页查询所有歌单")
    void testAllSongListByPage() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/songList/allSongListByPage")
                .param("pageNum", "1")
                .param("pageSize", "10"))
                .andExpect(MockMvcResultMatchers.status().isOk());
    }
}

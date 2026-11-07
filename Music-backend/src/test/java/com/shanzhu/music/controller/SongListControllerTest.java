package com.shanzhu.music.controller;

import com.shanzhu.music.BaseTest;
import com.shanzhu.music.entity.po.SongList;
import com.shanzhu.music.service.SongListService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * SongListController 单元测试
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class SongListControllerTest extends BaseTest {

    @Autowired
    private SongListService songListService;

    /**
     * 本类自己新增的那一行歌单的标题。
     * 修改与删除只针对它，绝不碰 schema.sql 的种子行 id=1——
     * 种子行被删掉之后，任何 INNER JOIN song_list 的查询都会变空，
     * 而这类破坏只在测试类顺序变化时才暴露：surefire 默认按文件系统顺序跑，
     * Windows 与 Linux 的目录枚举顺序不同，于是本地常绿、CI 常红。
     */
    private static final String PROBE_TITLE = "单测专用歌单（勿改此标题）";

    private Integer probeId() {
        List<SongList> found = songListService.songListOfTitle(PROBE_TITLE);
        return found == null || found.isEmpty() ? null : found.get(0).getId();
    }

    @Test
    @Order(1)
    @DisplayName("测试添加歌单")
    void testAddSongList() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.post("/songList/add")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("title", PROBE_TITLE)
                .param("pic", "/img/songListPic/test.jpg")
                .param("introduction", "这是一个测试歌单")
                .param("style", "流行"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.success").exists());

        Assertions.assertNotNull(probeId(), "新增的歌单应能按标题查回，后续用例只操作这一行");
    }

    @Test
    @Order(2)
    @DisplayName("测试修改歌单")
    void testUpdateSongList() throws Exception {
        Integer id = probeId();
        assertNotNull(id);
        mockMvc.perform(MockMvcRequestBuilders.post("/songList/update")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("id", String.valueOf(id))
                .param("title", PROBE_TITLE)
                .param("introduction", "修改后的简介")
                .param("style", "摇滚"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.success").exists());
    }

    @Test
    @Order(3)
    @DisplayName("测试删除歌单：只删自己新增的那一行")
    void testDeleteSongList() throws Exception {
        Integer id = probeId();
        assertNotNull(id);
        mockMvc.perform(MockMvcRequestBuilders.delete("/songList/delete")
                .param("id", String.valueOf(id)))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.success").exists());

        Assertions.assertNull(songListService.selectByPrimaryKey(id),
                "删除后这一行不应再存在");
        // 种子行必须还活着：它是其它用例 INNER JOIN 的依赖
        Assertions.assertNotNull(songListService.selectByPrimaryKey(1),
                "删除用例不得伤及 schema.sql 的种子歌单 id=1");
    }

    @Test
    @Order(4)
    @DisplayName("测试查询歌单详情")
    void testSelectByPrimaryKey() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/songList/selectByPrimaryKey")
                .param("id", "1"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.success").exists());
    }

    @Test
    @Order(5)
    @DisplayName("测试查询所有歌单")
    void testAllSongList() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/songList/allSongList"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.success").exists());
    }

    @Test
    @Order(6)
    @DisplayName("测试根据标题精确查询歌单")
    void testSongListOfName() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/songList/songListOfTitle")
                .param("title", "经典"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.success").exists());
    }

    @Test
    @Order(7)
    @DisplayName("测试根据标题模糊查询歌单")
    void testLikeTitle() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/songList/likeTitle")
                .param("title", "经典"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.success").exists());
    }

    @Test
    @Order(8)
    @DisplayName("测试根据风格模糊查询歌单")
    void testLikeStyle() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/songList/likeStyle")
                .param("style", "流行"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.success").exists());
    }


    @Test
    @Order(30)
    @DisplayName("测试上传可执行文件被拒绝")
    void testUpdateSongListPic() throws Exception {
        // 只断言 200 的旧写法等于没测：本项目业务失败也返回 200。
        // 这里断言的是 #22 那批补上的扩展名白名单确实生效 ——
        // 校验在落盘之前，所以不会产生垃圾文件
        byte[] bytes = new byte[]{0x4d, 0x5a, 0x01, 0x02};
        mockMvc.perform(MockMvcRequestBuilders.multipart("/songList/updateSongListPic")
                .file(new MockMultipartFile("file", "evil.exe", "application/octet-stream", bytes))
                .param("id", "1"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.success").value(false));
    }

    @Test
    @Order(10)
    @DisplayName("测试查询推荐歌单")
    void testQueryRecommendedSongListById() throws Exception {
        // 推荐歌单是用户端功能，按角色只能由 consumer 调用
        consumerMvc.perform(MockMvcRequestBuilders.post("/songList/queryRecommendedSongListById")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("id", "1"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.success").value(true))
                .andExpect(MockMvcResultMatchers.jsonPath("$.data").isArray());
    }

    @Test
    @Order(11)
    @DisplayName("测试分页查询所有歌单")
    void testAllSongListByPage() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/songList/allSongListByPage")
                .param("pageNum", "1")
                .param("pageSize", "10"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.success").exists());
    }
}

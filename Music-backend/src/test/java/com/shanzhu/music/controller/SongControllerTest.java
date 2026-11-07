package com.shanzhu.music.controller;

import com.shanzhu.music.BaseTest;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

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

        mockMvc.perform(MockMvcRequestBuilders.multipart("/song/manage/add")
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
        mockMvc.perform(MockMvcRequestBuilders.get("/song/manage/singer/detail")
                .param("singerId", "1"))
                .andExpect(MockMvcResultMatchers.status().isOk());
    }

    @Test
    @Order(3)
    @DisplayName("测试修改歌曲")
    void testUpdateSong() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.post("/song/manage/update")
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
        mockMvc.perform(MockMvcRequestBuilders.delete("/song/manage/delete")
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
        mockMvc.perform(MockMvcRequestBuilders.multipart("/song/manage/updateSongPic")
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
        mockMvc.perform(MockMvcRequestBuilders.multipart("/song/manage/updateSongUrl")
                .file(new MockMultipartFile("file", "evil.exe", "application/octet-stream", bytes))
                .param("id", "1"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.success").value(false));
    }

    @Test
    @Order(7)
    @DisplayName("测试查询歌曲详情")
    void testDetail() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/song/manage/detail")
                .param("songId", "1"))
                .andExpect(MockMvcResultMatchers.status().isOk());
    }

    @Test
    @Order(8)
    @DisplayName("测试根据歌曲名精确查询")
    void testSongOfSongName() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/song/manage/songOfSongName")
                .param("songName", "泡沫"))
                .andExpect(MockMvcResultMatchers.status().isOk());
    }

    @Test
    @Order(9)
    @DisplayName("测试根据歌曲名模糊查询")
    void testLikeSongOfName() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/song/manage/likeSongOfName")
                .param("songName", "泡沫"))
                .andExpect(MockMvcResultMatchers.status().isOk());
    }

    @Test
    @Order(10)
    @DisplayName("测试查询所有歌曲")
    void testAllSong() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/song/manage/allSong"))
                .andExpect(MockMvcResultMatchers.status().isOk());
    }

    @Test
    @Order(11)
    @DisplayName("测试分页查询所有歌曲")
    void testAllSongByPage() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/song/manage/allSongByPage")
                .param("pageNum", "1")
                .param("pageSize", "10"))
                .andExpect(MockMvcResultMatchers.status().isOk());
    }

    /**
     * 匿名添加歌曲必须被拒。
     *
     * 这条针对的是此前真实存在的洞：WebMvcConfig 为放行静态音频而排除了 /song/**，
     * 与静态资源同前缀的 add/update/delete/updateSongPic/updateSongUrl 因此全部
     * 处于未认证状态。基类的 mockMvc 预置了管理员令牌，这类"鉴权缺失"缺陷永远不会
     * 让任何用例变红，所以此处现场构造一个不带默认请求的 MockMvc。
     *
     * 断 401 而非 404 还顺带证明 /song/manage/** 命中的是 Controller 而不是静态资源
     * 处理器：若被资源映射抢走，这里会是 404。
     */
    @Test
    @Order(40)
    @DisplayName("测试未登录添加歌曲被拒绝")
    void testAddSongWithoutTokenIsRejected() throws Exception {
        MockMvc anonymous = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        MockMultipartFile file = new MockMultipartFile(
                "file", "anonymous.mp3", "audio/mpeg", "x".getBytes());

        anonymous.perform(MockMvcRequestBuilders.multipart("/song/manage/add")
                        .file(file)
                        .param("singerId", "1")
                        .param("name", "匿名上传"))
                .andExpect(MockMvcResultMatchers.status().isUnauthorized());
    }

    /**
     * 静态音频前缀必须仍然免鉴权，否则 <audio> 标签拿不到音频（该标签无法携带
     * Authorization 头），库里 50+ 条无扩展名的历史 URL 也会一起失效。
     * 这里请求的文件在测试环境不存在，预期是资源层的 404 而不是鉴权层的 401；
     * 断 404 正是为了证明请求穿过了拦截器，白名单收窄没把静态文件一起关进去。
     */
    @Test
    @Order(41)
    @DisplayName("测试静态音频路径不被鉴权拦截")
    void testStaticSongFileIsNotBlockedByAuth() throws Exception {
        MockMvc anonymous = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();

        anonymous.perform(MockMvcRequestBuilders.get("/song/1715868205989"))
                .andExpect(MockMvcResultMatchers.status().isNotFound());
    }

    /**
     * 消费者令牌调用管理写接口必须被拒。
     *
     * admin 与 consumer 共用同一个签名密钥，"令牌合法"并不等于"可以做这件事"：
     * 补上 @RequireUserType("admin") 之前，任何一个已登录用户都能增删改曲库。
     * 基类预置的是管理员令牌，这条走的是角色不匹配分支，返回 403 而非 401。
     */
    @Test
    @Order(42)
    @DisplayName("测试消费者令牌调用管理写接口被拒绝")
    void testAddSongWithConsumerTokenIsForbidden() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "not-mine.mp3", "audio/mpeg", "x".getBytes());

        mockMvc.perform(MockMvcRequestBuilders.multipart("/song/manage/add")
                        .file(file)
                        .param("singerId", "1")
                        .param("name", "越权添加")
                        .header("Authorization", consumerToken()))
                .andExpect(MockMvcResultMatchers.status().isForbidden());
    }
}

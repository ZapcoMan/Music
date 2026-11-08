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

    /**
     * 仅用于取一首确实存在的歌曲 id，见 anExistingSongId()
     */
    @org.springframework.beans.factory.annotation.Autowired
    private com.shanzhu.music.service.SongService songService;

    /**
     * @Order(1) 添加成功后记下的歌曲 id 与歌手 id。
     * 本类原先多处写死 id=1 或 songId=1：曲库是空的，id 从自增开始，
     * 写死的值要么指向别的用例刚建/刚删的行，要么指向根本不存在的行。
     */
    private static Integer addedSongId;
    private static final int ADDED_SINGER_ID = 1;

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

        mockMvc.perform(MockMvcRequestBuilders.multipart("/api/v1/songs")
                .file(file)
                .param("singerId", "1")
                .param("name", "测试歌曲")
                .param("introduction", "这是一首测试歌曲")
                .param("pic", "/img/songPic/test.jpg")
                .param("lyric", "[00:00.00]测试歌词"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.success").value(true))
                .andExpect(MockMvcResultMatchers.jsonPath("$.data.avator").exists());

        addedSongId = songOfName("测试歌曲").getId();
    }

    /**
     * 按歌名精确取回一行，供各用例引用真实 id
     */
    private com.shanzhu.music.entity.po.Song songOfName(String name) {
        return songService.allSong().stream()
                .filter(song -> name.equals(song.getName()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("测试前提：曲库中应有《" + name + "》"));
    }

    @Test
    @Order(2)
    @DisplayName("测试查询某位歌手的歌曲")
    void testSongOfSingerId() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/singers/" + ADDED_SINGER_ID + "/songs"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.success").value(true))
                .andExpect(MockMvcResultMatchers.jsonPath("$.data").isArray())
                // 只有断"包含刚建的那首"才真正验证了按歌手过滤这件事；
                // 只断 isArray 的话，返回空数组也算通过
                .andExpect(MockMvcResultMatchers.jsonPath("$.data[?(@.id == " + addedSongId + ")]").isNotEmpty());
    }

    @Test
    @Order(3)
    @DisplayName("测试修改歌曲")
    void testUpdateSong() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.patch("/api/v1/songs/" + addedSongId)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("name", "修改后的歌曲名")
                .param("introduction", "修改后的简介")
                .param("lyric", "修改后的歌词"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.success").value(true));

        // 只断 success 的话，"改到别的行"或"一个字段都没写进去"都测不出来
        com.shanzhu.music.entity.po.Song updated = songService.selectByPrimaryKey(addedSongId);
        org.junit.jupiter.api.Assertions.assertEquals("修改后的歌曲名", updated.getName(), "歌名应已更新");
        org.junit.jupiter.api.Assertions.assertEquals("修改后的简介", updated.getIntroduction(), "简介应已更新");
    }

    @Test
    @Order(4)
    @DisplayName("测试删除歌曲")
    void testDeleteSong() throws Exception {
        // 删除目标由本用例自己创建：原先写死 id=1，删的是碰巧存在（或根本不存在）的行，
        // 而断言只有 $.data exists —— 业务成功与"data=false 也算存在"在它眼里没区别
        MockMultipartFile file = new MockMultipartFile(
                "file", "to-delete.mp3", "audio/mpeg", "x".getBytes());
        mockMvc.perform(MockMvcRequestBuilders.multipart("/api/v1/songs")
                .file(file)
                .param("singerId", String.valueOf(ADDED_SINGER_ID))
                .param("name", "待删除歌曲"));
        Integer doomedId = songOfName("待删除歌曲").getId();

        mockMvc.perform(MockMvcRequestBuilders.delete("/api/v1/songs/" + doomedId))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.data").value(true));

        org.junit.jupiter.api.Assertions.assertNull(songService.selectByPrimaryKey(doomedId),
                "删除后应查不到这首歌曲");
    }


    @Test
    @Order(30)
    @DisplayName("测试上传可执行文件被拒绝")
    void testUpdateSongPic() throws Exception {
        // 只断言 200 的旧写法等于没测：本项目业务失败也返回 200。
        // 这里断言的是 #22 那批补上的扩展名白名单确实生效 ——
        // 校验在落盘之前，所以不会产生垃圾文件
        byte[] bytes = new byte[]{0x4d, 0x5a, 0x01, 0x02};
        mockMvc.perform(MockMvcRequestBuilders.multipart("/api/v1/songs/1/picture")
                .file(new MockMultipartFile("file", "evil.exe", "application/octet-stream", bytes))
                )
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
        mockMvc.perform(MockMvcRequestBuilders.multipart("/api/v1/songs/1/audio")
                .file(new MockMultipartFile("file", "evil.exe", "application/octet-stream", bytes))
                )
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.success").value(false));
    }

    @Test
    @Order(7)
    @DisplayName("测试查询歌曲详情")
    void testDetail() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/songs/" + anExistingSongId()))
                .andExpect(MockMvcResultMatchers.status().isOk())
                // 本用例原先写死 songId=1 并只断 200，而 @Order(4) 的删除用例已经把 id=1
                // 删掉了——它其实一直在查一首不存在的歌，却从没红过。
                // 现在改为取一首确实存在的歌曲，并断言载荷是对象、id 与请求一致，
                // 这样 detail 的返回形状才真的被测到。
                .andExpect(MockMvcResultMatchers.jsonPath("$.data").isMap())
                .andExpect(MockMvcResultMatchers.jsonPath("$.data.id")
                        .value(anExistingSongId()));
    }

    /**
     * 取当前库里仍存在的一首歌曲 id，避免用例依赖固定种子行的存活状态。
     */
    private Integer anExistingSongId() {
        java.util.List<com.shanzhu.music.entity.po.Song> songs = songService.allSong();
        org.junit.jupiter.api.Assertions.assertFalse(songs.isEmpty(), "测试前提：曲库中应有歌曲");
        return songs.get(0).getId();
    }

    @Test
    @Order(8)
    @DisplayName("测试根据歌曲名精确查询")
    void testSongOfSongName() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/songs")
                .param("songName", "修改后的歌曲名"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.data").isArray())
                // 查的是本类 @Order(3) 改过名的那一首：只断 isArray 时返回空数组也算通过
                .andExpect(MockMvcResultMatchers
                        .jsonPath("$.data[?(@.name == '修改后的歌曲名')]").isNotEmpty());
    }

    @Test
    @Order(9)
    @DisplayName("测试根据关键词模糊搜索歌曲")
    void testLikeSongOfName() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/songs")
                .param("keyword", "修改后"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.data").isArray())
                .andExpect(MockMvcResultMatchers
                        .jsonPath("$.data[?(@.name == '修改后的歌曲名')]").isNotEmpty());
    }

    @Test
    @Order(10)
    @DisplayName("测试查询所有歌曲")
    void testAllSong() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/songs"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.data").isArray());
    }

    @Test
    @Order(11)
    @DisplayName("测试分页查询所有歌曲")
    void testAllSongByPage() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/songs/page")
                .param("pageNum", "1")
                .param("pageSize", "10"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.data.list").isArray())
                .andExpect(MockMvcResultMatchers.jsonPath("$.data.total").isNumber());
    }

    /**
     * 分页上限要真的夹得住。
     *
     * 这条用例最初是为了验 pagehelper.max-page-size，跑出来的是
     * expected:<100> but was:<5000>——配置层根本夹不住 startPage 起页这条路
     * （PageHelper 只在 setPageSize 里应用 maxPageSize），上限因此改由
     * PageLimits 在调 startPage 之前夹。
     *
     * 留在这里的意义：防止有人把那条配置当成有效防护，也防止日后新增分页入口
     * 时忘了过 clamp。
     *
     * 断的是夹完之后的 pageSize 元数据，不是返回条数——种子数据不足 100 行，
     * 断条数在任何实现下都会通过。
     */
    @Test
    @Order(12)
    @DisplayName("测试分页大小被上限夹断")
    void testPageSizeCappedByMaxPageSize() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/songs/page")
                        .param("pageNum", "1")
                        .param("pageSize", "5000"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.data.pageSize").value(100));
    }

    /**
     * 匿名添加歌曲必须被拒。
     *
     * 这条针对的是此前真实存在的洞：WebMvcConfig 为放行静态音频而排除了 /song/**，
     * 与静态资源同前缀的 add/update/delete/updateSongPic/updateSongUrl 因此全部
     * 处于未认证状态。基类的 mockMvc 预置了管理员令牌，这类"鉴权缺失"缺陷永远不会
     * 让任何用例变红，所以此处现场构造一个不带默认请求的 MockMvc。
     *
     * 断 401 而非 404 还顺带证明 /api/v1/songs 命中的是 Controller 而不是静态资源
     * 处理器：若被资源映射抢走，这里会是 404。
     */
    @Test
    @Order(40)
    @DisplayName("测试未登录添加歌曲被拒绝")
    void testAddSongWithoutTokenIsRejected() throws Exception {
        MockMvc anonymous = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        MockMultipartFile file = new MockMultipartFile(
                "file", "anonymous.mp3", "audio/mpeg", "x".getBytes());

        anonymous.perform(MockMvcRequestBuilders.multipart("/api/v1/songs")
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

        mockMvc.perform(MockMvcRequestBuilders.multipart("/api/v1/songs")
                        .file(file)
                        .param("singerId", "1")
                        .param("name", "越权添加")
                        .header("Authorization", consumerToken()))
                .andExpect(MockMvcResultMatchers.status().isForbidden());
    }
}

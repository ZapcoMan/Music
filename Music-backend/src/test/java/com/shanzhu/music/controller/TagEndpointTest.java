package com.shanzhu.music.controller;

import com.shanzhu.music.BaseTest;
import com.shanzhu.music.entity.po.Singer;
import com.shanzhu.music.entity.po.Song;
import com.shanzhu.music.service.SingerService;
import com.shanzhu.music.service.SongService;
import com.shanzhu.music.service.TagService;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 标签七个端点资源化后的契约测试。
 *
 * 为什么需要这个类：这批接口此前一次都没被测过——AdminWriteAuthMatrixTest 只打在
 * 鉴权层（请求根本进不了业务方法），全仓检索也找不到任何一处用例命中过这些路径。
 * 路径改成资源式之后，"应用能启动"证明不了映射写对、更证明不了替换语义还在。
 *
 * 写入用例全部打在自建资源上并在末尾删除：种子行的标签是召回类用例的前提，
 * 不能拿去验证替换与清空。
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class TagEndpointTest extends BaseTest {

    private static final String TEST_SONG_NAME = "标签端点测试曲";
    private static final String TEST_SINGER_NAME = "标签端点测试歌手";
    private static final String OWN_TAG_NAME = "标签端点专用";

    /**
     * 本类自建资源的 id，在用例间传递
     */
    private static Integer testSongId;
    private static Integer testSingerId;

    /**
     * 只用于取回自建资源的真实 id、核对逗号串，以及绕开被测读路径核对中间表
     */
    @Autowired
    private SongService songService;

    @Autowired
    private SingerService singerService;

    @Autowired
    private TagService tagService;

    /**
     * 核对逗号串只能直接查库：SongMapper 的 selectByPrimaryKey/allSong 走
     * ResultMapWithBLOBs，其列清单里没有 tags，Java 侧读到的恒为 null
     * （SingerMapper 有该列）。绕开这一层才能验证"中间表与逗号串一次写定"。
     */
    @Autowired
    private JdbcTemplate jdbcTemplate;

    /**
     * 不带任何令牌的另一份实例：默认的 mockMvc 已预置管理员令牌，测不出未登录被拒
     */
    private MockMvc anonymous;

    @BeforeEach
    void setUpAnonymousMockMvc() {
        this.anonymous = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    }

    @Test
    @Order(1)
    @DisplayName("热门标签按数量上限返回，消费者令牌可读")
    void hotTagsRespectsLimit() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/tags/hot").param("limit", "5"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.success").value(true))
                .andExpect(MockMvcResultMatchers.jsonPath("$.data.length()").value(5))
                // 只断 isArray 的话，返回 5 个不相干的标签也算通过；按关联歌曲数倒序，
                // 种子里挂歌最多的"流行"必须在前几名里
                .andExpect(MockMvcResultMatchers.jsonPath("$.data[?(@.name == '流行')]").isNotEmpty());

        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/tags/hot").param("limit", "1"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.data.length()").value(1));

        consumerMvc.perform(MockMvcRequestBuilders.get("/api/v1/tags/hot"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.success").value(true));
    }

    @Test
    @Order(2)
    @DisplayName("读某首歌的标签取自 song_tag 中间表")
    void readSongTags() throws Exception {
        // 种子歌曲 1（七里香）在 schema.sql 里挂了 流行/抒情/中文
        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/songs/1/tags"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.success").value(true))
                .andExpect(MockMvcResultMatchers.jsonPath("$.data.length()").value(3))
                .andExpect(MockMvcResultMatchers.jsonPath("$.data[?(@ == '流行')]").isNotEmpty())
                .andExpect(MockMvcResultMatchers.jsonPath("$.data[?(@ == '抒情')]").isNotEmpty())
                .andExpect(MockMvcResultMatchers.jsonPath("$.data[?(@ == '中文')]").isNotEmpty());

        consumerMvc.perform(MockMvcRequestBuilders.get("/api/v1/songs/1/tags"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.data.length()").value(3));
    }

    @Test
    @Order(3)
    @DisplayName("读某位歌手的标签取自 singer_tag 中间表")
    void readSingerTags() throws Exception {
        // 种子歌手 1（周杰伦）挂 流行/中文
        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/singers/1/tags"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.success").value(true))
                .andExpect(MockMvcResultMatchers.jsonPath("$.data.length()").value(2))
                .andExpect(MockMvcResultMatchers.jsonPath("$.data[?(@ == '流行')]").isNotEmpty())
                .andExpect(MockMvcResultMatchers.jsonPath("$.data[?(@ == '中文')]").isNotEmpty());

        consumerMvc.perform(MockMvcRequestBuilders.get("/api/v1/singers/1/tags"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.data.length()").value(2));
    }

    @Test
    @Order(4)
    @DisplayName("按标签查歌曲返回带 matchCount 的歌曲")
    void songsByTags() throws Exception {
        // 抒情（tag id 6）只挂在七里香一首上，条数与命中对象都能钉死
        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/tags/songs")
                        .param("tagIds", "6").param("limit", "10"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.success").value(true))
                .andExpect(MockMvcResultMatchers.jsonPath("$.data.length()").value(1))
                .andExpect(MockMvcResultMatchers.jsonPath("$.data[0].id").value(1))
                // match_count / singer_id 这类下划线列要靠 map-underscore-to-camel-case
                // 才映射得进 TaggedSongVo。测试 profile 此前缺这一行（dev 与 prod 都有），
                // 于是它们恒为 null；配置已补齐，这里断实际命中数。
                .andExpect(MockMvcResultMatchers.jsonPath("$.data[0].matchCount").value(1));

        // 流行（tag id 1）覆盖三首种子歌曲，返回的正是这三首而不是别的行
        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/tags/songs").param("tagIds", "1"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.data.length()").value(3))
                .andExpect(MockMvcResultMatchers.jsonPath("$.data[?(@.id == 2)]").isNotEmpty());

        // 空 tagIds 走接口自己的拒绝分支，而不是让 SQL 的 IN () 报语法错
        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/tags/songs").param("tagIds", ""))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.success").value(false));
    }

    @Test
    @Order(5)
    @DisplayName("准备一首自建歌曲供写入用例使用")
    void createSongForWriteCases() throws Exception {
        MockMultipartFile mp3 = new MockMultipartFile(
                "file", "tag-endpoint.mp3", "audio/mpeg", "tag endpoint content".getBytes());

        mockMvc.perform(MockMvcRequestBuilders.multipart("/api/v1/songs")
                        .file(mp3)
                        .param("singerId", "1")
                        .param("name", TEST_SONG_NAME))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.success").value(true));

        testSongId = songOfName(TEST_SONG_NAME).getId();
        assertNotNull(testSongId, "新建歌曲应拿到自增 id");
    }

    @Test
    @Order(6)
    @DisplayName("PUT 歌曲标签是整组替换，中间表与逗号串一次写定")
    void putSongTagsReplacesWholeSet() throws Exception {
        assertNotNull(testSongId, "需要先建出测试歌曲");

        mockMvc.perform(MockMvcRequestBuilders.put("/api/v1/songs/" + testSongId + "/tags")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[\"流行\",\"摇滚\"]"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.success").value(true))
                // 载荷是生效的标签数量
                .andExpect(MockMvcResultMatchers.jsonPath("$.data").value(2));

        // 第二次只提交一个：若实现是追加，这里会读到三个
        mockMvc.perform(MockMvcRequestBuilders.put("/api/v1/songs/" + testSongId + "/tags")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[\"民谣\"]"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.data").value(1));

        assertEquals(List.of("民谣"), tagService.songTagNames(testSongId),
                "替换后标签应是新提交的那一组，旧的不留");

        // 双轨一致：中间表改了、song.tags 逗号串没跟上，正是本接口初版犯过的错
        assertEquals("民谣", storedSongTags(), "逗号串应与中间表同步成同一组");
    }

    @Test
    @Order(7)
    @DisplayName("非法标签与缺省请求都被拒绝且不改坏现状")
    void invalidTagsLeaveStateUntouched() throws Exception {
        assertNotNull(testSongId, "需要先建出测试歌曲");

        mockMvc.perform(MockMvcRequestBuilders.put("/api/v1/songs/" + testSongId + "/tags")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[\"字典里没有的标签\"]"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.success").value(false))
                .andExpect(MockMvcResultMatchers.jsonPath("$.message")
                        .value(Matchers.containsString("标签不存在")));
        assertEquals(List.of("民谣"), tagService.songTagNames(testSongId),
                "非法输入不该留下半改状态");

        // 不带请求体是"没打算改标签"，不能当成"清空"
        mockMvc.perform(MockMvcRequestBuilders.put("/api/v1/songs/" + testSongId + "/tags"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.success").value(false));
        assertEquals(List.of("民谣"), tagService.songTagNames(testSongId), "缺省请求不该动标签");
    }

    @Test
    @Order(8)
    @DisplayName("空数组表示清空歌曲标签")
    void emptyArrayClearsSongTags() throws Exception {
        assertNotNull(testSongId, "需要先建出测试歌曲");

        mockMvc.perform(MockMvcRequestBuilders.put("/api/v1/songs/" + testSongId + "/tags")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[]"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.success").value(true))
                .andExpect(MockMvcResultMatchers.jsonPath("$.data").value(0));

        assertTrue(tagService.songTagNames(testSongId).isEmpty(), "空数组应把中间表清空");
        assertEquals("", storedSongTags(), "逗号串同样要清空");
    }

    @Test
    @Order(9)
    @DisplayName("PUT 歌手标签同样是整组替换")
    void putSingerTagsReplacesWholeSet() throws Exception {
        String createRequest = "{" +
                "\"name\":\"" + TEST_SINGER_NAME + "\"," +
                "\"sex\":1," +
                "\"location\":\"测试地区\"" +
                "}";
        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/singers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createRequest))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.success").value(true));

        List<Singer> created = singerService.singerOfName("%" + TEST_SINGER_NAME + "%");
        assertFalse(created.isEmpty(), "新建歌手应能查回");
        testSingerId = created.get(0).getId();

        mockMvc.perform(MockMvcRequestBuilders.put("/api/v1/singers/" + testSingerId + "/tags")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[\"伤感\",\"英文\"]"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.success").value(true))
                .andExpect(MockMvcResultMatchers.jsonPath("$.data").value(2));

        // 只提交一个：追加实现会在这里读到两个
        mockMvc.perform(MockMvcRequestBuilders.put("/api/v1/singers/" + testSingerId + "/tags")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[\"摇滚\"]"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.data").value(1));

        assertEquals(List.of("摇滚"), tagService.singerTagNames(testSingerId), "歌手标签应被整体换掉");
        assertEquals("摇滚", storedSingerTags(), "singer.tags 逗号串应与 singer_tag 中间表同步");

        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/singers/" + testSingerId + "/tags"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.data.length()").value(1))
                .andExpect(MockMvcResultMatchers.jsonPath("$.data[?(@ == '摇滚')]").isNotEmpty());
    }

    @Test
    @Order(10)
    @DisplayName("POST /api/v1/tags 写入字典后新标签即可挂到歌曲上")
    void batchAddMakesTagUsable() throws Exception {
        assertNotNull(testSongId, "需要先建出测试歌曲");

        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/tags")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[{\"name\":\"" + OWN_TAG_NAME + "\",\"type\":0}]"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.success").value(true))
                // 返回的是提交条数：重复项被 INSERT IGNORE 忽略时它不能当"新增条数"
                .andExpect(MockMvcResultMatchers.jsonPath("$.data").value(1));

        // 字典里有了它，替换标签才不会被判非法——这一条同时把两个端点的耦合钉住
        mockMvc.perform(MockMvcRequestBuilders.put("/api/v1/songs/" + testSongId + "/tags")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[\"" + OWN_TAG_NAME + "\"]"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.success").value(true))
                .andExpect(MockMvcResultMatchers.jsonPath("$.data").value(1));
        assertEquals(List.of(OWN_TAG_NAME), tagService.songTagNames(testSongId), "新标签应已挂到歌曲上");

        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/tags")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[{\"name\":\"   \"}]"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.success").value(false));
    }

    @Test
    @Order(11)
    @DisplayName("标签写入只允许管理员，读取两类令牌都可用")
    void writeEndpointsAreAdminOnly() throws Exception {
        assertNotNull(testSongId, "需要先建出测试歌曲");
        assertNotNull(testSingerId, "需要先建出测试歌手");

        // 消费者令牌要拿到 403 而不是 404：路径写错时命中的是静态资源兜底，角色校验压根不触发
        consumerMvc.perform(MockMvcRequestBuilders.put("/api/v1/songs/" + testSongId + "/tags")
                        .contentType(MediaType.APPLICATION_JSON).content("[]"))
                .andExpect(MockMvcResultMatchers.status().isForbidden());
        consumerMvc.perform(MockMvcRequestBuilders.put("/api/v1/singers/" + testSingerId + "/tags")
                        .contentType(MediaType.APPLICATION_JSON).content("[]"))
                .andExpect(MockMvcResultMatchers.status().isForbidden());
        consumerMvc.perform(MockMvcRequestBuilders.post("/api/v1/tags")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[{\"name\":\"越权标签\"}]"))
                .andExpect(MockMvcResultMatchers.status().isForbidden());

        anonymous.perform(MockMvcRequestBuilders.post("/api/v1/tags")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[{\"name\":\"匿名标签\"}]"))
                .andExpect(MockMvcResultMatchers.status().isUnauthorized());
        anonymous.perform(MockMvcRequestBuilders.get("/api/v1/tags/hot"))
                .andExpect(MockMvcResultMatchers.status().isUnauthorized());

        // 读接口对消费者开放：若被收窄成只许 admin，这一条会红
        consumerMvc.perform(MockMvcRequestBuilders.get("/api/v1/songs/" + testSongId + "/tags"))
                .andExpect(MockMvcResultMatchers.status().isOk());
    }

    @Test
    @Order(12)
    @DisplayName("清理自建资源，不给种子数据留孤儿关联")
    void cleanUpOwnedResources() throws Exception {
        // 测试库的 song_tag/singer_tag 没有 ON DELETE CASCADE（真实库有），
        // 所以删资源前先把标签清空，否则留下指向已删歌曲的关联行，selectHotTags 的计数会被带偏
        if (testSongId != null) {
            mockMvc.perform(MockMvcRequestBuilders.put("/api/v1/songs/" + testSongId + "/tags")
                            .contentType(MediaType.APPLICATION_JSON).content("[]"))
                    .andExpect(MockMvcResultMatchers.jsonPath("$.success").value(true));
            mockMvc.perform(MockMvcRequestBuilders.delete("/api/v1/songs/" + testSongId))
                    .andExpect(MockMvcResultMatchers.status().isOk());
            assertNull(songService.selectByPrimaryKey(testSongId), "测试歌曲应已删除");
        }
        if (testSingerId != null) {
            mockMvc.perform(MockMvcRequestBuilders.put("/api/v1/singers/" + testSingerId + "/tags")
                            .contentType(MediaType.APPLICATION_JSON).content("[]"))
                    .andExpect(MockMvcResultMatchers.jsonPath("$.success").value(true));
            mockMvc.perform(MockMvcRequestBuilders.delete("/api/v1/singers/" + testSingerId))
                    .andExpect(MockMvcResultMatchers.status().isOk());
            assertNull(singerService.selectByPrimaryKey(testSingerId), "测试歌手应已删除");
        }
    }

    /**
     * 按名字精确取回本类建出的那一首，避免写死自增 id
     */
    private Song songOfName(String name) {
        return songService.allSong().stream()
                .filter(song -> name.equals(song.getName()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("测试前提：曲库中应有《" + name + "》"));
    }

    /**
     * 直接读库里的 song.tags，绕开读不到该列的 Java 查询路径
     */
    private String storedSongTags() {
        return jdbcTemplate.queryForObject("SELECT tags FROM song WHERE id = ?",
                String.class, testSongId);
    }

    /**
     * 同上，取 singer.tags
     */
    private String storedSingerTags() {
        return jdbcTemplate.queryForObject("SELECT tags FROM singer WHERE id = ?",
                String.class, testSingerId);
    }
}

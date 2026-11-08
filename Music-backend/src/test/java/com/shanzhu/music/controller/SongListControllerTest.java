package com.shanzhu.music.controller;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.shanzhu.music.BaseTest;
import com.shanzhu.music.entity.po.SongList;
import com.shanzhu.music.service.SongListService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * SongListController 单元测试
 *
 * 路径按资源式契约写死（/api/v1/song-lists…）。本项目业务失败也返回 200，
 * 所以每条用例都落到响应 data 或数据库真实状态，不再留"只断 200 / $.success exists"的空壳。
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

    /** 修改用例写入的简介与风格，后续用例据此确认改的就是这一行 */
    private static final String UPDATED_INTRODUCTION = "修改后的简介";
    private static final String UPDATED_STYLE = "摇滚";
    private static final String PROBE_PIC = "/img/songListPic/test.jpg";

    /**
     * testAddSongList 建出来的行，其余用例只认这个 id。
     * 旧写法每条用例各自按标题重查一次，查空时用例照样绿——断言的是"不存在的东西不存在"。
     */
    private static Integer probeId;

    /**
     * 不带任何令牌的实例：基类 mockMvc 预置了管理员令牌，用它测不出未登录。
     */
    private MockMvc anonymousMvc;

    @BeforeEach
    void setUpAnonymousMockMvc() {
        this.anonymousMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    }

    private static Integer requireProbeId() {
        assertNotNull(probeId, "需要一个 testAddSongList 建出来的歌单");
        return probeId;
    }

    /** 按标题精确回查本类新增的那一行 */
    private SongList reloadProbe() {
        List<SongList> found = songListService.songListOfTitle(PROBE_TITLE);
        return found == null || found.isEmpty() ? null : found.get(0);
    }

    /** 取出响应里的 data 数组 */
    private static JSONArray dataOf(MvcResult result) throws Exception {
        return JSON.parseObject(result.getResponse().getContentAsString(StandardCharsets.UTF_8)).getJSONArray("data");
    }

    /**
     * data 里是否含指定 id 的歌单。
     * 用遍历而不是 JsonPath 过滤器，因为过滤器对中文值与数值的比较
     * 在不同 JsonPath 实现间行为不一致，这里只想要一个稳定判断。
     */
    private static boolean containsId(JSONArray data, Integer id) {
        for (int i = 0; i < data.size(); i++) {
            JSONObject item = data.getJSONObject(i);
            if (item != null && id.equals(item.getInteger("id"))) {
                return true;
            }
        }
        return false;
    }

    @Test
    @Order(1)
    @DisplayName("测试添加歌单：POST /api/v1/song-lists")
    void testAddSongList() throws Exception {
        MvcResult result = mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/song-lists")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("title", PROBE_TITLE)
                .param("pic", PROBE_PIC)
                .param("introduction", "这是一个测试歌单")
                .param("style", "流行"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.code").value(20000))
                .andExpect(MockMvcResultMatchers.jsonPath("$.success").value(true))
                .andReturn();

        assertNotNull(JSON.parseObject(result.getResponse().getContentAsString(StandardCharsets.UTF_8)).getString("message"),
                "写入成功应带回业务消息");

        SongList created = reloadProbe();
        assertNotNull(created, "新增的歌单应能按标题查回，后续用例只操作这一行");
        probeId = created.getId();
        assertEquals(PROBE_PIC, created.getPic(), "新增时提交的封面应落库");
        assertEquals("这是一个测试歌单", created.getIntroduction());
        assertEquals("流行", created.getStyle());
    }

    @Test
    @Order(2)
    @DisplayName("测试部分修改歌单：PATCH /api/v1/song-lists/{id}，以路径 id 为准")
    void testUpdateSongList() throws Exception {
        Integer id = requireProbeId();

        // 表单里故意带另一个 id：改哪一条以 URL 指向的资源为准，请求里的同名字段不该被采纳
        mockMvc.perform(MockMvcRequestBuilders.patch("/api/v1/song-lists/" + id)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("id", "999999")
                .param("introduction", UPDATED_INTRODUCTION)
                .param("style", UPDATED_STYLE))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.code").value(20000))
                .andExpect(MockMvcResultMatchers.jsonPath("$.success").value(true));

        SongList updated = songListService.selectByPrimaryKey(id);
        assertNotNull(updated, "路径 id 指向的歌单必须还在");
        assertEquals(UPDATED_INTRODUCTION, updated.getIntroduction());
        assertEquals(UPDATED_STYLE, updated.getStyle());
        // PATCH 语义：未提交的字段保持原值。标题尤其不能置空——它既证明没有全量覆盖，
        // 也是本类后续用例定位这一行的依据
        assertEquals(PROBE_TITLE, updated.getTitle(), "未提交的字段应保持原值，PATCH 不是全量替换");
        assertEquals(PROBE_PIC, updated.getPic(), "封面由上传接口维护，PATCH 不得改写");
        assertNull(songListService.selectByPrimaryKey(999999),
                "请求体里的 id 不被采纳，不得改到别的行");
    }

    @Test
    @Order(40)
    @DisplayName("测试删除歌单：DELETE /api/v1/song-lists/{id}，只删自己新增的那一行")
    void testDeleteSongList() throws Exception {
        Integer id = requireProbeId();

        mockMvc.perform(MockMvcRequestBuilders.delete("/api/v1/song-lists/" + id))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.code").value(20000))
                .andExpect(MockMvcResultMatchers.jsonPath("$.data").value(true));

        assertNull(songListService.selectByPrimaryKey(id), "删除后这一行不应再存在");
        assertNull(reloadProbe(), "删除后按标题也应查不到这一行");
        // 种子行必须还活着：它是其它用例 INNER JOIN 的依赖
        assertNotNull(songListService.selectByPrimaryKey(1),
                "删除用例不得伤及 schema.sql 的种子歌单 id=1");
    }

    @Test
    @Order(3)
    @DisplayName("测试查询歌单详情：GET /api/v1/song-lists/{id}")
    void testSelectByPrimaryKey() throws Exception {
        Integer id = requireProbeId();

        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/song-lists/" + id))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.code").value(20000))
                .andExpect(MockMvcResultMatchers.jsonPath("$.data.id").value(id))
                .andExpect(MockMvcResultMatchers.jsonPath("$.data.title").value(PROBE_TITLE))
                .andExpect(MockMvcResultMatchers.jsonPath("$.data.introduction").value(UPDATED_INTRODUCTION));
    }

    @Test
    @Order(4)
    @DisplayName("测试标题精确查询：GET /api/v1/song-lists?title=")
    void testSongListOfTitle() throws Exception {
        Integer id = requireProbeId();

        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/song-lists")
                .param("title", PROBE_TITLE))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.success").value(true))
                .andExpect(MockMvcResultMatchers.jsonPath("$.data[0].id").value(id));

        // 只测命中一半证明不了"精确"：再给一个真存在但只是前缀的标题，必须查空
        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/song-lists")
                .param("title", "单测专用"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.data").isArray())
                .andExpect(MockMvcResultMatchers.jsonPath("$.data.length()").value(0));
    }

    @Test
    @Order(5)
    @DisplayName("测试标题模糊查询：GET /api/v1/song-lists?keyword=")
    void testLikeTitle() throws Exception {
        Integer id = requireProbeId();

        MvcResult result = mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/song-lists")
                .param("keyword", "单测专用"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.success").value(true))
                .andExpect(MockMvcResultMatchers.jsonPath("$.data").isArray())
                .andReturn();

        assertTrue(containsId(dataOf(result), id), "标题片段模糊查询应命中本类新增的歌单");
    }

    @Test
    @Order(6)
    @DisplayName("测试风格模糊查询：GET /api/v1/song-lists?style=")
    void testLikeStyle() throws Exception {
        Integer id = requireProbeId();

        MvcResult result = mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/song-lists")
                .param("style", "摇"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.success").value(true))
                .andExpect(MockMvcResultMatchers.jsonPath("$.data").isArray())
                .andReturn();

        assertTrue(containsId(dataOf(result), id),
                "按风格片段查询应命中被改成" + UPDATED_STYLE + "的测试歌单");
    }

    @Test
    @Order(7)
    @DisplayName("测试查询所有歌单：GET /api/v1/song-lists")
    void testAllSongList() throws Exception {
        Integer id = requireProbeId();

        MvcResult result = mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/song-lists"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.success").value(true))
                .andExpect(MockMvcResultMatchers.jsonPath("$.data").isArray())
                .andReturn();

        JSONArray data = dataOf(result);
        assertFalse(data.isEmpty(), "测试库有种子歌单，全量列表不应为空");
        assertTrue(containsId(data, id), "全量列表应包含本类新增的歌单");
    }


    @Test
    @Order(30)
    @DisplayName("测试上传可执行文件被拒绝且不改动封面")
    void testUpdateSongListPicRejectsExecutable() throws Exception {
        Integer id = requireProbeId();
        String picBefore = songListService.selectByPrimaryKey(id).getPic();

        // 只断言 200 的旧写法等于没测：本项目业务失败也返回 200。
        // 这里断言的是 #22 那批补上的扩展名白名单确实生效 ——
        // 校验在落盘之前，所以不会产生垃圾文件
        byte[] bytes = new byte[]{0x4d, 0x5a, 0x01, 0x02};
        mockMvc.perform(MockMvcRequestBuilders.multipart("/api/v1/song-lists/" + id + "/picture")
                .file(new MockMultipartFile("file", "evil.exe", "application/octet-stream", bytes)))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.success").value(false));

        assertEquals(picBefore, songListService.selectByPrimaryKey(id).getPic(),
                "被拒绝的上传不得改写库里的封面");
    }

    @Test
    @Order(31)
    @DisplayName("测试消费者令牌不能上传歌单封面")
    void testUpdateSongListPicRequiresAdmin() throws Exception {
        // 鉴权发生在参数解析之前，这条请求不会落盘，只需断状态码；
        // 路径拼错会返回 404 而不是 403，所以这条同时校验了新路径的字面
        consumerMvc.perform(MockMvcRequestBuilders.multipart(
                        "/api/v1/song-lists/" + requireProbeId() + "/picture")
                .file(new MockMultipartFile("file", "cover.png", "image/png", new byte[]{1})))
                .andExpect(MockMvcResultMatchers.status().isForbidden());
    }

    @Test
    @Order(10)
    @DisplayName("测试查询推荐歌曲：GET /api/v1/song-lists/recommended，身份只取令牌")
    void testRecommendedSongs() throws Exception {
        // 推荐按角色只允许 consumer；用户身份不再由请求里的 id 传入
        consumerMvc.perform(MockMvcRequestBuilders.get("/api/v1/song-lists/recommended"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.code").value(20000))
                .andExpect(MockMvcResultMatchers.jsonPath("$.success").value(true))
                .andExpect(MockMvcResultMatchers.jsonPath("$.data").isArray());

        // 管理员令牌打消费者专属接口应 403。这条顺带证明路径没写错：
        // 拼错路径命中的是静态资源兜底返回 404，角色校验根本不会触发
        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/song-lists/recommended"))
                .andExpect(MockMvcResultMatchers.status().isForbidden());

        anonymousMvc.perform(MockMvcRequestBuilders.get("/api/v1/song-lists/recommended"))
                .andExpect(MockMvcResultMatchers.status().isUnauthorized());
    }

    @Test
    @Order(8)
    @DisplayName("测试分页查询所有歌单：GET /api/v1/song-lists/page")
    void testAllSongListByPage() throws Exception {
        MvcResult result = mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/song-lists/page")
                .param("pageNum", "1")
                .param("pageSize", "2"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.success").value(true))
                .andExpect(MockMvcResultMatchers.jsonPath("$.data.list").isArray())
                .andExpect(MockMvcResultMatchers.jsonPath("$.data.pageNum").value(1))
                .andExpect(MockMvcResultMatchers.jsonPath("$.data.pageSize").value(2))
                .andReturn();

        JSONObject page = JSON.parseObject(result.getResponse().getContentAsString(StandardCharsets.UTF_8)).getJSONObject("data");
        JSONArray list = page.getJSONArray("list");
        assertTrue(list.size() <= 2, "每页大小必须被 PageHelper 真正限制住，实际返回 " + list.size() + " 条");
        assertTrue(page.getLongValue("total") >= 1, "测试库至少有种子歌单，total 不应为 0");
    }
}

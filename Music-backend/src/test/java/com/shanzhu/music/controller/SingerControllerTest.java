package com.shanzhu.music.controller;

import cn.hutool.core.date.DateUtil;
import com.shanzhu.music.BaseTest;
import com.shanzhu.music.common.R;
import com.shanzhu.music.entity.po.Singer;
import com.shanzhu.music.service.SingerService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * SingerController 单元测试
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class SingerControllerTest extends BaseTest {

    @Autowired
    private SingerService singerService;

    /**
     * testAddSinger 建出来的歌手，交给 testUpdateSinger 按真实 id 修改。
     * 原先 update 用例写死 id=1，测的是"恰好存在的那一行"而不是接口本身。
     */
    private static Integer addedSingerId;

    /**
     * 测试分页查询歌手
     */
    @Test
    @Order(1)
    @DisplayName("测试分页查询歌手")
    void testAllSingerByPage() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/singers/page")
                .param("pageNum", "1")
                .param("pageSize", "5"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.code").value(20000))
                .andExpect(MockMvcResultMatchers.jsonPath("$.success").value(true))
                .andExpect(MockMvcResultMatchers.jsonPath("$.data.list").isArray());
        
        System.out.println("分页查询歌手接口测试通过");
    }

    /**
     * 测试查询所有歌手
     */
    @Test
    @Order(2)
    @DisplayName("测试查询所有歌手")
    void testAllSinger() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/singers"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.success").value(true))
                .andExpect(MockMvcResultMatchers.jsonPath("$.data").isArray());
        
        System.out.println("查询所有歌手接口测试通过");
    }

    /**
     * 测试根据名称模糊查询歌手
     */
    @Test
    @Order(3)
    @DisplayName("测试根据名称模糊查询歌手")
    void testSingerOfName() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/singers")
                .param("name", "周"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.success").value(true))
                .andExpect(MockMvcResultMatchers.jsonPath("$.data").isArray());
        
        System.out.println("模糊查询歌手接口测试通过");
    }

    /**
     * 测试根据性别查询歌手
     */
    @Test
    @Order(4)
    @DisplayName("测试根据性别查询歌手")
    void testSingerOfSex() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/singers")
                .param("sex", "1"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.success").value(true))
                .andExpect(MockMvcResultMatchers.jsonPath("$.data").isArray());
        
        System.out.println("根据性别查询歌手接口测试通过");
    }

    /**
     * 测试查询歌手详情
     *
     * 原先固定查 id=1 且只断 $.success 存在：本类的歌手是在 @Order(6) 之后才建起来的，
     * id=1 是否存在要看别的类跑没跑过，而"查不到"同样返回 success=true——
     * 这条用例因此从未真正验证过详情接口。现在查 testAddSinger 建出来的那一个。
     */
    @Test
    @Order(9)
    @DisplayName("测试查询歌手详情")
    void testSelectByPrimaryKey() throws Exception {
        assertNotNull(addedSingerId, "需要一个 testAddSinger 建出来的歌手");
        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/singers/" + addedSingerId))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.code").value(20000))
                .andExpect(MockMvcResultMatchers.jsonPath("$.data.id").value(addedSingerId))
                .andExpect(MockMvcResultMatchers.jsonPath("$.data.name").value("测试歌手Controller"));
    }

    /**
     * 测试添加歌手
     *
     * 生日解析与 DTO→PO 组装已下沉到 SingerService，这里断的是落库结果：
     * 只断 code==20000 的话，"新增成功但生日没存进去"照样通过。
     */
    @Test
    @Order(6)
    @DisplayName("测试添加歌手")
    void testAddSinger() throws Exception {
        String jsonRequest = "{" +
                "\"name\":\"测试歌手Controller\"," +
                "\"sex\":1," +
                "\"pic\":\"/img/singerPic/test.jpg\"," +
                "\"birth\":\"1990-01-01\"," +
                "\"location\":\"北京\"," +
                "\"introduction\":\"测试简介\"" +
                "}";

        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/singers")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonRequest))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.code").value(20000));

        List<Singer> created = singerService.singerOfName("测试歌手Controller");
        assertFalse(created.isEmpty(), "新增后应能按名称查回");
        Singer singer = created.get(0);
        addedSingerId = singer.getId();
        assertEquals("1990-01-01", DateUtil.formatDate(singer.getBirth()),
                "生日应在 service 里解析成 date 落库");
        assertEquals("", singer.getPic(),
                "头像由上传接口维护，新增时只占空串（请求里的 pic 字段不采纳）");
    }

    /**
     * 测试修改歌手
     */
    @Test
    @Order(7)
    @DisplayName("测试修改歌手")
    void testUpdateSinger() throws Exception {
        assertNotNull(addedSingerId, "需要一个 testAddSinger 建出来的歌手");
        // 请求体里故意写另一个 id：改哪个歌手以 URL 指向的资源为准（见 SingerService#update 说明）
        String jsonRequest = "{" +
                "\"id\":999999," +
                "\"name\":\"测试歌手Controller\"," +
                "\"sex\":1," +
                "\"birth\":\"1979-01-18\"," +
                "\"location\":\"台湾\"," +
                "\"introduction\":\"华语流行音乐男歌手\"" +
                "}";

        mockMvc.perform(MockMvcRequestBuilders.patch("/api/v1/singers/" + addedSingerId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonRequest))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.code").value(20000));

        Singer updated = singerService.selectByPrimaryKey(addedSingerId);
        assertEquals("1979-01-18", DateUtil.formatDate(updated.getBirth()),
                "修改资料时生日同样要解析，不能丢掉");
        assertEquals("台湾", updated.getLocation());
    }

    /**
     * 测试删除歌手
     *
     * 上一版只添加不发 DELETE 请求，注释写着"需要真实ID"，然后就绿着交了 ——
     * 删除接口连同它的级联在本类里实际一次都没被测过（级联另见 SingerCascadeTest）。
     * 现在删 testAddSinger 建出来的那一个，并断它真的从库里消失。
     */
    @Test
    @Order(10)
    @DisplayName("测试删除歌手")
    void testDeleteSinger() throws Exception {
        assertNotNull(addedSingerId, "需要一个 testAddSinger 建出来的歌手");

        mockMvc.perform(MockMvcRequestBuilders.delete("/api/v1/singers/" + addedSingerId))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.code").value(20000))
                .andExpect(MockMvcResultMatchers.jsonPath("$.data").value(true));

        assertNull(singerService.selectByPrimaryKey(addedSingerId), "删除后应查不到该歌手");
    }
}

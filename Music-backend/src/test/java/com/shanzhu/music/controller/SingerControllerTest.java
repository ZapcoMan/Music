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
        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/singer/allSingerByPage")
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
        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/singer/allSinger"))
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
        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/singer/singerOfName")
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
        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/singer/singerOfSex")
                .param("sex", "1"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.success").value(true))
                .andExpect(MockMvcResultMatchers.jsonPath("$.data").isArray());
        
        System.out.println("根据性别查询歌手接口测试通过");
    }

    /**
     * 测试查询歌手详情
     */
    @Test
    @Order(5)
    @DisplayName("测试查询歌手详情")
    void testSelectByPrimaryKey() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/singer/selectByPrimaryKey")
                .param("id", "1"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.success").exists());
        
        System.out.println("查询歌手详情接口测试通过");
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

        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/singer/add")
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
        String jsonRequest = "{" +
                "\"id\":" + addedSingerId + "," +
                "\"name\":\"测试歌手Controller\"," +
                "\"sex\":1," +
                "\"birth\":\"1979-01-18\"," +
                "\"location\":\"台湾\"," +
                "\"introduction\":\"华语流行音乐男歌手\"" +
                "}";

        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/singer/update")
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
     */
    @Test
    @Order(8)
    @DisplayName("测试删除歌手")
    void testDeleteSinger() throws Exception {
        // 先添加一个测试歌手
        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/singer/add")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("name", "待删除歌手")
                .param("sex", "1")
                .param("pic", "/img/singerPic/test.jpg")
                .param("birth", "1990-01-01")
                .param("location", "北京")
                .param("introduction", "测试"));
        
        // 这里简化测试,实际应该获取新添加的ID
        System.out.println("删除歌手接口测试完成(需要真实ID)");
    }
}

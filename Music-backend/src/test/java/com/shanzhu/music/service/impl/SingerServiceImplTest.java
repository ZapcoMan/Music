package com.shanzhu.music.service.impl;

import com.github.pagehelper.PageInfo;
import com.shanzhu.music.BaseTest;
import com.shanzhu.music.entity.po.Singer;
import com.shanzhu.music.service.SingerService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * SingerService 单元测试
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class SingerServiceImplTest extends BaseTest {

    @Autowired
    private SingerService singerService;

    private static Integer testSingerId;
    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");

    /**
     * 测试添加歌手
     */
    @Test
    @Order(1)
    @DisplayName("测试添加歌手")
    void testInsert() throws ParseException {
        Singer singer = new Singer();
        singer.setName("测试歌手");
        singer.setSex((byte) 1);
        singer.setPic("/img/singerPic/test.jpg");
        singer.setBirth(dateFormat.parse("1990-01-01"));
        singer.setLocation("北京");
        singer.setIntroduction("这是一个测试歌手");

        Boolean result = singerService.insert(singer);
        assertTrue(result, "添加歌手应该成功");
        
        // 保存ID供后续测试使用
        assertNotNull(singer.getId(), "歌手ID不应为空");
        testSingerId = singer.getId();
        System.out.println("添加的歌手ID: " + testSingerId);
    }

    /**
     * 测试查询歌手
     */
    @Test
    @Order(2)
    @DisplayName("测试查询歌手")
    void testSelectByPrimaryKey() {
        assertNotNull(testSingerId, "测试歌手ID不应为空");
        
        Singer singer = singerService.selectByPrimaryKey(testSingerId);
        assertNotNull(singer, "查询结果不应为空");
        assertEquals("测试歌手", singer.getName(), "歌手名称应该匹配");
        System.out.println("查询到的歌手: " + singer.getName());
    }

    /**
     * 测试修改歌手
     */
    @Test
    @Order(3)
    @DisplayName("测试修改歌手")
    void testUpdate() throws ParseException {
        assertNotNull(testSingerId, "测试歌手ID不应为空");
        
        Singer singer = new Singer();
        singer.setId(testSingerId);
        singer.setName("修改后的歌手");
        singer.setSex((byte) 0);
        singer.setBirth(dateFormat.parse("1995-06-15"));
        singer.setLocation("上海");
        singer.setIntroduction("修改后的简介");

        Boolean result = singerService.update(singer);
        assertTrue(result, "修改歌手应该成功");

        // 验证修改是否生效
        Singer updatedSinger = singerService.selectByPrimaryKey(testSingerId);
        assertEquals("修改后的歌手", updatedSinger.getName(), "歌手名称应该已修改");
        assertEquals((byte) 0, updatedSinger.getSex(), "性别应该已修改");
        System.out.println("修改后的歌手: " + updatedSinger.getName());
    }

    /**
     * 测试查询所有歌手
     */
    @Test
    @Order(4)
    @DisplayName("测试查询所有歌手")
    void testAllSinger() {
        List<Singer> singers = singerService.allSinger();
        assertNotNull(singers, "歌手列表不应为空");
        assertTrue(singers.size() >= 3, "至少应该有3个测试歌手");
        System.out.println("歌手总数: " + singers.size());
    }

    /**
     * 测试分页查询歌手
     */
    @Test
    @Order(5)
    @DisplayName("测试分页查询歌手")
    void testAllSingerByPage() {
        PageInfo<Singer> pageInfo = singerService.allSingerByPage(1, 2);
        assertNotNull(pageInfo, "分页结果不应为空");
        assertTrue(pageInfo.getTotal() >= 3, "总记录数应该至少3条");
        assertEquals(2, pageInfo.getSize(), "每页大小应该是2");
        assertTrue(pageInfo.getList().size() <= 2, "当前页数据不应超过2条");
        System.out.println("分页查询 - 总数: " + pageInfo.getTotal() + ", 当前页: " + pageInfo.getPageNum());
    }

    /**
     * 测试根据名称模糊查询
     */
    @Test
    @Order(6)
    @DisplayName("测试根据名称模糊查询")
    void testSingerOfName() {
        List<Singer> singers = singerService.singerOfName("%周%");
        assertNotNull(singers, "查询结果不应为空");
        assertTrue(singers.size() >= 1, "应该至少找到1个包含'周'的歌手");
        System.out.println("模糊查询结果数: " + singers.size());
    }

    /**
     * 测试根据性别查询
     */
    @Test
    @Order(7)
    @DisplayName("测试根据性别查询")
    void testSingerOfSex() {
        List<Singer> maleSingers = singerService.singerOfSex(1);
        assertNotNull(maleSingers, "查询结果不应为空");
        System.out.println("男性歌手数量: " + maleSingers.size());

        List<Singer> femaleSingers = singerService.singerOfSex(0);
        assertNotNull(femaleSingers, "查询结果不应为空");
        System.out.println("女性歌手数量: " + femaleSingers.size());
    }

    /**
     * 测试删除歌手
     */
    @Test
    @Order(8)
    @DisplayName("测试删除歌手")
    void testDelete() {
        assertNotNull(testSingerId, "测试歌手ID不应为空");
        
        Boolean result = singerService.delete(testSingerId);
        assertTrue(result, "删除歌手应该成功");

        // 验证删除是否生效
        Singer deletedSinger = singerService.selectByPrimaryKey(testSingerId);
        assertNull(deletedSinger, "删除后查询应该返回null");
        System.out.println("歌手已删除,ID: " + testSingerId);
    }

    /**
     * 测试删除不存在的歌手
     */
    @Test
    @Order(9)
    @DisplayName("测试删除不存在的歌手")
    void testDeleteNonExistent() {
        Boolean result = singerService.delete(99999);
        assertFalse(result, "删除不存在的歌手应该返回false");
    }


    @Test
    @Order(8)
    @DisplayName("测试插入无名歌手被数据库约束拒绝")
    void testInsertWithNullName() {
        // singer.name 是 NOT NULL。此前这个用例什么都不做却计为通过。
        // 现在断言约束真的存在：service 不会替调用方兜住这个名字缺失
        Singer singer = new Singer();
        singer.setName(null);
        singer.setSex((byte) 1);
        singer.setIntroduction("缺名探针");
        Exception thrown = null;
        try {
            singerService.insert(singer);
        } catch (Exception e) {
            thrown = e;
        }
        if (thrown == null) {
            // 没抛异常就说明约束没生效，必须把这条脏数据清掉并让用例失败
            singerService.delete(singer.getId());
            org.junit.jupiter.api.Assertions.fail("name 为 null 应被 NOT NULL 约束拒绝");
        }
        org.junit.jupiter.api.Assertions.assertEquals(0,
                singerService.allSinger().stream().filter(x -> "缺名探针".equals(x.getIntroduction())).count(),
                "不应留下任何插入成功的痕迹");
    }
}

package com.shanzhu.music.controller;

import com.shanzhu.music.BaseTest;
import com.shanzhu.music.entity.po.Collect;
import com.shanzhu.music.service.CollectService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

/**
 * CollectController 单元测试
 *
 * 收藏的归属只认令牌（userId=35），所以"造数据"要么走 service（可直接指定作者），
 * 要么走 HTTP 后按内容/关联键回查。collect 表在测试库没有种子行，
 * 上一版那种"打一个 delete 然后断 $.success 存在"的用例删的是不存在的行，返回 false 也算过。
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class CollectControllerTest extends BaseTest {

    @Autowired
    private CollectService collectService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private Integer insertSongCollect(Integer userId, Integer songId) {
        Collect collect = new Collect();
        collect.setUserId(userId);
        collect.setType((byte) 0);
        collect.setSongId(songId);
        collectService.insert(collect);
        Assertions.assertNotNull(collect.getId(), "收藏应已落库");
        return collect.getId();
    }

    private int countSongCollect(Integer songId) {
        Integer count = jdbcTemplate.queryForObject(
                "select count(*) from collect where user_id = 35 and song_id = ? and type = 0",
                Integer.class, songId);
        return count == null ? 0 : count;
    }

    private int countSongListCollect(Integer songListId) {
        Integer count = jdbcTemplate.queryForObject(
                "select count(*) from collect where user_id = 35 and song_list_id = ? and type = 1",
                Integer.class, songListId);
        return count == null ? 0 : count;
    }

    @Test
    @Order(1)
    @DisplayName("测试添加歌曲收藏")
    void testAddCollect() throws Exception {
        // songId 用一个只有本用例碰的值，免得别处给 user 1 造的同键行干扰"不应生效"那半句
        consumerMvc.perform(MockMvcRequestBuilders.post("/api/v1/collections")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("userId", "1")
                .param("type", "0")
                .param("songId", "21"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.success").value(true));

        // 上一版只断 $.success 存在：库里到底有没有这一行、归属给谁都没查，
        // 而"归属给谁"正是这类接口最需要盯住的东西
        Assertions.assertEquals(1, jdbcTemplate.queryForObject(
                        "select count(*) from collect where user_id = 35 and song_id = 21 and type = 0",
                        Integer.class),
                "收藏应落在令牌用户 35 名下");
        Assertions.assertEquals(0, jdbcTemplate.queryForObject(
                        "select count(*) from collect where user_id = 1 and song_id = 21",
                        Integer.class),
                "请求参数里的 userId=1 不应被采纳");
    }

    @Test
    @Order(2)
    @DisplayName("测试添加收藏-重复收藏被挡住且只有一行")
    void testAddCollectDuplicate() throws Exception {
        collectService.deleteByUserIdSongId(35, 2);

        consumerMvc.perform(MockMvcRequestBuilders.post("/api/v1/collections")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("type", "0")
                .param("songId", "2"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.success").value(true));

        // 上一版第二次提交仍只断言 $.success 存在——"已收藏"是 success=false，
        // 插出第二行是 success=true，两种相反的结果都能通过
        consumerMvc.perform(MockMvcRequestBuilders.post("/api/v1/collections")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("type", "0")
                .param("songId", "2"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.success").value(false))
                .andExpect(MockMvcResultMatchers.jsonPath("$.code").value(2))
                .andExpect(MockMvcResultMatchers.jsonPath("$.message").value("已收藏"));

        Assertions.assertEquals(1, countSongCollect(2), "重复收藏不应插出第二行");
    }

    @Test
    @Order(3)
    @DisplayName("测试按歌曲定位取消收藏")
    void testDeleteCollectBySongId() throws Exception {
        insertSongCollect(35, 3);
        Assertions.assertEquals(1, countSongCollect(3), "前置收藏应已落库");

        consumerMvc.perform(MockMvcRequestBuilders.delete("/api/v1/collections")
                .param("songId", "3"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.success").value(true))
                .andExpect(MockMvcResultMatchers.jsonPath("$.data").value(true));

        Assertions.assertEquals(0, countSongCollect(3), "取消收藏后该行应已从库中删除");
    }

    @Test
    @Order(4)
    @DisplayName("测试取消收藏时两个定位符都不给应报参数错误")
    void testDeleteCollectWithoutLocator() throws Exception {
        insertSongCollect(35, 4);

        // 旧实现会让 songId=null 走到 "delete ... and song_id = null"，条件恒不成立，
        // 返回的 false 与"删了但没删掉"混在一起；现在明确告诉调用方是参数缺失
        consumerMvc.perform(MockMvcRequestBuilders.delete("/api/v1/collections"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.success").value(false))
                .andExpect(MockMvcResultMatchers.jsonPath("$.message")
                        .value("请指定要取消收藏的歌曲或歌单"));

        Assertions.assertEquals(1, countSongCollect(4), "参数错误的请求不应删掉任何行");
    }

    @Test
    @Order(5)
    @DisplayName("测试查询所有收藏-仅管理员")
    void testAllCollect() throws Exception {
        Integer id = insertSongCollect(35, 5);

        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/collections"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.success").value(true))
                // 过滤式没命中也返回空数组，用 exists 断等于没断，故用 isNotEmpty
                .andExpect(MockMvcResultMatchers.jsonPath("$.data[?(@.id == " + id + ")]")
                        .isNotEmpty());

        // 全量收藏含全站 user_id，属于跨用户数据读取，消费者令牌不该能拉到
        consumerMvc.perform(MockMvcRequestBuilders.get("/api/v1/collections"))
                .andExpect(MockMvcResultMatchers.status().isForbidden());
    }

    @Test
    @Order(6)
    @DisplayName("测试我的收藏只返回令牌用户的数据")
    void testCollectMineIgnoresUserIdParameter() throws Exception {
        // 先给"别人"（user 1）造一行，再给本人造一行：
        // 若端点重新采纳请求里的 userId，返回的就是 user 1 的行，下面两句会当场失败
        insertSongCollect(1, 6);
        insertSongCollect(35, 7);

        consumerMvc.perform(MockMvcRequestBuilders.get("/api/v1/collections/mine")
                .param("userId", "1"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.success").value(true))
                .andExpect(MockMvcResultMatchers.jsonPath("$.data").isArray())
                .andExpect(MockMvcResultMatchers.jsonPath("$.data[?(@.userId == 1)]").isEmpty());

        Assertions.assertEquals(1, countSongCollect(7), "本人的收藏应能读回");
    }

    @Test
    @Order(7)
    @DisplayName("测试收藏歌曲接口不传 songId 时给出参数错误而非 500")
    void testSongCollectRequiresSongId() throws Exception {
        consumerMvc.perform(MockMvcRequestBuilders.post("/api/v1/collections")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("type", "0"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.success").value(false))
                .andExpect(MockMvcResultMatchers.jsonPath("$.message").value("收藏歌曲为空"));
    }

    @Test
    @Order(8)
    @DisplayName("测试收藏歌单接口不传 songListId 时给出参数错误")
    void testSongListCollectRequiresId() throws Exception {
        consumerMvc.perform(MockMvcRequestBuilders.post("/api/v1/collections")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("type", "1"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.success").value(false))
                .andExpect(MockMvcResultMatchers.jsonPath("$.message").value("收藏歌单为空"));
    }

    @Test
    @Order(20)
    @DisplayName("测试收藏歌单可写入、可读回、可取消")
    void testSongListCollectRoundTrip() throws Exception {
        collectService.deleteByUserIdSongListId(35, 1);

        consumerMvc.perform(MockMvcRequestBuilders.post("/api/v1/collections")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("type", "1")
                .param("songListId", "1"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.success").value(true));

        // 歌单收藏的读取走 /collections/song-lists，返回的是歌单视图而非 collect 行
        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/collections/song-lists")
                .header("Authorization", consumerToken()))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.success").value(true))
                .andExpect(MockMvcResultMatchers.jsonPath("$.data[?(@.id == 1)]").isNotEmpty());

        // 重复收藏要挡住，而不是插出第二行
        consumerMvc.perform(MockMvcRequestBuilders.post("/api/v1/collections")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("type", "1")
                .param("songListId", "1"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.code").value(2));
        Assertions.assertEquals(1, countSongListCollect(1), "重复收藏不应插出第二行");

        consumerMvc.perform(MockMvcRequestBuilders.delete("/api/v1/collections")
                .param("songListId", "1"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.success").value(true));

        // 上一版这里还调了一次 collectService.collectSongListOfUserId(35) 但没接返回值，
        // 再用缓存方法做断言；改成直接查库，确认行真的没了
        Assertions.assertEquals(0, countSongListCollect(1), "取消收藏后 collect 行应已删除");
    }

    @Test
    @Order(21)
    @DisplayName("测试收藏歌单读回的是歌单视图")
    void testCollectSongListView() throws Exception {
        Collect collect = new Collect();
        collect.setUserId(35);
        collect.setType((byte) 1);
        collect.setSongListId(2);
        collectService.insert(collect);

        consumerMvc.perform(MockMvcRequestBuilders.get("/api/v1/collections/song-lists"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.data[?(@.id == 2)]").isNotEmpty());
    }
}

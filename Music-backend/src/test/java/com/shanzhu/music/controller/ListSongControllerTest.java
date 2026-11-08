package com.shanzhu.music.controller;

import com.shanzhu.music.BaseTest;
import com.shanzhu.music.entity.po.ListSong;
import com.shanzhu.music.service.ListSongService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

/**
 * ListSongController 单元测试
 *
 * 这批端点改成了歌单的子资源（/api/v1/song-lists/{songListId}/songs[/{songId}]），
 * 歌单归属由 URL 给出，所以用例盯两件事：
 * 1) URL 是权威的——请求体里再传一个不同的 songListId 也不能把歌加进别的歌单；
 * 2) 路径里的 songListId / songId 真的参与过滤与删除条件。
 *
 * 断言一律落到 list_song 表（业务失败也返回 HTTP 200，只断 $.success 等于没测），
 * 并且每首歌占一个专用 songId、每个用例先清场：测试库的 list_song 没有种子行，
 * 借种子歌曲 1、2 会被别的测试类插入的关联干扰成"计数对不上"的假失败。
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ListSongControllerTest extends BaseTest {

    @Autowired
    private ListSongService listSongService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private static final int LIST_A = 987651;
    private static final int LIST_B = 987652;
    private static final int LIST_EMPTY = 987653;
    private static final int SONG_IN_A = 987101;
    private static final int SONG_IN_B = 987102;
    private static final int SONG_TO_DELETE = 987103;
    private static final int SONG_KEEP = 987104;
    private static final int SONG_MISSING = 987105;
    private static final int SONG_FORBIDDEN = 987106;
    private static final int SONG_PROTECTED = 987107;

    private void newRelation(int songId, int songListId) {
        // 先删再插：关联表没有唯一约束，同一条 (song, list) 插两次就是两行，
        // 后面"恰好一行"的计数断言会莫名其妙地失败
        jdbcTemplate.update("delete from list_song where song_id = ? and song_list_id = ?",
                songId, songListId);
        ListSong listSong = new ListSong();
        listSong.setSongId(songId);
        listSong.setSongListId(songListId);
        listSongService.insert(listSong);
        Assertions.assertEquals(1, countOf(songId, songListId), "关联行应已落库");
    }

    private int countOf(int songId, int songListId) {
        Integer count = jdbcTemplate.queryForObject(
                "select count(*) from list_song where song_id = ? and song_list_id = ?",
                Integer.class, songId, songListId);
        return count == null ? 0 : count;
    }

    @Test
    @Order(1)
    @DisplayName("测试给歌单添加歌曲-以 URL 中的歌单为准")
    void testAddListSong() throws Exception {
        jdbcTemplate.update("delete from list_song where song_id = ?", SONG_IN_A);
        // 故意再带一个 songListId：以路径指向的资源为权威，这个参数不该生效
        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/song-lists/" + LIST_A + "/songs")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("songId", String.valueOf(SONG_IN_A))
                .param("songListId", String.valueOf(LIST_B)))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.success").value(true))
                .andExpect(MockMvcResultMatchers.jsonPath("$.message").value("保存成功"));

        Assertions.assertEquals(1, countOf(SONG_IN_A, LIST_A), "歌曲应按 URL 加入歌单 A");
        Assertions.assertEquals(0, countOf(SONG_IN_A, LIST_B), "请求参数里的 songListId 不应生效");
    }

    @Test
    @Order(2)
    @DisplayName("测试按歌单查询歌曲")
    void testDetail() throws Exception {
        newRelation(SONG_IN_A, LIST_A);
        newRelation(SONG_IN_B, LIST_B);

        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/song-lists/" + LIST_A + "/songs"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.success").value(true))
                .andExpect(MockMvcResultMatchers.jsonPath("$.data").isArray())
                // 过滤式没命中也返回空数组，用 exists 断等于没断，故用 isNotEmpty
                .andExpect(MockMvcResultMatchers.jsonPath(
                        "$.data[?(@.songId == " + SONG_IN_A + ")]").isNotEmpty())
                // 另一张歌单的歌不该串进来：漏掉 song_list_id 条件时这句会失败
                .andExpect(MockMvcResultMatchers.jsonPath(
                        "$.data[?(@.songId == " + SONG_IN_B + ")]").isEmpty())
                .andExpect(MockMvcResultMatchers.jsonPath(
                        "$.data[?(@.songListId == " + LIST_A + ")]").isNotEmpty());

        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/song-lists/" + LIST_B + "/songs"))
                .andExpect(MockMvcResultMatchers.jsonPath(
                        "$.data[?(@.songId == " + SONG_IN_B + ")]").isNotEmpty())
                .andExpect(MockMvcResultMatchers.jsonPath(
                        "$.data[?(@.songId == " + SONG_IN_A + ")]").isEmpty());
    }

    @Test
    @Order(3)
    @DisplayName("测试查询空歌单返回空数组而不是 null")
    void testDetailOfUnknownSongList() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/song-lists/" + LIST_EMPTY + "/songs"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.success").value(true))
                .andExpect(MockMvcResultMatchers.jsonPath("$.data").isArray())
                .andExpect(MockMvcResultMatchers.jsonPath("$.data").isEmpty());
    }

    @Test
    @Order(4)
    @DisplayName("测试删除歌单里的歌曲")
    void testDelete() throws Exception {
        newRelation(SONG_TO_DELETE, LIST_A);
        newRelation(SONG_KEEP, LIST_B);

        mockMvc.perform(MockMvcRequestBuilders.delete(
                "/api/v1/song-lists/" + LIST_A + "/songs/" + SONG_TO_DELETE))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.success").value(true))
                .andExpect(MockMvcResultMatchers.jsonPath("$.data").value(true));

        Assertions.assertEquals(0, countOf(SONG_TO_DELETE, LIST_A), "关联行应已删除");
        // 删除条件必须同时含 song_id 与 song_list_id：另一张歌单里的歌不该被连带删掉
        Assertions.assertEquals(1, countOf(SONG_KEEP, LIST_B), "别的歌单的关联不应受影响");
    }

    @Test
    @Order(5)
    @DisplayName("测试删除不存在的关联返回 data=false 而不是报错")
    void testDeleteMissingRelation() throws Exception {
        newRelation(SONG_KEEP, LIST_A);

        // 这首歌并没有在歌单 A 里：应删到 0 行，返回 false，且不能把别的行删掉
        mockMvc.perform(MockMvcRequestBuilders.delete(
                "/api/v1/song-lists/" + LIST_A + "/songs/" + SONG_MISSING))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.success").value(true))
                .andExpect(MockMvcResultMatchers.jsonPath("$.data").value(false));

        Assertions.assertEquals(1, countOf(SONG_KEEP, LIST_A), "无匹配行的删除不应影响其它关联");
    }

    @Test
    @Order(6)
    @DisplayName("测试消费者令牌不能改动歌单与歌曲的关联")
    void consumerCannotManageRelation() throws Exception {
        newRelation(SONG_PROTECTED, LIST_A);

        consumerMvc.perform(MockMvcRequestBuilders.post("/api/v1/song-lists/" + LIST_A + "/songs")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("songId", String.valueOf(SONG_FORBIDDEN)))
                .andExpect(MockMvcResultMatchers.status().isForbidden());
        Assertions.assertEquals(0, countOf(SONG_FORBIDDEN, LIST_A), "被拒的写入不应落库");

        consumerMvc.perform(MockMvcRequestBuilders.delete(
                "/api/v1/song-lists/" + LIST_A + "/songs/" + SONG_PROTECTED))
                .andExpect(MockMvcResultMatchers.status().isForbidden());
        Assertions.assertEquals(1, countOf(SONG_PROTECTED, LIST_A), "被拒的删除不应生效");
    }
}

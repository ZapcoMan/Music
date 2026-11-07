package com.shanzhu.music.service.impl;

import com.shanzhu.music.BaseTest;
import com.shanzhu.music.service.SingerService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 删除歌手的级联回归。
 *
 * 这条测试盯着两件事，缺一不可：
 *
 * 一是级联必须完整。song.singer_id 上没有外键，级联全靠应用层，
 * 少清一张表就留下指向不存在歌手的孤儿数据，而列表页、召回和统计都会继续算它们。
 *
 * 二是**不能清过头**。批量 SQL 用 `song_id in (select id from song where singer_id = ?)`
 * 圈定范围，而 collect 与 comment 同时存"歌曲类"(type=0)和"歌单类"(type=1)两种记录——
 * 歌单类那行的 song_id 为 null 本不会被子查询命中，但一旦有人为了"顺手"把 type 条件去掉，
 * 删一位歌手就会连带清空用户对该歌单的收藏与评论。那属于无关数据，且是静默丢失。
 * 所以这里专门插入 type=1 的对照行，断言它们删后仍在。
 */
class SingerCascadeTest extends BaseTest {

    private static final int SINGER_ID = 91001;
    private static final int SONG_A = 91011;
    private static final int SONG_B = 91012;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private SingerService singerService;

    private Integer controlCollectId;
    private Integer controlCommentId;

    @AfterEach
    void cleanControlRows() {
        // 对照行是本次测试自己造的，收尾删掉，避免影响其它用例的计数
        if (controlCollectId != null) {
            jdbc.update("delete from collect where id = ?", controlCollectId);
        }
        if (controlCommentId != null) {
            jdbc.update("delete from comment where id = ?", controlCommentId);
        }
        jdbc.update("delete from song where id in (?, ?)", SONG_A, SONG_B);
        jdbc.update("delete from singer where id = ?", SINGER_ID);
    }

    @Test
    @DisplayName("删除歌手：清净其歌曲引用，且不动歌单类收藏与评论")
    void deleteSingerCascadesOwnReferencesAndKeepsSongListScopedOnes() {
        Integer songListId = jdbc.queryForObject("select min(id) from song_list", Integer.class);
        assertNotNull(songListId, "测试前提：种子数据里应有歌单");

        jdbc.update("insert into singer(id, name, sex) values (?, '级联测试歌手', 1)", SINGER_ID);
        jdbc.update("insert into song(id, singer_id, name) values (?, ?, '级联测试曲A')", SONG_A, SINGER_ID);
        jdbc.update("insert into song(id, singer_id, name) values (?, ?, '级联测试曲B')", SONG_B, SINGER_ID);

        // 歌曲 A 挂满三种引用：歌单关联、歌曲收藏、歌曲评论
        jdbc.update("insert into list_song(song_id, song_list_id) values (?, ?)", SONG_A, songListId);
        jdbc.update("insert into collect(user_id, type, song_id) values (1, 0, ?)", SONG_A);
        jdbc.update("insert into comment(user_id, type, song_id, content, up) values (1, 0, ?, '级联测试评论', 0)", SONG_A);

        // 对照组：同一歌单上的 type=1 记录，与被删歌手毫无关系
        jdbc.update("insert into collect(user_id, type, song_list_id) values (1, 1, ?)", songListId);
        controlCollectId = jdbc.queryForObject("select max(id) from collect", Integer.class);
        jdbc.update("insert into comment(user_id, type, song_list_id, content, up) values (1, 1, ?, '歌单评论对照', 0)", songListId);
        controlCommentId = jdbc.queryForObject("select max(id) from comment", Integer.class);

        assertNotNull(controlCollectId);
        assertNotNull(controlCommentId);

        Boolean deleted = singerService.delete(SINGER_ID);
        assertTrue(deleted, "删除应返回成功");

        // 歌手自己的歌曲与三种引用全部清空
        assertEquals(0, count("song", "singer_id = " + SINGER_ID), "该歌手的歌曲应被连带删除");
        assertEquals(0, count("list_song", "song_id in (" + SONG_A + "," + SONG_B + ")"), "歌单关联残留成孤儿");
        assertEquals(0, count("collect", "type = 0 and song_id in (" + SONG_A + "," + SONG_B + ")"), "歌曲收藏残留成孤儿");
        assertEquals(0, count("comment", "type = 0 and song_id in (" + SONG_A + "," + SONG_B + ")"), "歌曲评论残留成孤儿");

        // 对照必须原样存在
        assertEquals(1, count("collect", "id = " + controlCollectId), "删歌手不得清掉用户对歌单的收藏");
        assertEquals(1, count("comment", "id = " + controlCommentId), "删歌手不得清掉歌单评论");
        assertEquals(0, count("singer", "id = " + SINGER_ID), "歌手本体应被删除");
    }

    private int count(String table, String where) {
        Integer n = jdbc.queryForObject("select count(*) from " + table + " where " + where, Integer.class);
        return n == null ? 0 : n;
    }
}

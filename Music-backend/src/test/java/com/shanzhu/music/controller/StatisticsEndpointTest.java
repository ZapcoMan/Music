package com.shanzhu.music.controller;

import com.shanzhu.music.BaseTest;
import com.shanzhu.music.entity.vo.OverviewVo;
import com.shanzhu.music.entity.vo.StatCountVo;
import com.shanzhu.music.service.StatisticsService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 资料页概览接口。
 *
 * 为什么需要这个类：这批数字原先是前端把四张表整表拉下来自己数出来的，
 * 换成后端聚合之后必须证明"数字没变"，而唯一可信的对照物是直接查库——
 * 拿被测服务跟自己对账等于什么都没测。
 *
 * 断言全部用相对关系而不是硬编码条数：测试库里其它用例会增删行，
 * 写死的数字会随执行顺序变红。
 */
class StatisticsEndpointTest extends BaseTest {

    @Autowired
    private StatisticsService statisticsService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("概览的四个总数与直接查库一致")
    void totalsMatchDatabase() {
        OverviewVo overview = statisticsService.overview();
        assertNotNull(overview, "概览结果不应为空");
        assertEquals(count("consumer"), overview.getConsumerTotal(), "用户总数应与查库一致");
        assertEquals(count("song"), overview.getSongTotal(), "歌曲总数应与查库一致");
        assertEquals(count("singer"), overview.getSingerTotal(), "歌手总数应与查库一致");
        assertEquals(count("song_list"), overview.getSongListTotal(), "歌单总数应与查库一致");
    }

    @Test
    @DisplayName("分组计数覆盖全部非空值，且不包含空值分组")
    void groupsMatchDatabase() {
        OverviewVo overview = statisticsService.overview();

        assertEquals(nonNullCount("singer", "sex"), sum(overview.getSingerSex()),
                "歌手性别分组之和应等于 sex 非空的行数");
        assertEquals(nonNullCount("consumer", "sex"), sum(overview.getConsumerSex()),
                "用户性别分组之和应等于 sex 非空的行数");
        assertEquals(nonBlankCount("singer", "location"), sum(overview.getSingerLocation()),
                "歌手地区分组之和应等于 location 非空的行数");
        assertEquals(nonBlankCount("song_list", "style"), sum(overview.getSongListStyle()),
                "歌单风格分组之和应等于 style 非空的行数");

        // 空标签既归不进任何图表桶，也不该出现在出参里
        for (StatCountVo row : overview.getSingerLocation()) {
            assertTrue(row.getLabel() != null && !row.getLabel().isBlank(),
                    "地区分组不应出现空标签: " + row);
        }
    }

    @Test
    @DisplayName("管理端令牌可读到概览")
    void adminCanReadOverview() throws Exception {
        long consumerTotal = count("consumer");
        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/statistics/overview")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.success").value(true))
                .andExpect(MockMvcResultMatchers.jsonPath("$.data.consumerTotal")
                        .value(Math.toIntExact(consumerTotal)))
                .andExpect(MockMvcResultMatchers.jsonPath("$.data.singerSex").isArray());
    }

    /**
     * 这条路径是给后台看板用的聚合值，不是消费者数据；漏掉这条断言的话，
     * 类级 @RequireUserType("admin") 写错成 {"consumer","admin"} 也不会有任何用例变红。
     */
    @Test
    @DisplayName("消费者令牌读概览被拒")
    void consumerTokenRejected() throws Exception {
        consumerMvc.perform(MockMvcRequestBuilders.get("/api/v1/statistics/overview"))
                .andExpect(MockMvcResultMatchers.status().isForbidden());
    }

    private Long count(String table) {
        return jdbcTemplate.queryForObject("select count(*) from " + table, Long.class);
    }

    private Long nonNullCount(String table, String column) {
        return jdbcTemplate.queryForObject(
                "select count(*) from " + table + " where " + column + " is not null", Long.class);
    }

    private Long nonBlankCount(String table, String column) {
        return jdbcTemplate.queryForObject(
                "select count(*) from " + table + " where " + column + " is not null and "
                        + column + " <> ''", Long.class);
    }

    private long sum(List<StatCountVo> rows) {
        assertNotNull(rows, "分组结果不应为 null");
        return rows.stream().mapToLong(StatCountVo::getTotal).sum();
    }

    /**
     * 直接查库取一组 sex 分组的行数，用于核对出参里的 label 是原始值而不是中文翻译
     */
    @Test
    @DisplayName("性别分组的 label 是库里的原始码值")
    void sexGroupKeepsRawCode() {
        List<Map<String, Object>> dbRows =
                jdbcTemplate.queryForList("select sex, count(*) as c from singer where sex is not null group by sex");
        List<StatCountVo> apiRows = statisticsService.overview().getSingerSex();
        assertEquals(dbRows.size(), apiRows.size(), "分组条数应与查库一致");
        for (Map<String, Object> dbRow : dbRows) {
            String rawSex = String.valueOf(dbRow.get("sex"));
            long dbCount = ((Number) dbRow.get("c")).longValue();
            assertTrue(apiRows.stream().anyMatch(r -> rawSex.equals(r.getLabel())
                            && dbCount == r.getTotal()),
                    "出参中应能按原值找到分组 " + rawSex);
        }
    }
}

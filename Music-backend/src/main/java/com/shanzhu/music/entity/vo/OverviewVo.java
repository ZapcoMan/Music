package com.shanzhu.music.entity.vo;

import lombok.Data;

import java.util.List;

/**
 * 管理端资料页的概览数据。
 *
 * 存在的理由：这个页面原先为了算四个总数和三张分布图，调的是四个"查全表"的接口
 * （consumers / songs / singers / song-lists 的不分页版本），把全部用户的手机号、
 * 邮箱连同头像地址整表搬进浏览器再在前端数个数。计数这件事在 SQL 侧一次扫描就能完成，
 * 出参也只有几十个数字，既不再把 PII 发到前端，首屏体积也从整表降到聚合对象。
 *
 * 分组只给"库里有哪些值、各多少行"：性别码到中文标签、地区与风格到图表分桶的
 * 归并规则属于展示层，留在页面里，避免把图表口径写进 SQL。
 */
@Data
public class OverviewVo {

    /**
     * 用户总数
     */
    private Long consumerTotal;

    /**
     * 歌曲总数
     */
    private Long songTotal;

    /**
     * 歌手总数
     */
    private Long singerTotal;

    /**
     * 歌单总数
     */
    private Long songListTotal;

    /**
     * 用户按性别分组，label 为 sex 列原值
     */
    private List<StatCountVo> consumerSex;

    /**
     * 歌手按性别分组，label 为 sex 列原值
     */
    private List<StatCountVo> singerSex;

    /**
     * 歌手按地区分组，label 为 location 列原值
     */
    private List<StatCountVo> singerLocation;

    /**
     * 歌单按风格分组，label 为 style 列原值
     */
    private List<StatCountVo> songListStyle;
}

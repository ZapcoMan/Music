package com.shanzhu.music.mapper;

import com.shanzhu.music.entity.vo.StatCountVo;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 管理端资料页的计数与分组查询。
 *
 * 这里刻意不复用各业务 Mapper 的"查全表"方法再在 Java 里数：那正是页面慢和
 * PII 外发的来源。count 与 group by 都留在 SQL 侧，一行出参换一张图。
 */
@Mapper
public interface StatisticsMapper {

    /**
     * 用户总数
     */
    Long countConsumer();

    /**
     * 歌曲总数
     */
    Long countSong();

    /**
     * 歌手总数
     */
    Long countSinger();

    /**
     * 歌单总数
     */
    Long countSongList();

    /**
     * 用户按性别分组计数
     */
    List<StatCountVo> groupConsumerSex();

    /**
     * 歌手按性别分组计数
     */
    List<StatCountVo> groupSingerSex();

    /**
     * 歌手按地区分组计数，取值来自 location 列原文
     */
    List<StatCountVo> groupSingerLocation();

    /**
     * 歌单按风格分组计数，取值来自 style 列原文
     */
    List<StatCountVo> groupSongListStyle();
}

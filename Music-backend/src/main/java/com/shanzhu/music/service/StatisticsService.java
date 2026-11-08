package com.shanzhu.music.service;

import com.shanzhu.music.entity.vo.OverviewVo;

/**
 * 管理端统计服务
 */
public interface StatisticsService {

    /**
     * 资料页概览：四个总数 + 三张分布图的分组计数，全部在 SQL 侧完成
     *
     * @return 概览数据
     */
    OverviewVo overview();
}

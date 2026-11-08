package com.shanzhu.music.controller;

import com.shanzhu.music.annotation.RequireUserType;
import com.shanzhu.music.common.R;
import com.shanzhu.music.entity.vo.OverviewVo;
import com.shanzhu.music.service.StatisticsService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 统计 控制层
 *
 * 只读，且只对管理端开放：这些数字是给后台看板用的聚合值，
 * 消费者侧不需要、也不该看到用户总量。
 */
@RestController
@RequestMapping("/api/v1/statistics")
@RequireUserType("admin")
@RequiredArgsConstructor
@io.swagger.v3.oas.annotations.tags.Tag(name = "统计", description = "管理端概览聚合接口")
public class StatisticsController {

    private final StatisticsService statisticsService;

    /**
     * 资料页概览
     *
     * @return 四个总数与三组分布计数
     */
    @Operation(summary = "资料页概览", description = "总数与分组计数在 SQL 侧完成，不回传任何明细行")
    @GetMapping("/overview")
    public R<OverviewVo> overview() {
        return R.ok(statisticsService.overview());
    }
}

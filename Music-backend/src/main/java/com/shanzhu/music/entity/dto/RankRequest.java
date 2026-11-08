package com.shanzhu.music.entity.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 提交歌单评分请求 DTO（表单绑定）。
 *
 * 原先 Controller 直接收 Integer songListId, Integer score 两个裸参数，
 * 一个约束都没有，而评分是协同过滤与精排的直接输入：
 * score 传 null 会落一条空分，那行仍被平均分的分母计入，把歌单评分无声拉低；
 * score 传 999 则直接把平均分拉到量纲之外。污染它比污染评论更难发现。
 */
@Data
public class RankRequest {

    @NotNull(message = "歌单id不能为空")
    private Integer songListId;

    /**
     * 1-10，与 ranks.score 的量纲以及 recommendation.rating-max 一致。
     * 前端是 5 星（allow-half），提交前 ×2，所以 0.5 星也正好落在整数刻度上。
     */
    @NotNull(message = "评分不能为空")
    @Min(value = 1, message = "评分范围是 1-10")
    @Max(value = 10, message = "评分范围是 1-10")
    private Integer score;
}

package com.shanzhu.music.entity.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 评分请求DTO
 */
@Data
public class RankRequest {

    @NotNull(message = "歌单ID不能为空")
    private Integer songListId;

    @NotNull(message = "用户ID不能为空")
    private Integer consumerId;

    @NotNull(message = "评分不能为空")
    @Min(value = 1, message = "评分最低为1分")
    @Max(value = 5, message = "评分最高为5分")
    private Integer score;
}

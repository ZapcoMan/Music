package com.shanzhu.music.entity.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 收藏请求DTO
 */
@Data
public class CollectRequest {

    private Integer id;

    @NotNull(message = "用户ID不能为空")
    private Integer userId;

    @NotNull(message = "收藏类型不能为空")
    private Byte type;

    private Integer songId;

    private Integer songListId;
}

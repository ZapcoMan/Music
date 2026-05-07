package com.shanzhu.music.entity.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 歌单歌曲关联请求DTO
 */
@Data
public class ListSongRequest {

    private Integer id;

    @NotNull(message = "歌曲ID不能为空")
    private Integer songId;

    @NotNull(message = "歌单ID不能为空")
    private Integer songListId;
}

package com.shanzhu.music.entity.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 歌曲请求DTO
 */
@Data
public class SongRequest {

    private Integer id;

    @NotNull(message = "歌手ID不能为空")
    private Integer singerId;

    @NotBlank(message = "歌曲名称不能为空")
    @Size(min = 1, max = 100, message = "歌曲名称长度必须在1-100之间")
    private String name;

    @Size(max = 500, message = "简介长度不能超过500个字符")
    private String introduction;

    private String lyric;
}

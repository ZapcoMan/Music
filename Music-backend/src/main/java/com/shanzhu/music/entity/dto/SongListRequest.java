package com.shanzhu.music.entity.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 歌单请求DTO
 */
@Data
public class SongListRequest {

    private Integer id;

    @NotBlank(message = "歌单标题不能为空")
    @Size(min = 1, max = 100, message = "标题长度必须在1-100之间")
    private String title;

    @Size(max = 500, message = "简介长度不能超过500个字符")
    private String introduction;

    @Size(max = 50, message = "风格长度不能超过50个字符")
    private String style;
}

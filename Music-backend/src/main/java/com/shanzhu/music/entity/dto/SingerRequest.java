package com.shanzhu.music.entity.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 歌手请求DTO
 */
@Data
public class SingerRequest {

    private Integer id;

    @NotBlank(message = "歌手名称不能为空")
    @Size(min = 1, max = 50, message = "歌手名称长度必须在1-50之间")
    private String name;

    @NotNull(message = "性别不能为空")
    private Byte sex;

    private String birth;

    @Size(max = 100, message = "地区长度不能超过100个字符")
    private String location;

    @Size(max = 500, message = "简介长度不能超过500个字符")
    private String introduction;
}

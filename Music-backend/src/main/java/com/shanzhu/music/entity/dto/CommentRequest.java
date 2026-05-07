package com.shanzhu.music.entity.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 评论请求DTO
 */
@Data
public class CommentRequest {

    private Integer id;

    @NotNull(message = "用户ID不能为空")
    private Integer userId;

    @NotBlank(message = "评论内容不能为空")
    @Size(min = 1, max = 500, message = "评论内容长度必须在1-500之间")
    private String content;

    @NotNull(message = "评论类型不能为空")
    private Byte type;

    private Integer songId;

    private Integer songListId;
}

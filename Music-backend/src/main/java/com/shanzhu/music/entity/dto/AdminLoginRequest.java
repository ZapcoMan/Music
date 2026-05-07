package com.shanzhu.music.entity.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 管理员登录请求DTO
 */
@Data
public class AdminLoginRequest {

    @NotBlank(message = "管理员账号不能为空")
    @Size(min = 1, max = 50, message = "账号长度必须在1-50之间")
    private String name;

    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 50, message = "密码长度必须在6-50之间")
    private String password;
}

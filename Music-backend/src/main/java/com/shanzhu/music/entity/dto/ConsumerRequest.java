package com.shanzhu.music.entity.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 用户注册/更新请求DTO
 */
@Data
public class ConsumerRequest {

    @NotBlank(message = "用户名不能为空")
    @Size(min = 2, max = 50, message = "用户名长度必须在2-50之间")
    private String username;

    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 50, message = "密码长度必须在6-50之间")
    private String password;

    private Byte sex;

    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phoneNum;

    @Email(message = "邮箱格式不正确")
    private String email;

    private String birth;

    @Size(max = 255, message = "签名长度不能超过255个字符")
    private String introduction;

    @Size(max = 100, message = "地区长度不能超过100个字符")
    private String location;
}

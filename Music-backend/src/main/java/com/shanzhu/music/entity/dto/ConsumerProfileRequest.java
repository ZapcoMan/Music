package com.shanzhu.music.entity.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 修改本人资料请求 DTO。
 *
 * 与注册用的 {@link ConsumerRequest} 分开，是因为两者的必填口径本就不同：
 * 注册必须有用户名与口令，而改资料既不改动登录凭据，也允许用户留着口令不动。
 * 此前复用 ConsumerRequest，它的 {@code @NotBlank(password)} 会在请求进入
 * service 之前就把"只想改个签名"的提交判成 400——Info.vue 里那句
 * "留空即不发送该字段，后端 password 为 null 时保持原密码不变"的约定
 * 从来没有机会生效。
 *
 * 这里刻意不使用 {@code @NotBlank}：{@code @Size} / {@code @Pattern} /
 * {@code @Email} 对 null 一律放行，正好对应"不填即不改"。
 */
@Data
public class ConsumerProfileRequest {

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

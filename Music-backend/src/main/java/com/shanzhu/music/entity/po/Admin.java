package com.shanzhu.music.entity.po;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

import java.io.Serializable;

/**
 * 管理员
 */
@Data
public class Admin implements Serializable {

    /**
     * 主键
     */
    private Integer id;

    /**
     * 账号
     */
    private String name;

    /**
     * 密码（BCrypt 散列）
     *
     * 与 Consumer.password 同口径：登录出参本来就是 AdminLoginVo，这个字段从来
     * 不该出现在任何响应体里。加注解是为了让"下一个人顺手 return R.ok(admin)"
     * 变成一次编译期就看不见的失败，而不是一个静默的凭据外泄。
     */
    @JsonIgnore
    private String password;

}

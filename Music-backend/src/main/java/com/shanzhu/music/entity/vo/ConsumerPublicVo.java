package com.shanzhu.music.entity.vo;

import lombok.Data;

/**
 * 用户公开信息
 *
 * /consumer/selectByPrimaryKey 会被用来查询"别人"（评论区展示作者头像和昵称），
 * 因此只暴露与身份展示相关的字段。手机号、邮箱、生日、地区、签名等私密资料
 * 走 /consumer/profile，由令牌身份决定只能拿到自己的。
 */
@Data
public class ConsumerPublicVo {

    /**
     * 主键
     */
    private Integer id;

    /**
     * 账号
     */
    private String username;

    /**
     * 头像
     */
    private String avator;

}

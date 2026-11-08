package com.shanzhu.music.entity.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 发表评论请求 DTO。
 *
 * 此前 Controller 直接收 5 个裸参数（Byte type, Integer songId, Integer songListId,
 * String content），一个校验注解都没有：content 可以为 null、可以为空串、
 * 也可以长到被数据库截断；type 传 7 一样能走下去。
 *
 * 采用字段绑定而不是 @RequestBody：前端 Comment.vue 发的是 URLSearchParams
 * （表单），端点保持 @Valid + 对象绑定的写法就不必同时改前端，
 * 校验失败由 GlobalExceptionHandler 的 BindException 分支统一转成 400。
 */
@Data
public class CommentRequest {

    /**
     * 评论类型：0 歌曲、1 歌单。
     * 限死区间是因为原实现只判 null，其余值一律走"歌单"分支——
     * type=7 会拿 songListId 建一条谁都读不到的评论。
     */
    @NotNull(message = "评论类型不能为空")
    @Min(value = 0, message = "评论类型只能是 0（歌曲）或 1（歌单）")
    @Max(value = 1, message = "评论类型只能是 0（歌曲）或 1（歌单）")
    private Byte type;

    private Integer songId;

    private Integer songListId;

    /**
     * 上限 255 与 comment.content 的 varchar(255) 对齐；
     * 超过它在严格模式下是报错、在非严格模式下是被静默截断，两者都不该交给数据库决定。
     */
    @NotBlank(message = "评论内容不能为空")
    @Size(max = 255, message = "评论内容不能超过255字")
    private String content;

    /**
     * 评论必须挂在真实存在的目标上：type 与目标 id 是配对关系，
     * 属于跨字段约束，所以写在 DTO 上而不是散进 Controller。
     *
     * 原实现只在 type == 0 时检查 songId 非空，type == 1 且 songListId 为 null 时
     * 会插入一条两个外键列都为空的评论——它不出现在任何列表查询里，
     * 却占用自增 id 并被点赞计数等统计扫到。
     */
    @AssertTrue(message = "歌曲评论要带 songId，歌单评论要带 songListId")
    public boolean isTargetPresent() {
        // type 缺失时由 @NotNull 报错，这里不再重复报一条，
        // 否则同一次提交会出现两条指向同一处问题的提示
        if (type == null) {
            return true;
        }
        return type == 0 ? songId != null : songListId != null;
    }
}

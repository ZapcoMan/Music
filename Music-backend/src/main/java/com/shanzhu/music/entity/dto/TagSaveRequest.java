package com.shanzhu.music.entity.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 批量新增标签的单条请求。
 *
 * 此前 batchAdd 直接收 List&lt;Tag&gt;——Tag 是 tag 表的持久层对象，
 * 于是 Controller 拿到手就开始就地改写它（trim、补默认 type），
 * web 层因此可以往实体里塞任何字段，而其中一部分会被当成列写进库。
 * 换成只含业务意图的请求对象之后，落什么值由 service 决定。
 */
@Data
public class TagSaveRequest {

    @NotBlank(message = "标签名不能为空")
    @Size(max = 50, message = "标签名长度不能超过50")
    private String name;

    /**
     * 0风格 1情感 2场景 3语言 4其他。
     * 允许缺省：字典表里 type 是可空列，service 会把缺省值统一落到 4（其他），
     * 这个"落到哪一类"是业务约定，所以由 service 决定，不交给调用方猜。
     */
    @Min(value = 0, message = "标签类型取值范围是 0-4")
    @Max(value = 4, message = "标签类型取值范围是 0-4")
    private Byte type;
}

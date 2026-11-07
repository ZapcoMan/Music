package com.shanzhu.music.common;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

/**
 * 统一返回结果。
 *
 * 两类改动值得说明。其一是删掉了此前与 data 并存的 dataMap 载体：它在后端没有任何一处
 * 赋值、前端也没有任何一处读取，只是把两种返回风格焊在同一个类上，让"这个接口到底返回哪
 * 个字段"变成口头约定。其二是所有静态工厂补齐泛型参数，不再返回 raw R——raw 类型会让调用
 * 处退化成无检查的赋值，编译器再也看不出接口声明与实际返回是否一致。
 */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL) // 只序列化非 null 的字段
public class R<T> {
    // 表示操作是否成功
    private Boolean success;
    // 状态码
    private Integer code;
    // 状态信息
    private String message;

    // 载荷
    private T data;

    // 构造器私有，防止外部实例化
    private R() {}

    // 通用返回成功的方法
    public static <T> R<T> ok() {
        return of(ResultCodeEnum.SUCCESS, ResultCodeEnum.SUCCESS.getMessage(), null);
    }

    // 通用返回成功并携带消息的方法
    public static <T> R<T> ok(String message) {
        return of(ResultCodeEnum.SUCCESS, message, null);
    }

    // 通用返回成功并携带数据的方法
    public static <T> R<T> ok(T data) {
        return of(ResultCodeEnum.SUCCESS, ResultCodeEnum.SUCCESS.getMessage(), data);
    }

    // 通用返回成功并携带消息和数据的方法
    public static <T> R<T> ok(String message, T data) {
        return of(ResultCodeEnum.SUCCESS, message, data);
    }

    /**
     * 与 ok(data) 同义的历史工厂。
     * 此前它漏了 setSuccess，而 success 字段带 NON_NULL 序列化策略，
     * 于是响应体里根本没有 success 这个键——前端 res.success === true 恒为假。
     * 同一个坑在 error(Integer, String) 上已经修过并留了注释，这里当时没一起改。
     */
    public static <T> R<T> success(T data) {
        return of(ResultCodeEnum.SUCCESS, "请求成功", data);
    }

    // 通用返回失败的方法，用于未知错误
    public static <T> R<T> error() {
        return of(ResultCodeEnum.UNKNOWN_ERROR, ResultCodeEnum.UNKNOWN_ERROR.getMessage(), null);
    }

    public static <T> R<T> error(Integer code, String msg) {
        R<T> result = new R<>();
        // 必须显式置 success=false：否则该字段序列化为 null，
        // 前端 res.success === true 的判定永远为假，成功与失败无法区分
        result.setSuccess(false);
        result.setCode(code);
        result.setMessage(msg);
        return result;
    }

    public static <T> R<T> error(String msg) {
        return error(ResultCodeEnum.UNKNOWN_ERROR.getCode(), msg);
    }

    // 通用返回失败的方法，接受一个结果枚举作为参数
    public static <T> R<T> setResult(ResultCodeEnum result) {
        return of(result, result.getMessage(), null);
    }

    private static <T> R<T> of(ResultCodeEnum result, String message, T data) {
        R<T> r = new R<>();
        r.setSuccess(result.getSuccess());
        r.setCode(result.getCode());
        r.setMessage(message);
        r.setData(data);
        return r;
    }
}

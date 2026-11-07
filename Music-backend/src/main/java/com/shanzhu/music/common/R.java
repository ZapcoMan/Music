package com.shanzhu.music.common;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.util.HashMap;
import java.util.Map;

/**
 * 封装返回结果的类，使用 Lombok 注解减少 boilerplate 代码
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

    // 存放数据的 Object
    private T data;

    // 存放数据的 Map
    private Map<String, Object> dataMap = new HashMap<>();

    // 构造器私有，防止外部实例化
    private R() {}

    // 通用返回成功的方法
    public static R ok() {
        R r = new R();
        r.setSuccess(ResultCodeEnum.SUCCESS.getSuccess());
        r.setCode(ResultCodeEnum.SUCCESS.getCode());
        r.setMessage(ResultCodeEnum.SUCCESS.getMessage());
        return r;
    }

    // 通用返回成功并携带消息的方法
    public static R ok(String message) {
        R r = new R();
        r.setSuccess(ResultCodeEnum.SUCCESS.getSuccess());
        r.setCode(ResultCodeEnum.SUCCESS.getCode());
        r.setMessage(message);
        return r;
    }

    // 通用返回成功并携带数据的方法
    public static <T> R<T> ok(T data) {
        R<T> r = new R<>();
        r.setSuccess(ResultCodeEnum.SUCCESS.getSuccess());
        r.setCode(ResultCodeEnum.SUCCESS.getCode());
        r.setMessage(ResultCodeEnum.SUCCESS.getMessage());
        r.setData(data);
        return r;
    }

    // 通用返回成功并携带消息和数据的方法
    public static <T> R<T> ok(String message, T data) {
        R<T> r = new R<>();
        r.setSuccess(ResultCodeEnum.SUCCESS.getSuccess());
        r.setCode(ResultCodeEnum.SUCCESS.getCode());
        r.setMessage(message);
        r.setData(data);
        return r;
    }

    public static <T> R<T> success(T data) {
        R<T> result = new R<>();
        result.setCode(ResultCodeEnum.SUCCESS.getCode());
        result.setData(data);
        result.setMessage("请求成功");
        return result;
    }

    // 通用返回失败的方法，用于未知错误
    public static R error() {
        R r = new R();
        r.setSuccess(ResultCodeEnum.UNKNOWN_ERROR.getSuccess());
        r.setCode(ResultCodeEnum.UNKNOWN_ERROR.getCode());
        r.setMessage(ResultCodeEnum.UNKNOWN_ERROR.getMessage());
        return r;
    }

    public static R error(Integer code, String msg) {
        R result = new R();
        // 必须显式置 success=false：否则该字段序列化为 null，
        // 前端 res.success === true 的判定永远为假，成功与失败无法区分
        result.setSuccess(false);
        result.setCode(code);
        result.setMessage(msg);
        return result;
    }

    public static R error(String msg) {
        R result = new R();
        result.setSuccess(false);
        result.setCode(ResultCodeEnum.UNKNOWN_ERROR.getCode());
        result.setMessage(msg);
        return result;
    }

    // 设置结果的方法，接受一个结果枚举作为参数
    public static R setResult(ResultCodeEnum result) {
        R r = new R();
        r.setSuccess(result.getSuccess());
        r.setCode(result.getCode());
        r.setMessage(result.getMessage());
        return r;
    }

    /**
     * ------------使用链式编程，返回类本身-----------
     **/

    // 自定义返回数据的方法，接受一个 Map 作为参数
    public R<T> data(Map<String, Object> map) {
        this.setData(null); // 清空 data
        this.setDataMap(map);
        return this;
    }

    // 通用设置data的方法，接受一个键值对作为参数
    public R<T> data(String key, Object value) {
        this.setData(null); // 清空 data
        this.dataMap.put(key, value);
        return this;
    }

    // 自定义状态信息的方法
    public R<T> message(String message) {
        this.setMessage(message);
        return this;
    }

    // 自定义状态码的方法
    public R<T> code(Integer code) {
        this.setCode(code);
        return this;
    }

    // 自定义返回结果的方法
    public R<T> success(Boolean success) {
        this.setSuccess(success);
        return this;
    }

    // 自定义序列化：只返回 data 或 dataMap，不同时返回
//    public Map<String, Object> getJsonResponse() {
//        if (this.data != null) {
//            return Map.of("data", this.data);
//        } else if (!this.dataMap.isEmpty()) {
//            return Map.of("dataMap", this.dataMap);
//        }
//        return new HashMap<>();
//    }
}

package com.shanzhu.music.entity.po;

import lombok.Data;

import java.util.Date;

/**
 * 操作审计日志实体
 */
@Data
public class OperationLog {

    /**
     * 主键ID
     */
    private Integer id;

    /**
     * 操作用户ID
     */
    private Integer userId;

    /**
     * 操作用户名
     */
    private String username;

    /**
     * 用户类型(admin/consumer)
     */
    private String userType;

    /**
     * 操作类型(LOGIN/LOGOUT/ADD/UPDATE/DELETE/QUERY)
     */
    private String operationType;

    /**
     * 操作模块(用户管理/歌曲管理等)
     */
    private String module;

    /**
     * 操作描述
     */
    private String description;

    /**
     * 请求URL
     */
    private String requestUrl;

    /**
     * 请求方法(GET/POST/PUT/DELETE)
     */
    private String requestMethod;

    /**
     * 请求参数
     */
    private String requestParams;

    /**
     * 响应结果
     */
    private String responseResult;

    /**
     * 操作IP地址
     */
    private String ipAddress;

    /**
     * 操作耗时(毫秒)
     */
    private Long duration;

    /**
     * 操作状态(SUCCESS/FAILED)
     */
    private String status;

    /**
     * 错误信息
     */
    private String errorMsg;

    /**
     * 操作时间
     */
    private Date createTime;
}

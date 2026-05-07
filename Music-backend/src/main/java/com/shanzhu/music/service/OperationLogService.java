package com.shanzhu.music.service;

import com.shanzhu.music.entity.po.OperationLog;

import java.util.List;

/**
 * 操作审计日志服务
 */
public interface OperationLogService {

    /**
     * 记录操作日志
     */
    void logOperation(OperationLog log);

    /**
     * 记录登录日志
     */
    void logLogin(Integer userId, String username, String userType, String ip, boolean success);

    /**
     * 记录登出日志
     */
    void logLogout(Integer userId, String username, String userType, String ip);

    /**
     * 记录添加操作
     */
    void logAdd(Integer userId, String username, String userType, String module, String description, String ip);

    /**
     * 记录更新操作
     */
    void logUpdate(Integer userId, String username, String userType, String module, String description, String ip);

    /**
     * 记录删除操作
     */
    void logDelete(Integer userId, String username, String userType, String module, String description, String ip);

    /**
     * 查询日志列表
     */
    List<OperationLog> getAllLogs();

    /**
     * 根据用户ID查询
     */
    List<OperationLog> getByUserId(Integer userId);

    /**
     * 清理过期日志
     */
    void cleanExpiredLogs(Integer days);
}

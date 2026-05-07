package com.shanzhu.music.service.impl;

import com.shanzhu.music.entity.po.OperationLog;
import com.shanzhu.music.mapper.OperationLogMapper;
import com.shanzhu.music.service.OperationLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

/**
 * 操作审计日志服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OperationLogServiceImpl implements OperationLogService {

    private final OperationLogMapper operationLogMapper;

    /**
     * 异步记录操作日志(不影响主业务流程)
     */
    @Async
    @Override
    public void logOperation(OperationLog operationLog) {
        try {
            operationLog.setCreateTime(new Date());
            operationLogMapper.insert(operationLog);
            log.debug("操作日志记录成功 - userId={}, operation={}", operationLog.getUserId(), operationLog.getOperationType());
        } catch (Exception e) {
            log.error("操作日志记录失败", e);
        }
    }

    @Override
    public void logLogin(Integer userId, String username, String userType, String ip, boolean success) {
        OperationLog log = new OperationLog();
        log.setUserId(userId);
        log.setUsername(username);
        log.setUserType(userType);
        log.setOperationType("LOGIN");
        log.setModule("认证模块");
        log.setDescription(username + " 登录系统");
        log.setIpAddress(ip);
        log.setStatus(success ? "SUCCESS" : "FAILED");
        
        logOperation(log);
    }

    @Override
    public void logLogout(Integer userId, String username, String userType, String ip) {
        OperationLog log = new OperationLog();
        log.setUserId(userId);
        log.setUsername(username);
        log.setUserType(userType);
        log.setOperationType("LOGOUT");
        log.setModule("认证模块");
        log.setDescription(username + " 退出系统");
        log.setIpAddress(ip);
        log.setStatus("SUCCESS");
        
        logOperation(log);
    }

    @Override
    public void logAdd(Integer userId, String username, String userType, String module, String description, String ip) {
        OperationLog log = new OperationLog();
        log.setUserId(userId);
        log.setUsername(username);
        log.setUserType(userType);
        log.setOperationType("ADD");
        log.setModule(module);
        log.setDescription(description);
        log.setIpAddress(ip);
        log.setStatus("SUCCESS");
        
        logOperation(log);
    }

    @Override
    public void logUpdate(Integer userId, String username, String userType, String module, String description, String ip) {
        OperationLog log = new OperationLog();
        log.setUserId(userId);
        log.setUsername(username);
        log.setUserType(userType);
        log.setOperationType("UPDATE");
        log.setModule(module);
        log.setDescription(description);
        log.setIpAddress(ip);
        log.setStatus("SUCCESS");
        
        logOperation(log);
    }

    @Override
    public void logDelete(Integer userId, String username, String userType, String module, String description, String ip) {
        OperationLog log = new OperationLog();
        log.setUserId(userId);
        log.setUsername(username);
        log.setUserType(userType);
        log.setOperationType("DELETE");
        log.setModule(module);
        log.setDescription(description);
        log.setIpAddress(ip);
        log.setStatus("SUCCESS");
        
        logOperation(log);
    }

    @Override
    public List<OperationLog> getAllLogs() {
        return operationLogMapper.selectAll();
    }

    @Override
    public List<OperationLog> getByUserId(Integer userId) {
        return operationLogMapper.selectByUserId(userId);
    }

    @Override
    public void cleanExpiredLogs(Integer days) {
        int count = operationLogMapper.deleteExpiredLogs(days);
        log.info("清理过期日志完成 - 删除{}条记录,保留{}天内的日志", count, days);
    }
}

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

    private static final String STATUS_SUCCESS = "SUCCESS";
    private static final String STATUS_FAILED = "FAILED";

    /**
     * 认证模块名，登录/登出共用
     */
    private static final String MODULE_AUTH = "认证模块";

    /**
     * 异步记录操作日志(不影响主业务流程)。
     * 显式指定 auditExecutor：不写 Executor 名的话 Spring 按"唯一的 TaskExecutor bean"挑选，
     * 一旦本应用再引入其它执行器，日志写入会静默回到调用线程执行。
     */
    @Async("auditExecutor")
    @Override
    public void logOperation(OperationLog operationLog) {
        try {
            operationLog.setCreateTime(new Date());
            operationLogMapper.insert(operationLog);
            log.debug("操作日志记录成功 - userId={}, operation={}", operationLog.getUserId(), operationLog.getOperationType());
        } catch (Exception e) {
            // 审计是旁路，写失败只降级告警，不能反过来影响业务请求
            log.warn("操作日志记录失败 - operation={}, err={}", operationLog.getOperationType(), e.toString());
        }
    }

    @Override
    public void logLogin(Integer userId, String username, String userType, String ip, boolean success) {
        record("LOGIN", MODULE_AUTH, userId, username, userType, ip,
                success ? STATUS_SUCCESS : STATUS_FAILED, username + " 登录系统");
    }

    @Override
    public void logLogout(Integer userId, String username, String userType, String ip) {
        record("LOGOUT", MODULE_AUTH, userId, username, userType, ip,
                STATUS_SUCCESS, username + " 退出系统");
    }

    @Override
    public void logAdd(Integer userId, String username, String userType, String module, String description, String ip) {
        record("ADD", module, userId, username, userType, ip, STATUS_SUCCESS, description);
    }

    @Override
    public void logUpdate(Integer userId, String username, String userType, String module, String description, String ip) {
        record("UPDATE", module, userId, username, userType, ip, STATUS_SUCCESS, description);
    }

    @Override
    public void logDelete(Integer userId, String username, String userType, String module, String description, String ip) {
        record("DELETE", module, userId, username, userType, ip, STATUS_SUCCESS, description);
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

    /**
     * 五个 logXxx 此前各抄一份同样的赋值序列，只差操作类型与状态；
     * 收敛到这一处，新增操作类型不必再复制第六份模板。
     */
    private void record(String operationType, String module, Integer userId, String username,
                        String userType, String ip, String status, String description) {
        OperationLog entry = new OperationLog();
        entry.setUserId(userId);
        entry.setUsername(username);
        entry.setUserType(userType);
        entry.setOperationType(operationType);
        entry.setModule(module);
        entry.setDescription(description);
        entry.setIpAddress(ip);
        entry.setStatus(status);
        logOperation(entry);
    }
}

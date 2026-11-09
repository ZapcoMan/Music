package com.shanzhu.music.task;

import com.shanzhu.music.service.OperationLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 审计日志保留期清理。
 *
 * 审计接线后 operation_log 只增不减，必须有清理方，否则它自己就成了那个
 * "无 LIMIT 且整表写入 Redis"一类的隐患。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OperationLogCleanupTask {

    private final OperationLogService operationLogService;

    /**
     * 保留天数，默认 90 天
     */
    @Value("${audit.log.retention-days:90}")
    private int retentionDays;

    /**
     * 每天 03:30 执行，避开业务高峰
     */
    @Scheduled(cron = "0 30 3 * * ?")
    public void cleanup() {
        // 定时任务
        try {
            operationLogService.cleanExpiredLogs(retentionDays);
        } catch (Exception e) {
            // 清理失败不影响业务，留日志等下一次调度
            log.warn("审计日志清理失败 - err={}", e.toString());
        }
    }
}

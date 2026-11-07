package com.shanzhu.music.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 定时任务开关。
 * OperationLogService.cleanExpiredLogs 此前没有任何调用方，也没有 @EnableScheduling，
 * 审计表一旦开始写入就是无界增长 —— 这里显式启用调度，让清理真正跑起来。
 */
@EnableScheduling
@Configuration
public class SchedulingConfig {
}

package com.shanzhu.music.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.aop.interceptor.SimpleAsyncUncaughtExceptionHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 审计日志异步执行器。
 *
 * 背景：OperationLogServiceImpl.logOperation 一直标着 @Async 并注释"不影响主业务流程"，
 * 但全项目此前没有 @EnableAsync，该注解完全不生效 —— 所谓异步实际是在请求线程里
 * 同步做一条 INSERT，接线后等于给每个写操作加一次数据库往返。
 * 这里显式启用并提供有界线程池，而不是让 Spring 退化成"每任务一个线程"的实现。
 */
@Slf4j
@EnableAsync
@Configuration
public class AsyncConfig implements AsyncConfigurer {

    /**
     * 审计写入专用线程池。
     * 队列满时丢弃最旧任务而非阻塞调用方：审计是可牺牲的旁路，
     * 让它反压到业务线程是把轻缺陷换成重缺陷。
     */
    @Bean("auditExecutor")
    public Executor auditExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(500);
        executor.setKeepAliveSeconds(60);
        executor.setThreadNamePrefix("audit-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.DiscardOldestPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(10);
        executor.initialize();
        return executor;
    }

    @Override
    public Executor getAsyncExecutor() {
        return auditExecutor();
    }

    /**
     * @Async 方法抛出的异常不会回到调用方，没有这个处理器就是静默丢失。
     */
    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return new SimpleAsyncUncaughtExceptionHandler();
    }
}

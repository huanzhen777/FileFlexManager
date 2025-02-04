package com.huanzhen.fileflexmanager.application.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

@Configuration
@EnableAsync
public class ThreadPoolConfig {
    
    @Bean("taskExecutor")
    public Executor taskExecutor() {
        // 获取CPU核心数
        int cpuCores = Runtime.getRuntime().availableProcessors();
        
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        // IO密集型任务，线程数设置为CPU核数的2-3倍
        executor.setCorePoolSize(cpuCores * 2);
        executor.setMaxPoolSize(cpuCores * 3);
        // 队列容量设置为核心线程数的2倍
        executor.setQueueCapacity(cpuCores * 4);
        // 空闲线程存活时间
        executor.setKeepAliveSeconds(300);
        executor.setThreadNamePrefix("io-task-");
        // 队列满了，使用提交任务的线程来执行
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        // 允许核心线程超时，避免资源浪费
        executor.setAllowCoreThreadTimeOut(true);
        executor.initialize();
        return executor;
    }

    /**
     * 用于监控线程池状态的方法
     */
    public static String getThreadPoolStatus(ThreadPoolTaskExecutor executor) {
        StringBuilder status = new StringBuilder();
        status.append("\n=========================\n");
        status.append("核心线程数: ").append(executor.getCorePoolSize())
              .append("\n活动线程数: ").append(executor.getActiveCount())
              .append("\n最大线程数: ").append(executor.getMaxPoolSize())
              .append("\n线程池大小: ").append(executor.getPoolSize())
              .append("\n队列大小: ").append(executor.getQueueSize())
              .append("\n=========================");
        return status.toString();
    }
} 
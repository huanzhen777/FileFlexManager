package com.huanzhen.fileflexmanager.domain.utils;

import org.springframework.scheduling.support.CronExpression;

import java.time.LocalDateTime;

public class CronUtils {
    /**
     * 获取下一次执行时间
     */
    public static LocalDateTime getNextExecuteTime(String cronExpression) {
        try {
            CronExpression cron = CronExpression.parse(cronExpression);
            return cron.next(LocalDateTime.now());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("无效的Cron表达式: " + cronExpression, e);
        }
    }

    /**
     * 验证Cron表达式是否有效
     */
    public static boolean isValid(String cronExpression) {
        try {
            CronExpression.parse(cronExpression);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * 常用Cron表达式示例
     */
    public static class Examples {
        public static final String EVERY_MINUTE = "0 * * * * *";           // 每分钟
        public static final String EVERY_HOUR = "0 0 * * * *";            // 每小时
        public static final String EVERY_DAY_MIDNIGHT = "0 0 0 * * *";    // 每天凌晨
        public static final String EVERY_WEEK_MONDAY = "0 0 0 * * MON";   // 每周一凌晨
        public static final String EVERY_MONTH_FIRST_DAY = "0 0 0 1 * *"; // 每月1号凌晨
    }
} 
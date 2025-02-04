package com.huanzhen.fileflexmanager.domain.exception;

/**
 * 任务取消异常
 */
public class TaskCancelledException extends RuntimeException {
    public TaskCancelledException() {
        super("任务已被取消");
    }

    public TaskCancelledException(String message) {
        super(message);
    }
} 
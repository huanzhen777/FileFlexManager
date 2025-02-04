package com.huanzhen.fileflexmanager.infrastructure.task.handler;

import cn.hutool.core.lang.Assert;
import com.huanzhen.fileflexmanager.domain.exception.TaskCancelledException;
import com.huanzhen.fileflexmanager.domain.model.entity.Task;
import com.huanzhen.fileflexmanager.domain.repository.TaskRepository;
import com.huanzhen.fileflexmanager.domain.service.TaskHandler;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
public abstract class BaseTaskHandler<T> implements TaskHandler<T> {

    protected final TaskRepository taskRepository;
    protected AtomicBoolean isCanceled = new AtomicBoolean(false);
    private long lastProgressUpdate = 0;
    private static final int UPDATE_PROGRESS_INTERVAL = 200; // 毫秒

    protected BaseTaskHandler(TaskRepository taskRepository) {
        this.taskRepository = taskRepository;
    }

    @Override
    public void handle(Task task) {
        try {
            task.begin();
            taskRepository.updateTask(task);

            doHandle(task);

            if (!task.isTerminal()) {
                task.markAsCompleted();
                taskRepository.updateTask(task);
            }
        } catch (TaskCancelledException e) {
            handleTaskCancellation(task, e.getMessage());
        } catch (Exception e) {
            handleTaskFailure(task, e);
        }
    }

    /**
     * 具体的任务处理逻辑
     * @param task 要处理的任务
     * @throws Exception 处理过程中的异常
     */
    protected abstract void doHandle(Task task) throws Exception;

    /**
     * 处理任务取消
     * @param task 被取消的任务
     * @param message 取消原因
     */
    protected void handleTaskCancellation(Task task, String message) {
        try {
            log.info("任务被取消: {}, 原因: {}", task.getId(), message);
            task.markAsCancelled(message);
            taskRepository.updateTask(task);
            onCancel(task);
        } catch (Exception e) {
            log.error("处理任务取消时发生错误", e);
        }
    }

    /**
     * 处理任务失败
     * @param task 失败的任务
     * @param e 导致失败的异常
     */
    protected void handleTaskFailure(Task task, Exception e) {
        log.error("任务执行失败: " + task.getId(), e);
        String errorMessage = e.getMessage();
        if (errorMessage == null || errorMessage.isEmpty()) {
            errorMessage = "任务执行失败";
        }
        task.markAsFailed(errorMessage);
        taskRepository.updateTask(task);
    }

    /**
     * 如果要支持取消，需要handler实现为多例，才能支持通过id找到handler取消
     * 如果支持取消，请在该方法清理资源
     */
    protected void onCancel(Task task) {
        throw new RuntimeException("该任务不支持取消");
    }

    @Override
    public void cancel(Task task) {
        onCancel(task);
        isCanceled.set(true);
    }

    /**
     * 检查任务是否被取消
     * @param task 要检查的任务
     * @throws TaskCancelledException 如果任务已被取消
     */
    protected void assertNotCancelled(Task task) {
        Assert.isTrue(!isCanceled.get(), TaskCancelledException::new);
    }

    /**
     * 检查任务是否被取消，并提供自定义消息
     * @param task 要检查的任务
     * @param message 自定义取消消息
     * @throws TaskCancelledException 如果任务已被取消
     */
    protected void assertNotCancelled(Task task, String message) {
        Assert.isTrue(!isCanceled.get(), () -> new TaskCancelledException(message));
    }

    /**
     * 更新任务进度，内置节流控制
     * @param task 要更新的任务
     * @param progress 进度值(0-100)
     * @param message 进度消息
     */
    protected void updateProgress(Task task, int progress, String message) {
        long now = System.currentTimeMillis();
        if (now - lastProgressUpdate >= UPDATE_PROGRESS_INTERVAL) {
            task.updateProgress(progress, message);
            taskRepository.updateTask(task);
            lastProgressUpdate = now;
        }
    }
} 
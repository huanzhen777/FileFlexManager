package com.huanzhen.fileflexmanager.application.service;

import cn.hutool.core.lang.Assert;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.huanzhen.fileflexmanager.domain.model.entity.Task;
import com.huanzhen.fileflexmanager.domain.model.enums.TaskType;
import com.huanzhen.fileflexmanager.domain.repository.TaskRepository;
import com.huanzhen.fileflexmanager.domain.service.TaskHandler;
import com.huanzhen.fileflexmanager.domain.service.TaskHandlerRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;

@Slf4j
@Service

@RequiredArgsConstructor
public class TaskApplicationService {
    private final TaskRepository taskRepository;

    private final Executor taskExecutor;

    private final TaskHandlerRegistry taskHandlerRegistry;


    /**
     * 创建并异步执行任务，不等待结果
     */
    public Task submitAsyncTask(TaskType type, JSONObject payload) {
        return submitAsyncTask(saveTask(type, payload));
    }

    private Task saveTask(TaskType type, JSONObject payload) {
        return saveTask(new Task(type, payload));
    }

    private Task saveTask(Task task) {
        TaskHandler handler = taskHandlerRegistry.getHandler(task.getType());
        if (StrUtil.isBlank(task.getDesc())) {
            task.setDesc(handler.getTaskDesc(task));
        }
        task = taskRepository.save(task);
        return task;
    }

    /**
     * 创建并同步执行任务，等待结果返回
     */
    public Task executeTask(Task task) {
        Assert.notNull(task);
        task = saveTask(task);
        executeTask(task.getId());
        return taskRepository.findById(task.getId());
    }

    @Transactional
    public Task executeTask(TaskType type, JSONObject payload) {
        return executeTask(new Task(type, payload));
    }

    public Task getTaskProgress(Long id) {
        return taskRepository.findById(id);
    }

    public List<Task> getPendingTasks() {
        return taskRepository.findPendingTasks();
    }

    private final Map<Long, TaskHandler> executeHandlers = new ConcurrentHashMap<>();

    public void executeTask(Long taskId) {
        Task task = taskRepository.findById(taskId);
        Assert.notNull(task);
        TaskHandler handler = taskHandlerRegistry.getHandler(task.getType());
        Assert.notNull(handler);
        try {
            executeHandlers.put(taskId, handler);
            task.begin();
            taskRepository.updateTask(task);
            handler.handle(task);
            if (task.getStatus() == null) {
                task.markAsCompleted();
            }
        } catch (Exception e) {
            log.error("任务处理失败: {}", taskId, e);
            task.markAsFailed(e.getMessage());
        } finally {
            executeHandlers.remove(taskId);
        }
        taskRepository.updateTask(task);
    }

    public void cancelTask(Long taskId) {
        Task task = taskRepository.findById(taskId);
        if (task == null) {
            throw new IllegalArgumentException("任务不存在: " + taskId);
        }

        if (!task.canCancel()) {
            throw new IllegalStateException("任务无法取消，当前状态: " + task.getStatus());
        }

        // 如果任务正在运行，先尝试通知处理器取消任务
        TaskHandler handler = executeHandlers.get(taskId);
        Assert.notNull(handler, "当前不支持取消");
        handler.cancel(task);
    }

    public List<Task> getRunningTasks() {
        return taskRepository.findRunningTasks();
    }

    public Page<Task> getAllTasks(int page, int size, Boolean includeCompleted) {
        return taskRepository.findAll(page, size, includeCompleted);
    }

    /**
     * 创建定时任务
     */
    public Task createScheduledTask(TaskType type, JSONObject payload, String cronExpression) {
        Task task = new Task(type, payload, cronExpression);
        TaskHandler handler = taskHandlerRegistry.getHandler(task.getType());
        task.setDesc(handler.getTaskDesc(task));
        return taskRepository.save(task);
    }

    /**
     * 启用定时任务
     */
    public Task enableScheduledTask(Long taskId) {
        Task task = taskRepository.findById(taskId);
        if (task == null) {
            throw new IllegalArgumentException("任务不存在: " + taskId);
        }
        task.enable();
        taskRepository.updateTask(task);
        return task;
    }

    /**
     * 禁用定时任务
     */
    public Task disableScheduledTask(Long taskId) {
        Task task = taskRepository.findById(taskId);
        if (task == null) {
            throw new IllegalArgumentException("任务不存在: " + taskId);
        }
        task.disable();
        taskRepository.updateTask(task);
        return task;
    }

    /**
     * 获取所有定时任务
     */
    public Page<Task> getScheduledTasks(int page, int size) {
        return taskRepository.findScheduledTasks(page, size);
    }

    /**
     * 提交异步任务（供定时任务调度器使用）
     */
    public Task submitAsyncTask(Task task) {
        Task finalTask = task;
        taskExecutor.execute(() -> {
            try {
                executeTask(finalTask.getId());
            } catch (Exception e) {
                log.error("执行任务失败: {}", finalTask.getId(), e);
            }
        });
        return task;
    }

    /**
     * 删除定时任务
     */
    public void deleteScheduledTask(Long taskId) {
        Task task = taskRepository.findById(taskId);
        if (task == null) {
            throw new IllegalArgumentException("任务不存在: " + taskId);
        }
        if (!task.getScheduled()) {
            throw new IllegalArgumentException("非定时任务无法删除: " + taskId);
        }
        taskRepository.deleteById(taskId);
    }


}
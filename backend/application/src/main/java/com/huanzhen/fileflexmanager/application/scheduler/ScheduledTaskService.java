package com.huanzhen.fileflexmanager.application.scheduler;

import com.huanzhen.fileflexmanager.application.service.TaskApplicationService;
import com.huanzhen.fileflexmanager.domain.model.entity.Task;
import com.huanzhen.fileflexmanager.domain.model.enums.TaskType;
import com.huanzhen.fileflexmanager.domain.repository.TaskRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class ScheduledTaskService implements ApplicationListener<ApplicationReadyEvent> {
    private final TaskRepository taskRepository;
    private final TaskApplicationService taskService;
    private volatile boolean isAppReady = false;
    // 使用Map来追踪每种任务类型的执行状态
    private final ConcurrentHashMap<TaskType, Boolean> runningTasks = new ConcurrentHashMap<>();

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        isAppReady = true;
    }

    @Scheduled(fixedRate = 60, timeUnit = TimeUnit.SECONDS) // 每5分钟检查一次
    public void scheduleTasks() {
        if (!isAppReady) {
            log.debug("应用程序尚未完全启动，跳过定时任务调度");
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        List<Task> scheduledTasks = taskRepository.findScheduledTasks();

        for (Task task : scheduledTasks) {
            try {
                if (!task.getEnabled() || task.getNextExecuteTime() == null) {
                    continue;
                }

                TaskType taskType = task.getType();
                // 检查同类型的任务是否正在执行
                if (isTaskTypeRunning(taskType)) {
                    log.debug("任务类型 {} 正在执行中，跳过本次调度", taskType);
                    continue;
                }

                if (now.isAfter(task.getNextExecuteTime())) {
                    try {
                        // 标记该类型任务开始执行
                        markTaskTypeAsRunning(taskType);

                        // 创建新的执行实例
                        Task executionTask = new Task(task.getType(), task.getPayload());
                        executionTask.setDesc(task.getDesc() + " (定时执行 #" + (task.getExecuteCount() + 1) + ")");
                        executionTask.setFromSchedule(true);
                        // 使用同步方式执行任务
                        taskService.executeTask(executionTask);

                        // 更新定时任务信息
                        task.updateScheduledExecuteInfo();
                        taskRepository.updateTask(task);
                    } finally {
                        // 无论成功失败，都标记该类型任务执行完成
                        markTaskTypeAsCompleted(taskType);
                    }
                }
            } catch (Exception e) {
                log.error("处理定时任务失败: {}", task.getId(), e);
            }
        }
    }

    private boolean isTaskTypeRunning(TaskType taskType) {
        return Boolean.TRUE.equals(runningTasks.get(taskType));
    }

    private void markTaskTypeAsRunning(TaskType taskType) {
        runningTasks.put(taskType, true);
    }

    private void markTaskTypeAsCompleted(TaskType taskType) {
        runningTasks.remove(taskType);
    }
} 
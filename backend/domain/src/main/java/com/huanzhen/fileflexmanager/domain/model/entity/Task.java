package com.huanzhen.fileflexmanager.domain.model.entity;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSONObject;
import com.huanzhen.fileflexmanager.domain.model.enums.TaskStatus;
import com.huanzhen.fileflexmanager.domain.model.enums.TaskType;
import com.huanzhen.fileflexmanager.domain.utils.CronUtils;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@NoArgsConstructor
@Data
public class Task {
    private Long id;
    private TaskType type;
    private TaskStatus status;
    private Integer progress;
    private String message;
    private String desc;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private JSONObject payload;
    private LocalDateTime beginTime;
    private LocalDateTime endTime;
    private Boolean scheduled;
    private Boolean fromSchedule;
    private String cronExpression;
    private Boolean enabled;
    private LocalDateTime nextExecuteTime;
    private Integer executeCount;
    private LocalDateTime lastExecuteTime;

    public Task(TaskType type, JSONObject payload) {
        this.type = type;
        this.payload = payload;
        this.status = TaskStatus.PENDING;
        this.progress = 0;
        this.createTime = LocalDateTime.now();
        this.updateTime = this.createTime;
    }

    public Task(TaskType type, JSONObject payload, String cronExpression) {
        this(type, payload);
        this.scheduled = true;
        this.cronExpression = cronExpression;
        this.enabled = true;
        this.executeCount = 0;
        if (payload.getBoolean("executeNow")) {
            this.nextExecuteTime = LocalDateTime.now();
        } else {
            this.nextExecuteTime = CronUtils.getNextExecuteTime(cronExpression);
        }
    }

    public void updateProgress(int progress, String message) {
        this.progress = progress;
        this.message = message;
        this.status = TaskStatus.RUNNING;
        this.updateTime = LocalDateTime.now();
    }

    public void markAsCompleted(String message) {
        this.status = TaskStatus.COMPLETED;
        this.progress = 100;
        this.message = message;
        this.endTime = LocalDateTime.now();
        this.updateTime = this.endTime;
    }

    public void markAsCompletedAndMsgAppend(String message) {
        if (StrUtil.isNotBlank(this.message)) {
            markAsCompleted(this.message + "\n" + message);
        } else {
            markAsCompleted(message);
        }
    }

    public void markAsCompleted() {
        markAsCompleted(TaskStatus.COMPLETED.getDescription());
    }

    public void markAsFailed(String errorMessage) {
        this.status = TaskStatus.FAILED;
        this.message = errorMessage;
        this.endTime = LocalDateTime.now();
        this.updateTime = this.endTime;
    }

    public void markAsCancelled(String message) {
        if (!status.canCancel()) {
            throw new IllegalStateException("当前状态不可取消: " + status);
        }
        this.status = TaskStatus.CANCELLED;
        this.message = message;
        this.endTime = LocalDateTime.now();
        this.updateTime = this.endTime;
    }

    public boolean canCancel() {
        return status.canCancel();
    }

    public boolean isTerminal() {
        return status.isTerminal();
    }

    public boolean isCompleted() {
        return status == TaskStatus.COMPLETED;
    }

    public void begin() {
        this.status = TaskStatus.RUNNING;
        this.beginTime = LocalDateTime.now();
        this.updateTime = this.beginTime;
    }

    public void updateScheduledExecuteInfo() {
        this.executeCount++;
        this.lastExecuteTime = LocalDateTime.now();
        this.nextExecuteTime = CronUtils.getNextExecuteTime(cronExpression);
    }

    public void enable() {
        if (!this.scheduled) {
            throw new IllegalStateException("非定时任务无法启用");
        }
        this.enabled = true;
        this.nextExecuteTime = CronUtils.getNextExecuteTime(cronExpression);
    }

    public void disable() {
        if (!this.scheduled) {
            throw new IllegalStateException("非定时任务无法禁用");
        }
        this.enabled = false;
        this.nextExecuteTime = null;
    }

    public boolean isCancelled() {
        return TaskStatus.CANCELLED.equals(this.status);
    }
}
package com.huanzhen.fileflexmanager.interfaces.model.req;

import com.huanzhen.fileflexmanager.domain.model.enums.TaskType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.Map;

@Data
public class CreateScheduledTaskRequest {
    /**
     * 任务类型
     */
    @NotNull(message = "任务类型不能为空")
    private TaskType type;

    /**
     * 任务参数
     */
    @NotNull(message = "任务参数不能为空")
    private Map<String, Object> payload;

    /**
     * Cron表达式
     */
    @NotBlank(message = "Cron表达式不能为空")
    private String cronExpression;

    /**
     * 任务描述
     */
    private String desc;
} 
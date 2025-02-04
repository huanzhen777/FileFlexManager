package com.huanzhen.fileflexmanager.infrastructure.persistence.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.huanzhen.fileflexmanager.domain.model.enums.TaskStatus;
import com.huanzhen.fileflexmanager.domain.model.enums.TaskType;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("task")
public class TaskDO {
    @TableId(type = IdType.AUTO)
    private Long id;
    private TaskType type;
    private TaskStatus status;
    private Integer progress;
    private String message;
    private String desc;
    private String payload;
    @TableField("create_time")
    private LocalDateTime createTime;
    @TableField("update_time")
    private LocalDateTime updateTime;
    @TableField("begin_time")
    private LocalDateTime beginTime;
    @TableField("end_time")
    private LocalDateTime endTime;
    private Boolean scheduled;
    @TableField("from_schedule")
    private Boolean fromSchedule;
    @TableField("cron_expression")
    private String cronExpression;
    private Boolean enabled;
    @TableField("next_execute_time")
    private LocalDateTime nextExecuteTime;
    @TableField("execute_count")
    private Integer executeCount;
    @TableField("last_execute_time")
    private LocalDateTime lastExecuteTime;
} 
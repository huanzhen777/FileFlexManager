package com.huanzhen.fileflexmanager.interfaces.model.resp;

import com.alibaba.fastjson2.JSONObject;
import com.huanzhen.fileflexmanager.domain.model.enums.TaskStatus;
import lombok.Data;


@Data
public class TaskResponse {
    private Long id;
    private String type;
    private String typeDesc;
    private TaskStatus status;
    private Integer progress;
    private String message;
    private String desc;
    private Long createTime;
    private Long updateTime;
    private JSONObject payload;
    private Long beginTime;
    private Long endTime;
    private Boolean scheduled;
    private String cronExpression;
    private Boolean enabled;
    private Long nextExecuteTime;
    private Integer executeCount;
    private Long lastExecuteTime;

} 
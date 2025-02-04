package com.huanzhen.fileflexmanager.interfaces.model.req;

import com.alibaba.fastjson2.JSONObject;
import com.huanzhen.fileflexmanager.domain.model.enums.TaskType;
import lombok.Data;

@Data
public class CreateTaskRequest {
    private TaskType type;
    private JSONObject payload;
} 
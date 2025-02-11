package com.huanzhen.fileflexmanager.domain.model.req;

import com.alibaba.fastjson2.JSONObject;

public record UpdateScheduledTaskRequest(
    Long id,
    String type,
    String cronExpression,
    String desc,
    JSONObject payload
) {
}

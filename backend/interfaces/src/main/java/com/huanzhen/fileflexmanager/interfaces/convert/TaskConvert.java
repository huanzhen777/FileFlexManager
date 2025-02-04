package com.huanzhen.fileflexmanager.interfaces.convert;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.huanzhen.fileflexmanager.domain.model.entity.Task;
import com.huanzhen.fileflexmanager.domain.model.enums.TaskType;
import com.huanzhen.fileflexmanager.infrastructure.persistence.entity.TaskDO;
import com.huanzhen.fileflexmanager.interfaces.model.req.CreateTaskRequest;
import com.huanzhen.fileflexmanager.interfaces.model.resp.TaskResponse;
import com.huanzhen.fileflexmanager.interfaces.model.resp.TaskTypeResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

@Mapper(componentModel = "spring")
public interface TaskConvert {
    
    @Mapping(target = "payload", source = "payload", qualifiedByName = "jsonObjectToString")
    TaskDO toTaskDO(Task task);

    @Mapping(target = "payload", source = "payload", qualifiedByName = "stringToJsonObject")
    Task toTask(TaskDO taskDO);

    @Named("jsonObjectToString")
    default String jsonObjectToString(JSONObject jsonObject) {
        return jsonObject != null ? jsonObject.toString() : null;
    }

    @Named("stringToJsonObject")
    default JSONObject stringToJsonObject(String json) {
        return json != null ? JSON.parseObject(json) : null;
    }

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "status", constant = "PENDING")
    @Mapping(target = "progress", constant = "0")
    @Mapping(target = "message", ignore = true)
    @Mapping(target = "createTime", expression = "java(java.time.LocalDateTime.now())")
    @Mapping(target = "updateTime", expression = "java(java.time.LocalDateTime.now())")
    Task toTask(CreateTaskRequest createTaskRequest);

    @Mapping(target = "createTime", expression = "java(toTimestamp(task.getCreateTime()))")
    @Mapping(target = "updateTime", expression = "java(toTimestamp(task.getUpdateTime()))")
    @Mapping(target = "beginTime", expression = "java(toTimestamp(task.getBeginTime()))")
    @Mapping(target = "endTime", expression = "java(toTimestamp(task.getEndTime()))")
    @Mapping(target = "typeDesc", expression = "java(task.getType().getDescription())")
    TaskResponse toTaskResponse(Task task);

    default Long toTimestamp(LocalDateTime dateTime) {
        return dateTime != null ? dateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli() : null;
    }

    default LocalDateTime toDate(Long dateTime) {
        if (dateTime == null) {
            return null;
        }
        return  Instant.ofEpochMilli(dateTime)
                .atZone(ZoneId.systemDefault())
                .toLocalDateTime();
    }

    @Mapping(target = "type", expression = "java(taskType.name())")
    @Mapping(target = "description", source = "description")
    @Mapping(target = "supportScheduled", source = "supportScheduled")
    @Mapping(target = "paramConfigs", source = "paramConfigs")
    TaskTypeResponse toTaskTypeResponse(TaskType taskType);
} 
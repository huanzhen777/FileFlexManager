package com.huanzhen.fileflexmanager.infrastructure.persistence.converter;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.huanzhen.fileflexmanager.domain.model.entity.Task;
import com.huanzhen.fileflexmanager.infrastructure.persistence.entity.TaskDO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

@Mapper(componentModel = "spring", uses = TimeConverter.class)
public interface TaskDOConvert {
    
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
} 
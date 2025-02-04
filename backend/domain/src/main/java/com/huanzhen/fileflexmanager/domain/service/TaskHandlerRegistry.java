package com.huanzhen.fileflexmanager.domain.service;

import cn.hutool.core.lang.Assert;
import com.huanzhen.fileflexmanager.domain.model.enums.TaskType;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@RequiredArgsConstructor
public class TaskHandlerRegistry {
    private final Map<TaskType, Class<? extends TaskHandler>> handlerClass = new ConcurrentHashMap<>();


    public TaskHandler getHandler(TaskType type) {
        Class<? extends TaskHandler> aClass = handlerClass.get(type);
        Assert.notNull("未找到任务处理器");
        TaskHandler handler = applicationContext.getBean(aClass);
        Assert.notNull("未找到任务处理器");
        return handler;
    }

    private final List<TaskHandler> handlers;
    private final ApplicationContext applicationContext;


    @PostConstruct
    public void init() {
        handlers.forEach(handler ->
                handlerClass.put(handler.getTaskType(), handler.getClass())
        );
    }
} 
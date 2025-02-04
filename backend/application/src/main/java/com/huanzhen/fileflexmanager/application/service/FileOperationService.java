package com.huanzhen.fileflexmanager.application.service;

import cn.hutool.core.lang.Assert;
import com.alibaba.fastjson2.JSONObject;
import com.huanzhen.fileflexmanager.domain.model.entity.Task;
import com.huanzhen.fileflexmanager.domain.model.enums.FileOperationType;
import com.huanzhen.fileflexmanager.domain.service.FileOperationHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class FileOperationService {
    private final Map<FileOperationType, FileOperationHandler> handlers;
    private final TaskApplicationService taskApplicationService;
    private final FileService fileService;

    public FileOperationService(List<FileOperationHandler> operationHandlers,
                                TaskApplicationService taskApplicationService,
                                FileService fileService) {
        this.handlers = new ConcurrentHashMap<>();
        this.taskApplicationService = taskApplicationService;
        this.fileService = fileService;
        operationHandlers.forEach(handler -> handlers.put(handler.getOperationType(), handler));
    }

    /**
     * 执行文件操作
     * @param operationType 操作类型
     * @param params 操作参数
     * @return 如果是任务类型，返回任务ID；否则返回操作结果
     */
    public String executeOperation(FileOperationType operationType, JSONObject params) {
        if (operationType.isTask()) {
            if (operationType.getIsSync()) {
                Task task = taskApplicationService.executeTask(operationType.getTaskType(), params);
                return operationType.getDescription() + "执行成功";
            } else {
                Task task = taskApplicationService.submitAsyncTask(operationType.getTaskType(), params);
                return operationType.getDescription() + "已提交到任务中心，请前往任务中心查看";
            }
        }

        // 如果不是任务类型，直接执行操作
        FileOperationHandler handler = handlers.get(operationType);
        Assert.notNull(handler, "不支持的操作类型: " + operationType);

        try {
            boolean result = handler.handle(params);
            Assert.isTrue(result, "操作执行失败");
            return operationType.getDescription() + "执行成功";
        } catch (Exception e) {
            log.error("文件操作执行失败: {}", operationType, e);
            throw new RuntimeException("操作执行失败: " + e.getMessage(), e);
        }
    }
} 
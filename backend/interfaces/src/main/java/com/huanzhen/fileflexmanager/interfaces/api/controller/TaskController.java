package com.huanzhen.fileflexmanager.interfaces.api.controller;

import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.huanzhen.fileflexmanager.application.service.TaskApplicationService;
import com.huanzhen.fileflexmanager.domain.model.entity.Task;
import com.huanzhen.fileflexmanager.domain.model.enums.TaskType;
import com.huanzhen.fileflexmanager.interfaces.convert.TaskConvert;
import com.huanzhen.fileflexmanager.domain.model.BaseResponse;
import com.huanzhen.fileflexmanager.interfaces.model.req.CreateScheduledTaskRequest;
import com.huanzhen.fileflexmanager.interfaces.model.resp.TaskResponse;
import com.huanzhen.fileflexmanager.interfaces.model.resp.TaskTypeResponse;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/tasks")
public class TaskController {
    private final TaskApplicationService taskService;
    private final TaskConvert taskConvert;

    public TaskController(TaskApplicationService taskService, TaskConvert taskConvert) {
        this.taskService = taskService;
        this.taskConvert = taskConvert;
    }


    @GetMapping("/{id}")
    public BaseResponse<TaskResponse> getTaskProgress(@PathVariable Long id) {
        return BaseResponse.success(taskConvert.toTaskResponse(
                taskService.getTaskProgress(id)
        ));
    }

    @PostMapping("/{id}/cancel")
    public BaseResponse<TaskResponse> cancelTask(@PathVariable Long id) {
        taskService.cancelTask(id);
        return BaseResponse.success(taskConvert.toTaskResponse(
                taskService.getTaskProgress(id)
        ));
    }


    @GetMapping("/running")
    public BaseResponse<List<TaskResponse>> getRunningTasks() {
        List<TaskResponse> tasks = taskService.getRunningTasks().stream()
                .map(taskConvert::toTaskResponse)
                .collect(Collectors.toList());
        return BaseResponse.success(tasks);
    }

    @GetMapping
    public BaseResponse<Page<TaskResponse>> getAllTasks(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Boolean includeCompleted) {
        Page<Task> taskPage = taskService.getAllTasks(page, size, includeCompleted);

        Page<TaskResponse> responsePage = new Page<>(taskPage.getCurrent(), taskPage.getSize(), taskPage.getTotal());
        responsePage.setRecords(taskPage.getRecords().stream()
                .map(taskConvert::toTaskResponse)
                .collect(Collectors.toList()));

        return BaseResponse.success(responsePage);
    }

    @GetMapping("/getScheduledTasks")
    public BaseResponse<Page<TaskResponse>> getScheduledTasks(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        Page<Task> taskPage = taskService.getScheduledTasks(page, size);

        Page<TaskResponse> responsePage = new Page<>(taskPage.getCurrent(), taskPage.getSize(), taskPage.getTotal());
        responsePage.setRecords(taskPage.getRecords().stream()
                .map(taskConvert::toTaskResponse)
                .collect(Collectors.toList()));
        return BaseResponse.success(responsePage);
    }


    @PostMapping("/scheduled")
    public BaseResponse<TaskResponse> createScheduledTask(
            @RequestBody CreateScheduledTaskRequest request) {
        Task task = taskService.createScheduledTask(
                request.getType(),
                new JSONObject(request.getPayload()),
                request.getCronExpression()
        );
        return BaseResponse.success(taskConvert.toTaskResponse(task));
    }

    @PostMapping("/scheduled/{id}/enable")
    public BaseResponse<TaskResponse> enableScheduledTask(@PathVariable Long id) {
        Task task = taskService.enableScheduledTask(id);
        return BaseResponse.success(taskConvert.toTaskResponse(task));
    }

    @PostMapping("/scheduled/{id}/disable")
    public BaseResponse<TaskResponse> disableScheduledTask(@PathVariable Long id) {
        Task task = taskService.disableScheduledTask(id);
        return BaseResponse.success(taskConvert.toTaskResponse(task));
    }

    @DeleteMapping("/scheduled/{id}")
    public BaseResponse<Void> deleteScheduledTask(@PathVariable Long id) {
        taskService.deleteScheduledTask(id);
        return BaseResponse.success(null);
    }

    @GetMapping("/scheduled/types")
    public BaseResponse<List<TaskTypeResponse>> getScheduledTaskTypes() {
        return BaseResponse.success(
                Arrays.stream(TaskType.values())
                        .filter(TaskType::isSupportScheduled)
                        .map(taskConvert::toTaskTypeResponse)
                        .collect(Collectors.toList())
        );
    }
} 
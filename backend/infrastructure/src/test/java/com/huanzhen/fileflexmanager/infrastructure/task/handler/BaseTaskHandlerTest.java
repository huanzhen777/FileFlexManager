package com.huanzhen.fileflexmanager.infrastructure.task.handler;

import com.alibaba.fastjson2.JSONObject;
import com.huanzhen.fileflexmanager.domain.model.entity.Task;
import com.huanzhen.fileflexmanager.domain.model.enums.TaskStatus;
import com.huanzhen.fileflexmanager.domain.model.enums.TaskType;
import com.huanzhen.fileflexmanager.domain.repository.TaskRepository;
import com.huanzhen.fileflexmanager.domain.service.TaskHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public abstract class BaseTaskHandlerTest<T> {

    @Mock
    protected TaskRepository taskRepository;

    protected TaskHandler<T> taskHandler;

    protected Path testRootPath;

    @BeforeEach
    void setUp() throws IOException {
        testRootPath = Paths.get(System.getProperty("java.io.tmpdir"), "mynasctrl_test");
        setupTestHandler();
    }

    protected abstract void setupTestHandler();

    protected Task createTestTask(JSONObject payload) {
        Task task = new Task(getTaskType(), payload);
        task.setId(1L);
        task.begin();
        return task;
    }

    protected abstract TaskType getTaskType();

    protected void verifyTaskSuccess(Task task) {
        ArgumentCaptor<Task> taskCaptor = ArgumentCaptor.forClass(Task.class);
        verify(taskRepository, atLeastOnce()).updateTask(taskCaptor.capture());
        Task updatedTask = taskCaptor.getValue();
        assertEquals(TaskStatus.COMPLETED, updatedTask.getStatus(), "任务状态应该是已完成");
        assertEquals(100, updatedTask.getProgress(), "任务进度应该是100%");
    }

    protected void verifyTaskFailure(Task task, String expectedErrorMessage) {
        ArgumentCaptor<Task> taskCaptor = ArgumentCaptor.forClass(Task.class);
        verify(taskRepository, atLeastOnce()).updateTask(taskCaptor.capture());
        Task updatedTask = taskCaptor.getValue();
        assertEquals(TaskStatus.FAILED, updatedTask.getStatus(), "任务状态应该是失败");
        assertNotNull(updatedTask.getMessage(), "错误消息不应为空");
    }

    protected void verifyTaskCancelled(Task task) {
        ArgumentCaptor<Task> taskCaptor = ArgumentCaptor.forClass(Task.class);
        verify(taskRepository, atLeastOnce()).updateTask(taskCaptor.capture());
        Task updatedTask = taskCaptor.getValue();
        assertEquals(TaskStatus.CANCELLED, updatedTask.getStatus(), "任务状态应该是已取消");
    }

    protected void verifyProgressUpdates() {
        verify(taskRepository, atLeast(1)).updateTask(argThat(task -> 
            task.getProgress() >= 0 && task.getProgress() <= 100 &&
            task.getStatus() != null
        ));
    }

    protected void setupCancellationTest(Task task) {
        // 设置任务状态为运行中
        task.begin();
        // 模拟取消操作
        doAnswer(invocation -> {
            Task t = invocation.getArgument(0);
            if (t.getStatus() == TaskStatus.RUNNING) {
                t.markAsCancelled("任务已取消");
            }
            return null;
        }).when(taskRepository).updateTask(any());
    }
} 
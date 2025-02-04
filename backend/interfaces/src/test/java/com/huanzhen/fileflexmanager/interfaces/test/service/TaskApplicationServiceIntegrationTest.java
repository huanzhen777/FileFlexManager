package com.huanzhen.fileflexmanager.interfaces.test.service;

import com.alibaba.fastjson2.JSONObject;
import com.huanzhen.fileflexmanager.application.service.TaskApplicationService;
import com.huanzhen.fileflexmanager.domain.model.entity.Task;
import com.huanzhen.fileflexmanager.domain.model.enums.TaskStatus;
import com.huanzhen.fileflexmanager.domain.model.enums.TaskType;
import com.huanzhen.fileflexmanager.domain.repository.TaskRepository;
import com.huanzhen.fileflexmanager.domain.service.TaskHandler;
import com.huanzhen.fileflexmanager.domain.service.TaskHandlerRegistry;
import com.huanzhen.fileflexmanager.interfaces.test.config.TestConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@SpringBootTest
@ActiveProfiles("test")
@ContextConfiguration(classes = {TestConfig.class})
class TaskApplicationServiceIntegrationTest {

    @Autowired
    private TaskApplicationService taskApplicationService;

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private TaskHandlerRegistry taskHandlerRegistry;

    private static final TaskType TEST_TASK_TYPE = TaskType.TEST;

    @BeforeEach
    void setUp() {
        reset(taskRepository);
    }

    @Test
    void testPrototypeScopeTaskHandler() {
        // 验证每次获取的TaskHandler都是新实例
        TaskHandler handler1 = taskHandlerRegistry.getHandler(TEST_TASK_TYPE);
        TaskHandler handler2 = taskHandlerRegistry.getHandler(TEST_TASK_TYPE);

        assertNotNull(handler1);
        assertNotNull(handler2);
        assertNotSame(handler1, handler2);
    }

    @Test
    void testConcurrentTaskExecution() throws InterruptedException {
        // 准备测试数据
        Task task1 = new Task(TEST_TASK_TYPE, new JSONObject());
        Task task2 = new Task(TEST_TASK_TYPE, new JSONObject());
        task1.setId(1L);
        task2.setId(2L);

        // 记录每个任务的状态变化
        List<TaskStatus> task1States = new ArrayList<>();
        List<TaskStatus> task2States = new ArrayList<>();

        when(taskRepository.findById(1L)).thenReturn(task1);
        when(taskRepository.findById(2L)).thenReturn(task2);
        when(taskRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(taskRepository.updateTask(any())).thenAnswer(invocation -> {
            Task task = invocation.getArgument(0);
            if (task.getId().equals(1L)) {
                task1States.add(task.getStatus());
            } else if (task.getId().equals(2L)) {
                task2States.add(task.getStatus());
            }
            return 1;
        });

        // 执行两个并发任务
        Thread thread1 = new Thread(() -> taskApplicationService.executeTask(task1.getId()));
        Thread thread2 = new Thread(() -> taskApplicationService.executeTask(task2.getId()));

        thread1.start();
        thread2.start();

        // 等待任务完成
        thread1.join(2000);
        thread2.join(2000);

        // 验证任务1的状态变化
        assertTrue(task1States.contains(TaskStatus.RUNNING), "Task 1 should have been RUNNING");
        assertTrue(task1States.contains(TaskStatus.COMPLETED), "Task 1 should have been COMPLETED");
        assertEquals(TaskStatus.COMPLETED, task1States.get(task1States.size() - 1), "Task 1 should end with COMPLETED");

        // 验证任务2的状态变化
        assertTrue(task2States.contains(TaskStatus.RUNNING), "Task 2 should have been RUNNING");
        assertTrue(task2States.contains(TaskStatus.COMPLETED), "Task 2 should have been COMPLETED");
        assertEquals(TaskStatus.COMPLETED, task2States.get(task2States.size() - 1), "Task 2 should end with COMPLETED");
    }

    @Test
    void testTaskCancellation() throws InterruptedException {
        // 准备测试数据
        Task task = new Task(TEST_TASK_TYPE, new JSONObject());
        task.setId(1L);

        // 记录任务的状态变化
        List<TaskStatus> taskStates = new ArrayList<>();

        when(taskRepository.findById(1L)).thenReturn(task);
        when(taskRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(taskRepository.updateTask(any())).thenAnswer(invocation -> {
            Task t = invocation.getArgument(0);
            taskStates.add(t.getStatus());
            return 1;
        });

        // 启动任务
        Thread taskThread = new Thread(() -> {
            try {
                taskApplicationService.executeTask(task.getId());
            } catch (Exception e) {
                // 忽略预期中的取消异常
            }
        });
        taskThread.start();

        // 等待任务开始执行
        Thread.sleep(100);

        // 取消任务
        taskApplicationService.cancelTask(task.getId());

        // 等待任务结束
        taskThread.join(2000);
        assertFalse(taskThread.isAlive(), "Task thread should end after cancellation");

        // 验证任务状态变化
        assertTrue(taskStates.contains(TaskStatus.RUNNING), "Task should have been RUNNING");
        assertTrue(taskStates.contains(TaskStatus.CANCELLED), "Task should have been CANCELLED");
        assertEquals(TaskStatus.CANCELLED, taskStates.get(taskStates.size() - 1), "Task should end with CANCELLED");
    }

    @Test
    void testMultipleTasksCancellation() throws InterruptedException {
        // 准备测试数据
        Task task1 = new Task(TEST_TASK_TYPE, new JSONObject());
        task1.setId(1L);
        Task task2 = new Task(TEST_TASK_TYPE, new JSONObject());
        task2.setId(2L);

        // 记录任务的状态变化
        Map<Long, List<TaskStatus>> taskStatesMap = new HashMap<>();
        taskStatesMap.put(task1.getId(), new ArrayList<>());
        taskStatesMap.put(task2.getId(), new ArrayList<>());

        when(taskRepository.findById(anyLong())).thenAnswer(invocation -> {
            Long id = invocation.getArgument(0);
            return id.equals(task1.getId()) ? task1 : task2;
        });
        when(taskRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(taskRepository.updateTask(any())).thenAnswer(invocation -> {
            Task t = invocation.getArgument(0);
            taskStatesMap.get(t.getId()).add(t.getStatus());
            return 1;
        });

        // 启动任务
        Thread taskThread1 = new Thread(() -> {
            try {
                taskApplicationService.executeTask(task1.getId());
            } catch (Exception e) {
                // 忽略预期中的取消异常
            }
        });
        Thread taskThread2 = new Thread(() -> {
            try {
                taskApplicationService.executeTask(task2.getId());
            } catch (Exception e) {
                // 忽略预期中的取消异常
            }
        });
        taskThread1.start();
        taskThread2.start();

        // 等待任务开始执行
        Thread.sleep(100);

        // 取消任务1
        taskApplicationService.cancelTask(task1.getId());

        // 等待任务结束
        taskThread1.join(2000);
        taskThread2.join(2000);

        assertFalse(taskThread1.isAlive(), "Task 1 thread should end after cancellation");
        assertFalse(taskThread2.isAlive(), "Task 2 thread should end");

        // 验证任务状态变化
        List<TaskStatus> task1States = taskStatesMap.get(task1.getId());
        List<TaskStatus> task2States = taskStatesMap.get(task2.getId());

        assertTrue(task1States.contains(TaskStatus.RUNNING), "Task 1 should have been RUNNING");
        assertTrue(task1States.contains(TaskStatus.CANCELLED), "Task 1 should have been CANCELLED");
        assertEquals(TaskStatus.CANCELLED, task1States.get(task1States.size() - 1), "Task 1 should end with CANCELLED");

        assertTrue(task2States.contains(TaskStatus.RUNNING), "Task 2 should have been RUNNING");
        assertNotEquals(TaskStatus.CANCELLED, task2States.get(task2States.size() - 1), "Task 2 should not end with CANCELLED");
    }
} 
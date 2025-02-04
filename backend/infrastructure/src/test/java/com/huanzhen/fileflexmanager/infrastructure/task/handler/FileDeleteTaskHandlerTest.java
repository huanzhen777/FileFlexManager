package com.huanzhen.fileflexmanager.infrastructure.task.handler;

import com.alibaba.fastjson2.JSONObject;
import com.huanzhen.fileflexmanager.domain.model.entity.Task;
import com.huanzhen.fileflexmanager.domain.model.enums.TaskType;
import com.huanzhen.fileflexmanager.domain.model.params.params.FileDeleteParams;
import com.huanzhen.fileflexmanager.domain.repository.FileIndexRepository;
import com.huanzhen.fileflexmanager.domain.repository.FileTagRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class FileDeleteTaskHandlerTest extends BaseTaskHandlerTest<FileDeleteParams> {

    @TempDir
    Path tempDir;
    
    Path sourceFile;
    Path sourceFile2;
    Path sourceDir;
    Path nestedDir;

    @Mock
    private FileIndexRepository fileIndexRepository;
    @Mock
    private FileTagRepository fileTagRepository;

    @BeforeEach
    void setUp() throws IOException {
        MockitoAnnotations.openMocks(this);
        super.setUp();
        
        // 创建源文件
        sourceFile = tempDir.resolve("source.txt");
        Files.write(sourceFile, "test content".getBytes());
        
        // 创建第二个源文件
        sourceFile2 = tempDir.resolve("source2.txt");
        Files.write(sourceFile2, "test content 2".getBytes());
        
        // 创建源目录及其内容
        sourceDir = tempDir.resolve("sourceDir");
        Files.createDirectories(sourceDir);
        Files.write(sourceDir.resolve("test.txt"), "test content".getBytes());
        Files.write(sourceDir.resolve("test2.txt"), "test content 2".getBytes());

        // 创建嵌套目录结构用于测试进度更新
        nestedDir = tempDir.resolve("nestedDir");
        Files.createDirectories(nestedDir);
        Files.createDirectories(nestedDir.resolve("subdir1"));
        Files.createDirectories(nestedDir.resolve("subdir2"));
        Files.write(nestedDir.resolve("file1.txt"), "content".getBytes());
        Files.write(nestedDir.resolve("subdir1/file2.txt"), "content".getBytes());
        Files.write(nestedDir.resolve("subdir2/file3.txt"), "content".getBytes());
    }

    @Override
    protected void setupTestHandler() {
        taskHandler = new FileDeleteTaskHandler(taskRepository, fileIndexRepository, fileTagRepository);
    }

    @Override
    protected TaskType getTaskType() {
        return TaskType.FILE_DELETE;
    }

    @Test
    void testDeleteSingleFile_Success() throws IOException {
        // 准备测试数据
        JSONObject payload = new JSONObject();
        payload.put("selectedPaths", Arrays.asList(sourceFile.toString()));
        Task task = createTestTask(payload);

        // 执行测试
        taskHandler.handle(task);

        // 验证文件索引被删除
        verify(fileIndexRepository).deleteByPath(sourceFile.toString());
        
        // 验证文件确实被删除了
        assertFalse(Files.exists(sourceFile));

        // 验证进度更新
        verify(taskRepository, atLeastOnce()).updateTask(any());
        assertTrue(task.getProgress() == 100);
    }

    @Test
    void testDeleteMultipleFiles_Success() throws IOException {
        // 准备测试数据
        JSONObject payload = new JSONObject();
        payload.put("selectedPaths", Arrays.asList(
                sourceFile.toString(),
                sourceFile2.toString()
        ));
        Task task = createTestTask(payload);

        // 执行测试
        taskHandler.handle(task);

        // 验证文件索引被删除
        verify(fileIndexRepository).deleteByPath(sourceFile.toString());
        verify(fileIndexRepository).deleteByPath(sourceFile2.toString());
        
        // 验证文件确实被删除了
        assertFalse(Files.exists(sourceFile));
        assertFalse(Files.exists(sourceFile2));

        // 验证进度更新
        verify(taskRepository, atLeastOnce()).updateTask(any());
        assertTrue(task.getProgress() == 100);
    }

    @Test
    void testDeleteDirectory_Success() throws IOException {
        // 准备测试数据
        JSONObject payload = new JSONObject();
        payload.put("selectedPaths", Arrays.asList(sourceDir.toString()));
        Task task = createTestTask(payload);

        // 执行测试
        taskHandler.handle(task);

        // 验证目录索引被删除
        verify(fileIndexRepository).deleteStaleIndexes(eq(sourceDir.toString()), any(LocalDateTime.class));
        
        // 验证目录及其内容确实被删除了
        assertFalse(Files.exists(sourceDir));
        assertFalse(Files.exists(sourceDir.resolve("test.txt")));
        assertFalse(Files.exists(sourceDir.resolve("test2.txt")));

        // 验证进度更新
        verify(taskRepository, atLeastOnce()).updateTask(any());
        assertTrue(task.getProgress() == 100);
    }

    @Test
    void testDeleteNestedDirectory_WithProgressUpdates() throws IOException {
        // 准备测试数据
        JSONObject payload = new JSONObject();
        payload.put("selectedPaths", Arrays.asList(nestedDir.toString()));
        Task task = createTestTask(payload);

        // 执行测试
        taskHandler.handle(task);

        // 验证目录及其内容确实被删除了
        assertFalse(Files.exists(nestedDir));
        assertFalse(Files.exists(nestedDir.resolve("subdir1")));
        assertFalse(Files.exists(nestedDir.resolve("subdir2")));
        assertFalse(Files.exists(nestedDir.resolve("file1.txt")));
        assertFalse(Files.exists(nestedDir.resolve("subdir1/file2.txt")));
        assertFalse(Files.exists(nestedDir.resolve("subdir2/file3.txt")));

        // 验证进度更新
        verify(taskRepository, atLeast(3)).updateTask(any());
        assertTrue(task.getProgress() == 100);
    }

    @Test
    void testDeleteMultipleItems_MixedTypes() throws IOException {
        // 准备测试数据
        JSONObject payload = new JSONObject();
        payload.put("selectedPaths", Arrays.asList(
                sourceFile.toString(),
                sourceDir.toString()
        ));
        Task task = createTestTask(payload);

        // 执行测试
        taskHandler.handle(task);

        // 验证文件索引被删除
        verify(fileIndexRepository).deleteByPath(sourceFile.toString());
        verify(fileIndexRepository).deleteStaleIndexes(eq(sourceDir.toString()), any(LocalDateTime.class));
        
        // 验证所有项目确实被删除了
        assertFalse(Files.exists(sourceFile));
        assertFalse(Files.exists(sourceDir));
        assertFalse(Files.exists(sourceDir.resolve("test.txt")));
        assertFalse(Files.exists(sourceDir.resolve("test2.txt")));

        // 验证进度更新
        verify(taskRepository, atLeastOnce()).updateTask(any());
        assertTrue(task.getProgress() == 100);
    }



    @Test
    void testDeleteNonExistentFile() throws IOException {
        // 准备测试数据
        String nonExistentFile = tempDir.resolve("non_existent.txt").toString();
        JSONObject payload = new JSONObject();
        payload.put("selectedPaths", Arrays.asList(nonExistentFile));
        Task task = createTestTask(payload);

        // 执行测试
        taskHandler.handle(task);

        // 验证任务失败
        verifyTaskFailure(task, "路径不存在：" + nonExistentFile);
    }

    @Test
    void testDeleteMultipleFiles_PartialFailure() throws IOException {
        // 创建一个不存在的文件路径
        String nonExistentFile = tempDir.resolve("non_existent.txt").toString();

        // 准备测试数据
        JSONObject payload = new JSONObject();
        payload.put("selectedPaths", Arrays.asList(
                sourceFile.toString(),
                nonExistentFile
        ));
        Task task = createTestTask(payload);

        // 执行测试
        taskHandler.handle(task);

        // 验证任务失败
        verifyTaskFailure(task, "路径不存在：" + nonExistentFile);
        
        // 验证已存在的文件仍然存在
        assertTrue(Files.exists(sourceFile));
    }

    @Test
    void testDeleteMultipleFiles_EmptySelection() {
        // 准备测试数据
        JSONObject payload = new JSONObject();
        payload.put("selectedPaths", Arrays.asList());
        Task task = createTestTask(payload);

        // 执行测试
        taskHandler.handle(task);

        // 验证任务失败
        verifyTaskFailure(task, "选择文件/文件夹不能为空");
    }

    @Test
    void testTaskDescription() {
        // 测试单文件描述
        JSONObject singlePayload = new JSONObject();
        singlePayload.put("selectedPaths", Arrays.asList(sourceFile.toString()));
        Task singleTask = createTestTask(singlePayload);
        String singleDesc = taskHandler.getTaskDesc(singleTask);
        assertTrue(singleDesc.contains("批量删除: 1 个文件/目录"));

        // 测试多文件描述
        JSONObject multiPayload = new JSONObject();
        multiPayload.put("selectedPaths", Arrays.asList(sourceFile.toString(), sourceFile2.toString()));
        Task multiTask = createTestTask(multiPayload);
        String multiDesc = taskHandler.getTaskDesc(multiTask);
        assertTrue(multiDesc.contains("批量删除: 2 个文件/目录"));
    }
} 
package com.huanzhen.fileflexmanager.infrastructure.task.handler;

import com.alibaba.fastjson2.JSONObject;
import com.huanzhen.fileflexmanager.domain.model.entity.FileIndex;
import com.huanzhen.fileflexmanager.domain.model.entity.Task;
import com.huanzhen.fileflexmanager.domain.model.enums.TaskType;
import com.huanzhen.fileflexmanager.domain.model.params.params.FileIndexParams;
import com.huanzhen.fileflexmanager.domain.repository.FileIndexRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FileIndexTaskHandlerTest extends BaseTaskHandlerTest<FileIndexParams> {

    @Mock
    private FileIndexRepository fileIndexRepository;

    private Path testDir;
    private Path testFile1;
    private Path testFile2;
    private Path testSubDir;

    @Override
    protected void setupTestHandler() {
        taskHandler = new FileIndexTaskHandler(fileIndexRepository, taskRepository);
    }

    @BeforeEach
    void setUpTestFiles() throws IOException {
        // 创建测试目录结构
        testDir = Files.createDirectories(testRootPath.resolve("test_index"));
        testFile1 = Files.createFile(testDir.resolve("test1.txt"));
        testFile2 = Files.createFile(testDir.resolve("test2.txt"));
        testSubDir = Files.createDirectories(testDir.resolve("subdir"));
        Files.createFile(testSubDir.resolve("test3.txt"));

        // 写入一些测试数据
        Files.write(testFile1, Arrays.asList("test content 1"));
        Files.write(testFile2, Arrays.asList("test content 2"));
    }

    @AfterEach
    void cleanUp() throws IOException {
        // 清理测试文件
        Files.walk(testRootPath)
            .sorted((a, b) -> -a.compareTo(b))
            .forEach(path -> {
                try {
                    Files.deleteIfExists(path);
                } catch (IOException e) {
                    // 忽略清理错误
                }
            });
    }

    @Override
    protected TaskType getTaskType() {
        return TaskType.FILE_INDEX;
    }

    @Test
    void testIndexSingleFile() {
        // 准备测试数据
        JSONObject payload = new JSONObject();
        payload.put("selectPath", testFile1.toString());
        payload.put("calculateMd5", true);
        Task task = createTestTask(payload);

        // 执行测试
        taskHandler.handle(task);

        // 验证结果
        verify(fileIndexRepository, times(1)).save(argThat(fileIndex -> 
            fileIndex.getPath().equals(testFile1.toString()) &&
            !fileIndex.getIsDir() &&
            fileIndex.getSize() > 0 &&
            fileIndex.getMd5() != null &&
            !fileIndex.getMd5().isEmpty()
        ));
        verifyTaskSuccess(task);
        verifyProgressUpdates();
    }

    @Test
    void testIndexDirectory() {
        // 准备测试数据
        JSONObject payload = new JSONObject();
        payload.put("selectPath", testDir.toString());
        payload.put("calculateMd5", false);
        payload.put("maxDepth", 2);
        Task task = createTestTask(payload);

        // 执行测试
        taskHandler.handle(task);

        // 验证结果
        // 应该索引4个项目：testDir, testFile1, testFile2, testSubDir
        verify(fileIndexRepository, atLeast(4)).save(any(FileIndex.class));
        verifyTaskSuccess(task);
        verifyProgressUpdates();
    }

    @Test
    void testIndexNonExistentPath() {
        // 准备测试数据
        JSONObject payload = new JSONObject();
        payload.put("selectPath", testRootPath.resolve("non_existent").toString());
        Task task = createTestTask(payload);

        // 执行测试
        taskHandler.handle(task);

        // 验证结果
        verify(fileIndexRepository, never()).save(any(FileIndex.class));
        verifyTaskFailure(task, "目录不存在");
    }


    @Test
    void testUpdateExistingFileIndex() {
        // 准备测试数据
        JSONObject payload = new JSONObject();
        payload.put("selectPath", testFile1.toString());
        payload.put("calculateMd5", true);
        Task task = createTestTask(payload);

        // 模拟已存在的FileIndex
        FileIndex existingIndex = new FileIndex();
        existingIndex.setId(1L);
        existingIndex.setPath(testFile1.toString());
        when(fileIndexRepository.findByPath(testFile1.toString())).thenReturn(existingIndex);

        // 执行测试
        taskHandler.handle(task);

        // 验证结果
        verify(fileIndexRepository, times(1)).updateFileIndex(argThat(fileIndex -> 
            fileIndex.getId().equals(1L) &&
            fileIndex.getPath().equals(testFile1.toString())
        ));
        verifyTaskSuccess(task);
    }
} 
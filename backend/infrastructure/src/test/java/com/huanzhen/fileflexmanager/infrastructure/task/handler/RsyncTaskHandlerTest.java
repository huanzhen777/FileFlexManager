package com.huanzhen.fileflexmanager.infrastructure.task.handler;

import com.alibaba.fastjson2.JSONObject;
import com.huanzhen.fileflexmanager.domain.model.entity.Task;
import com.huanzhen.fileflexmanager.domain.model.enums.TaskType;
import com.huanzhen.fileflexmanager.domain.model.params.params.FileCopyParams;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RsyncTaskHandlerTest extends BaseTaskHandlerTest<FileCopyParams> {

    @TempDir
    Path tempDir;
    
    Path sourceFile;
    Path sourceFile2;
    Path sourceDir;
    Path targetDir;

    @BeforeEach
    void setUp() throws IOException {
        super.setUp();
        // 创建测试文件和目录
        sourceFile = tempDir.resolve("test1.txt");
        sourceFile2 = tempDir.resolve("test2.txt");
        sourceDir = tempDir.resolve("source_dir");
        targetDir = tempDir.resolve("target_dir");

        // 创建并写入测试文件
        Files.write(sourceFile, "test content 1".getBytes());
        Files.write(sourceFile2, "test content 2".getBytes());
        
        // 创建源目录和目标目录
        Files.createDirectories(sourceDir);
        Files.createDirectories(targetDir);
        
        // 在源目录中创建一些测试文件
        Files.write(sourceDir.resolve("file1.txt"), "content 1".getBytes());
        Files.write(sourceDir.resolve("file2.txt"), "content 2".getBytes());
        Files.createDirectories(sourceDir.resolve("subdir"));
        Files.write(sourceDir.resolve("subdir/file3.txt"), "content 3".getBytes());
    }

    @Override
    protected void setupTestHandler() {
        taskHandler = new RsyncTaskHandler(taskRepository);
    }

    @Override
    protected TaskType getTaskType() {
        return TaskType.FILE_COPY;
    }

    @Test
    void testCopySingleFile_Success() throws IOException {
        // 准备测试数据
        JSONObject payload = new JSONObject();
        payload.put("selectedPaths", List.of(sourceFile.toString()));
        payload.put("targetDir", targetDir.toString());
        Task task = createTestTask(payload);

        // 执行测试
        taskHandler.handle(task);

        // 验证结果
        Path targetFile = targetDir.resolve(sourceFile.getFileName());
        assertTrue(Files.exists(targetFile), "目标文件应该存在");
        assertEquals(
            new String(Files.readAllBytes(sourceFile)),
            new String(Files.readAllBytes(targetFile)),
            "文件内容应该相同"
        );
        assertTrue(Files.exists(sourceFile), "源文件应该仍然存在");
        
        verifyTaskSuccess(task);
        verifyProgressUpdates();
    }

    @Test
    void testCopyMultipleFiles_Success() throws IOException {
        // 准备测试数据
        JSONObject payload = new JSONObject();
        payload.put("selectedPaths", Arrays.asList(
            sourceFile.toString(),
            sourceFile2.toString()
        ));
        payload.put("targetDir", targetDir.toString());
        Task task = createTestTask(payload);

        // 执行测试
        taskHandler.handle(task);

        // 验证结果
        for (Path sourcePath : Arrays.asList(sourceFile, sourceFile2)) {
            Path targetFile = targetDir.resolve(sourcePath.getFileName());
            assertTrue(Files.exists(targetFile), "目标文件应该存在: " + targetFile);
            assertEquals(
                new String(Files.readAllBytes(sourcePath)),
                new String(Files.readAllBytes(targetFile)),
                "文件内容应该相同: " + targetFile
            );
            assertTrue(Files.exists(sourcePath), "源文件应该仍然存在: " + sourcePath);
        }
        
        verifyTaskSuccess(task);
        verifyProgressUpdates();
    }

    @Test
    void testCopyDirectory_Success() throws IOException {
        // 准备测试数据
        JSONObject payload = new JSONObject();
        payload.put("selectedPaths", List.of(sourceDir.toString()));
        payload.put("targetDir", targetDir.toString());
        Task task = createTestTask(payload);

        // 执行测试
        taskHandler.handle(task);

        // 验证结果
        Path targetDirPath = targetDir.resolve(sourceDir.getFileName());
        assertTrue(Files.exists(targetDirPath), "目标目录应该存在");
        assertTrue(Files.exists(targetDirPath.resolve("file1.txt")), "file1.txt 应该存在");
        assertTrue(Files.exists(targetDirPath.resolve("file2.txt")), "file2.txt 应该存在");
        assertTrue(Files.exists(targetDirPath.resolve("subdir/file3.txt")), "subdir/file3.txt 应该存在");
        
        // 验证文件内容
        assertEquals(
            new String(Files.readAllBytes(sourceDir.resolve("file1.txt"))),
            new String(Files.readAllBytes(targetDirPath.resolve("file1.txt"))),
            "file1.txt 内容应该相同"
        );
        
        verifyTaskSuccess(task);
        verifyProgressUpdates();
    }

    @Test
    void testCopyToNonExistentDirectory() {
        // 准备测试数据
        JSONObject payload = new JSONObject();
        payload.put("selectedPaths", List.of(sourceFile.toString()));
        payload.put("targetDir", tempDir.resolve("non_existent_dir").toString());
        Task task = createTestTask(payload);

        // 执行测试
        taskHandler.handle(task);

        // 验证任务失败
        verifyTaskFailure(task, "目标目录不存在：" + tempDir.resolve("non_existent_dir"));
        assertTrue(Files.exists(sourceFile), "源文件应该仍然存在");
    }

    @Test
    void testCopyNonExistentFile() {
        // 准备测试数据
        String nonExistentFile = tempDir.resolve("non_existent.txt").toString();
        JSONObject payload = new JSONObject();
        payload.put("selectedPaths", List.of(nonExistentFile));
        payload.put("targetDir", targetDir.toString());
        Task task = createTestTask(payload);

        // 执行测试
        taskHandler.handle(task);

        // 验证任务失败
        verifyTaskFailure(task, "源文件不存在：" + nonExistentFile);
    }

    @Test
    void testCopyToExistingFile() throws IOException {
        // 在目标目录创建同名文件
        Files.write(targetDir.resolve(sourceFile.getFileName()), "existing content".getBytes());
        
        // 准备测试数据
        JSONObject payload = new JSONObject();
        payload.put("selectedPaths", List.of(sourceFile.toString()));
        payload.put("targetDir", targetDir.toString());
        Task task = createTestTask(payload);

        // 执行测试
        taskHandler.handle(task);

        // 验证任务失败
        verifyTaskFailure(task, "目标文件已存在：" + targetDir.resolve(sourceFile.getFileName()));
        assertEquals(
            "existing content",
            new String(Files.readAllBytes(targetDir.resolve(sourceFile.getFileName()))),
            "目标文件内容不应改变"
        );
    }

    @Test
    void testTaskDescription() {
        // 测试单文件描述
        JSONObject singlePayload = new JSONObject();
        singlePayload.put("selectedPaths", List.of(sourceFile.toString()));
        singlePayload.put("targetDir", targetDir.toString());
        Task singleTask = createTestTask(singlePayload);
        String singleDesc = taskHandler.getTaskDesc(singleTask);
        assertTrue(singleDesc.contains("批量复制: 1 个文件/目录"));

        // 测试多文件描述
        JSONObject multiPayload = new JSONObject();
        multiPayload.put("selectedPaths", Arrays.asList(sourceFile.toString(), sourceFile2.toString()));
        multiPayload.put("targetDir", targetDir.toString());
        Task multiTask = createTestTask(multiPayload);
        String multiDesc = taskHandler.getTaskDesc(multiTask);
        assertTrue(multiDesc.contains("批量复制: 2 个文件/目录"));
    }
} 
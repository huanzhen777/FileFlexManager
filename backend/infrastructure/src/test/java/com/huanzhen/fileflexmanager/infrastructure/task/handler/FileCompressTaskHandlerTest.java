package com.huanzhen.fileflexmanager.infrastructure.task.handler;

import com.alibaba.fastjson2.JSONObject;
import com.huanzhen.fileflexmanager.domain.model.entity.Task;
import com.huanzhen.fileflexmanager.domain.model.enums.TaskType;
import com.huanzhen.fileflexmanager.domain.model.params.params.FileCompressParams;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.zip.ZipFile;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class FileCompressTaskHandlerTest extends BaseTaskHandlerTest<FileCompressParams> {

    private Path testDir;
    private Path testFile1;
    private Path testFile2;
    private Path testSubDir;
    private Path outputDir;

    @Override
    protected void setupTestHandler() {
        taskHandler = new FileCompressTaskHandler(taskRepository);
    }

    @BeforeEach
    void setUpTestFiles() throws IOException {
        // 创建测试目录结构
        testDir = Files.createDirectories(testRootPath.resolve("test_compress"));
        testFile1 = Files.createFile(testDir.resolve("test1.txt"));
        testFile2 = Files.createFile(testDir.resolve("test2.txt"));
        testSubDir = Files.createDirectories(testDir.resolve("subdir"));
        Files.createFile(testSubDir.resolve("test3.txt"));
        outputDir = Files.createDirectories(testRootPath.resolve("output"));

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
        return TaskType.FILE_COMPRESS;
    }

    @Test
    void testCompressSingleFile() throws IOException {
        // 准备测试数据
        JSONObject payload = new JSONObject();
        payload.put("selectPath", testFile1.toString());
        payload.put("destinationPath", outputDir.toString());
        payload.put("format", "zip");
        payload.put("level", 6);
        Task task = createTestTask(payload);

        // 执行测试
        taskHandler.handle(task);

        // 验证结果
        Path outputFile = outputDir.resolve("test1.txt.zip");
        assertTrue(Files.exists(outputFile));
        assertTrue(Files.size(outputFile) > 0);
        
        // 验证zip文件是否可以正常打开
        try (ZipFile zipFile = new ZipFile(outputFile.toFile())) {
            assertNotNull(zipFile.getEntry("test1.txt"));
        }
        
        verifyTaskSuccess(task);
        verifyProgressUpdates();
    }

    @Test
    void testCompressDirectory() throws IOException {
        // 准备测试数据
        JSONObject payload = new JSONObject();
        payload.put("selectPath", testDir.toString());
        payload.put("destinationPath", outputDir.toString());
        payload.put("format", "zip");
        payload.put("level", 6);
        Task task = createTestTask(payload);

        // 执行测试
        taskHandler.handle(task);

        // 验证结果
        Path outputFile = outputDir.resolve("test_compress.zip");
        assertTrue(Files.exists(outputFile));
        assertTrue(Files.size(outputFile) > 0);
        
        // 验证zip文件内容
        try (ZipFile zipFile = new ZipFile(outputFile.toFile())) {
            // 检查相对路径的文件
            assertNotNull(zipFile.getEntry("test_compress/test1.txt"), "test1.txt should exist in zip");
            assertNotNull(zipFile.getEntry("test_compress/test2.txt"), "test2.txt should exist in zip");
            assertNotNull(zipFile.getEntry("test_compress/subdir/test3.txt"), "subdir/test3.txt should exist in zip");
            
            // 验证文件内容
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                    zipFile.getInputStream(zipFile.getEntry("test_compress/test1.txt"))))) {
                assertEquals("test content 1", reader.readLine());
            }
        }
        
        verifyTaskSuccess(task);
        verifyProgressUpdates();
    }

    @Test
    void testCompressNonExistentPath() {
        // 准备测试数据
        JSONObject payload = new JSONObject();
        payload.put("selectPath", testRootPath.resolve("non_existent").toString());
        payload.put("destinationPath", outputDir.toString());
        payload.put("format", "zip");
        Task task = createTestTask(payload);

        // 执行测试
        taskHandler.handle(task);

        // 验证结果
        verifyTaskFailure(task, "源路径不存在");
    }

    @Test
    void testCompressWithDifferentFormats() throws IOException {
        String[] formats = {"zip", "tar", "tar.gz"};
        
        for (String format : formats) {
            // 准备测试数据
            JSONObject payload = new JSONObject();
            payload.put("selectPath", testFile1.toString());
            payload.put("destinationPath", outputDir.toString());
            payload.put("format", format);
            payload.put("level", 6);
            Task task = createTestTask(payload);

            // 执行测试
            taskHandler.handle(task);

            // 验证结果
            Path outputFile = outputDir.resolve("test1.txt." + format);
            assertTrue(Files.exists(outputFile));
            assertTrue(Files.size(outputFile) > 0);
            verifyTaskSuccess(task);
            
            // 清理输出文件，为下一次测试准备
            Files.deleteIfExists(outputFile);
        }
    }
} 
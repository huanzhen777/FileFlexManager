package com.huanzhen.fileflexmanager.infrastructure.task.handler;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.huanzhen.fileflexmanager.domain.exception.TaskCancelledException;
import com.huanzhen.fileflexmanager.domain.model.entity.Task;
import com.huanzhen.fileflexmanager.domain.model.enums.TaskStatus;
import com.huanzhen.fileflexmanager.domain.model.enums.TaskType;
import com.huanzhen.fileflexmanager.domain.model.params.params.FileDecompressParams;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;

class FileDecompressTaskHandlerTest extends BaseTaskHandlerTest<FileDecompressParams> {

    @TempDir
    Path tempDir;
    
    Path sourceFile;
    Path targetDir;

    @BeforeEach
    void setUp() throws IOException {
        super.setUp();
        // 创建一个较大的测试压缩文件
        sourceFile = createLargeZipFile();
        
        // 创建目标目录
        targetDir = tempDir.resolve("target");
        Files.createDirectories(targetDir);
    }

    /**
     * 创建一个较大的ZIP文件用于测试
     */
    private Path createLargeZipFile() throws IOException {
        Path zipFile = tempDir.resolve("large_test.zip");
        Path tempContent = tempDir.resolve("temp_content");
        
        // 创建一个大约100MB的测试文件
        try {
            Files.createFile(tempContent);
            byte[] buffer = new byte[1024];
            for (int i = 0; i < 1024 * 100; i++) { // 写入约100MB数据
                Files.write(tempContent, buffer, StandardOpenOption.APPEND);
            }
            
            // 创建ZIP文件
            try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zipFile.toFile()))) {
                ZipEntry entry = new ZipEntry("large_file");
                zos.putNextEntry(entry);
                Files.copy(tempContent, zos);
                zos.closeEntry();
            }
        } finally {
            Files.deleteIfExists(tempContent);
        }
        
        return zipFile;
    }

    @Override
    protected void setupTestHandler() {
        taskHandler = new FileDecompressTaskHandler(taskRepository);
    }

    @Override
    protected TaskType getTaskType() {
        return TaskType.FILE_DECOMPRESS;
    }

    @Test
    void testDecompressInvalidZipFile() throws IOException {
        // 准备测试数据
        JSONObject payload = new JSONObject();
        payload.put("selectPath", sourceFile.toString());
        payload.put("targetPath", targetDir.toString());
        Task task = createTestTask(payload);

        // 执行测试
        taskHandler.handle(task);

        // 验证任务状态
        verifyTaskFailure(task, null);
    }

    //@Test
    void testCancelDecompress() throws InterruptedException {
        FileDecompressParams fileDecompressParams = new FileDecompressParams();
        fileDecompressParams.setSelectPath(sourceFile.toString());
        fileDecompressParams.setDestinationPath(targetDir.toString());
        Task task = createTestTask((JSONObject) JSON.toJSON(fileDecompressParams));

        // 在另一个线程中执行取消操作
        Thread cancelThread = new Thread(() -> {
                task.setStatus(TaskStatus.CANCELLED);
        });
        cancelThread.start();

        // 执行测试
        assertThrows(TaskCancelledException.class, () -> taskHandler.handle(task));

        // 验证任务状态
        verifyTaskCancelled(task);
        
        // 确保取消线程完成
        cancelThread.join();
    }
} 
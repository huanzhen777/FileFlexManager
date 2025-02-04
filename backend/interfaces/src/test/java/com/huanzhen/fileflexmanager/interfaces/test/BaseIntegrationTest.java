package com.huanzhen.fileflexmanager.interfaces.test;

import com.huanzhen.fileflexmanager.interfaces.FileFlexManagerApplication;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;

/**
 * 集成测试基类，提供通用配置和工具方法
 */
@SpringBootTest(classes = FileFlexManagerApplication.class)
@ActiveProfiles("test")
@Transactional
public abstract class BaseIntegrationTest {

    @TempDir
    protected Path tempDir;

    /**
     * 创建测试文件
     */
    protected void createTestFile(Path path, String content) throws Exception {
        Files.write(path, content.getBytes());
    }

    /**
     * 创建测试目录
     */
    protected Path createTestDirectory(String... paths) throws Exception {
        Path dir = tempDir;
        for (String path : paths) {
            dir = dir.resolve(path);
        }
        return Files.createDirectories(dir);
    }

    /**
     * 获取当前时间
     */
    protected LocalDateTime now() {
        return LocalDateTime.now();
    }

    /**
     * 获取测试文件的完整路径
     */
    protected String getTestFilePath(String... paths) {
        Path path = tempDir;
        for (String p : paths) {
            path = path.resolve(p);
        }
        return path.toString();
    }
} 
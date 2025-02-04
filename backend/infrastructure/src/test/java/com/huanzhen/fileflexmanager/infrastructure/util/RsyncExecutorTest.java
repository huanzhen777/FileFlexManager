package com.huanzhen.fileflexmanager.infrastructure.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class RsyncExecutorTest {

    private RsyncExecutor rsyncExecutor;
    
    @TempDir
    Path tempDir;
    
    private Path sourceDir;
    private Path destDir;
    private long totalTestFileSize;

    @BeforeEach
    void setUp() throws Exception {
        rsyncExecutor = new RsyncExecutor();
        
        // 创建测试目录
        sourceDir = tempDir.resolve("source");
        destDir = tempDir.resolve("dest");
        Files.createDirectories(sourceDir);
        Files.createDirectories(destDir);
        
        // 创建测试文件
        totalTestFileSize = createTestFiles();
    }
    
    private long createTestFiles() throws Exception {
        long totalSize = 0;
        // 创建不同大小的测试文件
        int[] fileSizes = {5, 10, 15, 20, 25}; // MB
        for (int i = 0; i < fileSizes.length; i++) {
            Path file = sourceDir.resolve("test" + (i + 1) + ".txt");
            byte[] data = new byte[fileSizes[i] * 1024 * 1024];
            Files.write(file, data);
            totalSize += data.length;
        }
        return totalSize;
    }

    @Test
    void testRsyncExecution() throws Exception {
        // 准备测试数据
        List<String> sourcePaths = new ArrayList<>();
        File[] sourceFiles = sourceDir.toFile().listFiles();
        assertNotNull(sourceFiles, "源目录应该包含文件");
        for (File file : sourceFiles) {
            sourcePaths.add(file.getAbsolutePath());
        }

        RsyncExecutor.RsyncOptions options = RsyncExecutor.RsyncOptions.builder()
                .sourcePaths(sourcePaths)
                .destinationPath(destDir.toString())
                .archive(true)
                .verbose(true)
                .showProgress(true)
                .build();

        CountDownLatch latch = new CountDownLatch(1);
        AtomicBoolean hasProgress = new AtomicBoolean(false);
        AtomicReference<RsyncExecutor.RsyncProgress> lastProgress = new AtomicReference<>();
        AtomicInteger maxProgress = new AtomicInteger(0);

        // 执行rsync并监控进度
        Thread rsyncThread = new Thread(() -> {
            try {
                rsyncExecutor.execute(options, progress -> {
                    if (progress != null) {
                        hasProgress.set(true);
                        lastProgress.set(progress);
                        // 记录最大进度
                        if (progress.getPercentage() > maxProgress.get()) {
                            maxProgress.set(progress.getPercentage());
                        }
                        // 当收到第一个进度更新时，释放锁存器
                        if (progress.getPercentage() > 0) {
                            latch.countDown();
                        }
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        rsyncThread.start();

        // 等待进度更新或超时
        assertTrue(latch.await(30, TimeUnit.SECONDS), "应该在30秒内收到进度更新");
        
        // 验证进度信息
        assertTrue(hasProgress.get(), "应该收到进度更新");
        RsyncExecutor.RsyncProgress progress = lastProgress.get();
        assertNotNull(progress, "进度对象不应为空");
        
        // 验证进度字段
        assertAll("进度信息验证",
            () -> assertTrue(progress.getPercentage() >= 0 && progress.getPercentage() <= 100, 
                    "进度百分比应在0-100之间"),
            () -> assertNotNull(progress.getSpeed(), 
                    "传输速度不应为空"),
            () -> assertTrue(progress.getSpeed().matches("[0-9.]+[kMG]B/s"), 
                    "速度格式应为 xx.xxMB/s"),
            () -> assertNotNull(progress.getRemainingTime(), 
                    "剩余时间不应为空"),
            () -> assertTrue(progress.getRemainingTime().matches("\\d+:\\d+:\\d+"), 
                    "剩余时间格式应为 HH:mm:ss")
        );
        
        // 等待rsync完成
        rsyncThread.join(60000);
        assertFalse(rsyncThread.isAlive(), "rsync应该在60秒内完成");
        
        // 验证最终进度是否达到100%
        assertTrue(maxProgress.get() > 95, "最终进度应该接近100%");
        
        // 验证文件是否成功复制
        long copiedSize = 0;
        for (int i = 1; i <= 5; i++) {
            Path sourceFile = sourceDir.resolve("test" + i + ".txt");
            Path destFile = destDir.resolve("test" + i + ".txt");
            assertTrue(Files.exists(destFile), "目标文件应该存在: " + destFile);
            long fileSize = Files.size(destFile);
            assertEquals(Files.size(sourceFile), fileSize, 
                    "源文件和目标文件大小应该相同: " + destFile);
            copiedSize += fileSize;
        }
        
        // 验证总大小是否正确
        assertEquals(totalTestFileSize, copiedSize, "复制的总大小应该与源文件总大小相同");
    }

    @Test
    void testCancelOperation() throws Exception {
        // 准备测试数据
        List<String> sourcePaths = new ArrayList<>();
        sourcePaths.add(sourceDir.toString() + "/");
        
        RsyncExecutor.RsyncOptions options = RsyncExecutor.RsyncOptions.builder()
                .sourcePaths(sourcePaths)
                .destinationPath(destDir.toString())
                .archive(true)
                .verbose(true)
                .showProgress(true)
                .build();

        // 在另一个线程中执行rsync
        Thread rsyncThread = new Thread(() -> {
            assertThrows(InterruptedException.class, () -> 
                rsyncExecutor.execute(options, progress -> {})
            );
        });
        rsyncThread.start();

        // 等待一小段时间后取消操作
        Thread.sleep(100);
        rsyncExecutor.cancel();
        rsyncThread.join(1000);

        assertFalse(rsyncThread.isAlive(), "rsync线程应该被终止");
    }

    @Test
    void testBuildRsyncCommand() throws Exception {
        // 准备测试数据
        List<String> sourcePaths = List.of("/source/path1", "/source/path2");
        RsyncExecutor.RsyncOptions options = RsyncExecutor.RsyncOptions.builder()
                .sourcePaths(sourcePaths)
                .destinationPath("/dest/path")
                .archive(true)
                .verbose(true)
                .showProgress(true)
                .removeSource(true)
                .additionalOptions(List.of("--delete"))
                .build();

        // 执行测试
        List<String> command = buildRsyncCommandForTest(options);

        // 验证结果
        assertAll(
            () -> assertTrue(command.contains("rsync")),
            () -> assertTrue(command.contains("-a")),
            () -> assertTrue(command.contains("-v")),
            () -> assertTrue(command.contains("--info=progress2")),
            () -> assertTrue(command.contains("--delete")),
            () -> assertTrue(command.contains("--remove-source-files")),
            () -> assertTrue(command.contains("/source/path1")),
            () -> assertTrue(command.contains("/source/path2")),
            () -> assertTrue(command.contains("/dest/path"))
        );
    }

    @Test
    void testRsyncWithRemoveSource() throws Exception {
        // 准备测试数据
        Path sourceFile1 = sourceDir.resolve("test_remove1.txt");
        Path sourceFile2 = sourceDir.resolve("test_remove2.txt");
        String testContent1 = "test content for removal 1";
        String testContent2 = "test content for removal 2";
        Files.write(sourceFile1, testContent1.getBytes());
        Files.write(sourceFile2, testContent2.getBytes());

        List<String> sourcePaths = List.of(
            sourceFile1.toString(),
            sourceFile2.toString()
        );

        RsyncExecutor.RsyncOptions options = RsyncExecutor.RsyncOptions.builder()
                .sourcePaths(sourcePaths)
                .destinationPath(destDir.toString())
                .archive(true)
                .verbose(true)
                .showProgress(true)
                .removeSource(true)  // 启用源文件删除
                .build();

        // 执行rsync
        rsyncExecutor.execute(options, progress -> {
            // 对于多文件操作，不检查进度
        });

        // 验证目标文件存在且内容正确
        Path destFile1 = destDir.resolve("test_remove1.txt");
        Path destFile2 = destDir.resolve("test_remove2.txt");
        assertTrue(Files.exists(destFile1), "目标文件1应该存在");
        assertTrue(Files.exists(destFile2), "目标文件2应该存在");
        assertEquals(testContent1, Files.readString(destFile1), "目标文件1内容应该与源文件相同");
        assertEquals(testContent2, Files.readString(destFile2), "目标文件2内容应该与源文件相同");

        // 验证源文件已被删除
        assertFalse(Files.exists(sourceFile1), "源文件1应该已被删除");
        assertFalse(Files.exists(sourceFile2), "源文件2应该已被删除");
    }

    // 辅助方法，用于测试命令构建
    private List<String> buildRsyncCommandForTest(RsyncExecutor.RsyncOptions options) throws Exception {
        // 使用反射调用私有方法
        java.lang.reflect.Method buildCommand = RsyncExecutor.class.getDeclaredMethod("buildRsyncCommand", RsyncExecutor.RsyncOptions.class);
        buildCommand.setAccessible(true);
        return (List<String>) buildCommand.invoke(rsyncExecutor, options);
    }
} 
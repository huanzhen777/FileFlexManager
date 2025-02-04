/*
package com.huanzhen.fileflexmanager.application.service.handler;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSON;
import com.huanzhen.fileflexmanager.domain.model.entity.Task;
import com.huanzhen.fileflexmanager.domain.model.enums.TaskType;
import com.huanzhen.fileflexmanager.domain.model.params.TaskParams.FileChangeOwnerParams;
import com.huanzhen.fileflexmanager.domain.repository.TaskRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.PosixFileAttributeView;
import java.nio.file.attribute.UserPrincipal;
import java.nio.file.attribute.UserPrincipalLookupService;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
@Component
public class FileChangeOwnerTaskHandler extends BaseTaskHandler<FileChangeOwnerParams> {
    private final AtomicBoolean isCancelled = new AtomicBoolean(false);
    private long totalFiles = 0;
    private long processedFiles = 0;

    public FileChangeOwnerTaskHandler(TaskRepository taskRepository) {
        super(taskRepository);
    }

    @Override
    public TaskType getTaskType() {
        return TaskType.FILE_CHANGE_OWNER;
    }

    @Override
    protected void doHandle(Task task) throws Exception {
        FileChangeOwnerParams params = parseTaskParam(task);
        Path path = Paths.get(params.getSelectPath());

        if (!Files.exists(path)) {
            throw new IllegalArgumentException("路径不存在：" + params.getSelectPath());
        }

        // 验证用户名是否有效
        UserPrincipal newOwner;
        try {
            UserPrincipalLookupService lookupService = path.getFileSystem().getUserPrincipalLookupService();
            newOwner = lookupService.lookupPrincipalByName(params.getOwner());
        } catch (IOException e) {
            throw new IllegalArgumentException("无效的用户名：" + params.getOwner());
        }

        // 计算总文件数
        countFiles(path);
        processedFiles = 0;

        log.info("开始修改所有者: {} -> {}", params.getSelectPath(), params.getOwner());
        task.updateProgress(0, "开始修改所有者...");
        taskRepository.updateTask(task);

        // 执行修改
        if (Files.isDirectory(path)) {
            changeDirectoryOwner(path, newOwner, task);
        } else {
            changeFileOwner(path, newOwner);
            processedFiles++;
            updateProgressIfNeeded(task);
        }

        log.info("修改所有者完成");
        task.markAsCompleted("修改所有者完成");
    }

    private void countFiles(Path path) throws IOException {
        AtomicLong count = new AtomicLong(0);
        if (Files.isDirectory(path)) {
            Files.walkFileTree(path, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                    count.incrementAndGet();
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                    count.incrementAndGet();
                    return FileVisitResult.CONTINUE;
                }
            });
            totalFiles = count.get();
        } else {
            totalFiles = 1;
        }
    }

    private void changeDirectoryOwner(Path directory, UserPrincipal owner, Task task) throws IOException {
        Files.walkFileTree(directory, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                assertNotCancelled(task);
                changeFileOwner(file, owner);
                processedFiles++;
                updateProgressIfNeeded(task);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                assertNotCancelled(task);
                changeFileOwner(dir, owner);
                processedFiles++;
                updateProgressIfNeeded(task);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    private void changeFileOwner(Path file, UserPrincipal owner) throws IOException {
        try {
            // 尝试使用POSIX属性视图
            PosixFileAttributeView view = Files.getFileAttributeView(file, PosixFileAttributeView.class);
            if (view != null) {
                view.setOwner(owner);
            } else {
                // 如果POSIX不可用，使用基本的所有者设置
                Files.setOwner(file, owner);
            }
        } catch (IOException e) {
            log.error("修改文件所有者失败: {}", file, e);
            throw e;
        }
    }

    private void updateProgressIfNeeded(Task task) {
        int progress = totalFiles > 0 ? (int) ((processedFiles * 100) / totalFiles) : 0;
        updateProgress(task, progress, StrUtil.format("已处理: {}/{} 个文件 ({:.2f}%)",
                processedFiles, totalFiles, (double) progress));
    }

    @Override
    protected void onCancel(Task task) {
        isCancelled.set(true);
    }

    @Override
    public String getTaskDesc(Task task) {
        FileChangeOwnerParams params = parseTaskParam(task);
        return StrUtil.format("修改所有者: {} -> {}", params.getSelectPath(), params.getOwner());
    }
} */

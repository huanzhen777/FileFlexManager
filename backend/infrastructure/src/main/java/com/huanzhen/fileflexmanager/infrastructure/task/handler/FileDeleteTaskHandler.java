package com.huanzhen.fileflexmanager.infrastructure.task.handler;

import cn.hutool.core.lang.Assert;
import cn.hutool.core.util.StrUtil;
import com.huanzhen.fileflexmanager.domain.model.entity.FileIndex;
import com.huanzhen.fileflexmanager.domain.model.entity.Task;
import com.huanzhen.fileflexmanager.domain.model.enums.TaskType;
import com.huanzhen.fileflexmanager.domain.model.params.params.FileDeleteParams;
import com.huanzhen.fileflexmanager.domain.repository.FileIndexRepository;
import com.huanzhen.fileflexmanager.domain.repository.FileTagRepository;
import com.huanzhen.fileflexmanager.domain.repository.TaskRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Scope("prototype")
@Component
public class FileDeleteTaskHandler extends BaseTaskHandler<FileDeleteParams> {
    private long totalFiles = 0;
    private long deletedFiles = 0;

    private final FileIndexRepository fileIndexRepository;
    private final FileTagRepository fileTagRepository;

    public FileDeleteTaskHandler(TaskRepository taskRepository, FileIndexRepository fileIndexRepository, FileTagRepository fileTagRepository) {
        super(taskRepository);
        this.fileIndexRepository = fileIndexRepository;
        this.fileTagRepository = fileTagRepository;
    }

    @Override
    public TaskType getTaskType() {
        return TaskType.FILE_DELETE;
    }

    @Override
    protected void doHandle(Task task) throws Exception {
        FileDeleteParams params = parseTaskParam(task);
        Assert.notEmpty(params.getSelectedPaths(), "选择文件/文件夹不能为空");

        List<Path> paths = new ArrayList<>(params.getSelectedPaths().stream().map(Paths::get).toList());

        // 验证所有路径是否存在
        for (Path path : paths) {
            if (!Files.exists(path)) {
                throw new IllegalArgumentException("路径不存在：" + path);
            }
        }

        // 计算所有文件的总数
        totalFiles = 0;
        for (Path path : paths) {
            countFiles(path);
        }
        deletedFiles = 0;

        log.info("开始删除 {} 个文件/目录", paths.size());
        updateProgress(task, 0, "开始删除...");
        taskRepository.updateTask(task);

        // 执行删除
        for (Path path : paths) {
            assertNotCancelled(task);
            if (Files.isDirectory(path)) {
                deleteDirectoryWithTags(path, task);
            } else {
                deleteFileWithTags(path);
                deletedFiles++;
                updateProgressIfNeeded(task);
            }
        }

        log.info("删除完成");
        task.markAsCompleted("删除完成");
    }


    private void countFiles(Path path) throws IOException {
        if (Files.isDirectory(path)) {
            Files.walkFileTree(path, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                    totalFiles++;
                    return FileVisitResult.CONTINUE;
                }
            });
        } else {
            totalFiles++;
        }
    }

    private void deleteDirectoryWithTags(Path directory, Task task) throws IOException {
        // 先删除目录及其内容
        Files.walkFileTree(directory, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                assertNotCancelled(task);
                Files.delete(file);
                deletedFiles++;
                updateProgressIfNeeded(task);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                if (exc != null) throw exc;
                assertNotCancelled(task);
                Files.delete(dir);
                return FileVisitResult.CONTINUE;
            }
        });

        // 删除成功后，再删除标签和索引
        List<FileIndex> allIndexes = fileIndexRepository.findByParentPath(directory.toString());
        for (FileIndex index : allIndexes) {
            fileTagRepository.deleteByFileId(index.getId());
        }

        // 删除目录本身的标签
        FileIndex dirIndex = fileIndexRepository.findByPath(directory.toString());
        if (dirIndex != null) {
            fileTagRepository.deleteByFileId(dirIndex.getId());
        }

        // 删除目录的索引
        fileIndexRepository.deleteStaleIndexes(directory.toString(), LocalDateTime.now());
    }

    private void deleteFileWithTags(Path file) throws IOException {
        // 先删除文件
        Files.delete(file);

        // 删除成功后，再删除标签和索引
        FileIndex fileIndex = fileIndexRepository.findByPath(file.toString());
        if (fileIndex != null) {
            fileTagRepository.deleteByFileId(fileIndex.getId());
        }
        // 无论是否找到索引，都删除路径对应的索引记录
        fileIndexRepository.deleteByPath(file.toString());
    }

    private void updateProgressIfNeeded(Task task) {
        int progress = totalFiles > 0 ? (int) ((deletedFiles * 100) / totalFiles) : 0;
        updateProgress(task, progress, StrUtil.format("已删除: {}/{} 个文件 ({:.2f}%)",
                deletedFiles, totalFiles, (double) progress));
    }

    @Override
    protected void onCancel(Task task) {
        // 删除操作不需要特殊的取消处理
    }

    @Override
    public String getTaskDesc(Task task) {
        FileDeleteParams params = parseTaskParam(task);
        return StrUtil.format("批量删除: {} 个文件/目录", params.getSelectedPaths().size());
    }
} 
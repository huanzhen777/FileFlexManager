package com.huanzhen.fileflexmanager.infrastructure.task.handler;

import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.DigestUtil;
import com.huanzhen.fileflexmanager.domain.model.entity.FileIndex;
import com.huanzhen.fileflexmanager.domain.model.entity.Task;
import com.huanzhen.fileflexmanager.domain.model.enums.TaskType;
import com.huanzhen.fileflexmanager.domain.model.params.params.FileIndexParams;
import com.huanzhen.fileflexmanager.domain.repository.FileIndexRepository;
import com.huanzhen.fileflexmanager.domain.repository.TaskRepository;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;

@Slf4j
@Scope("prototype")
@Component
public class FileIndexTaskHandler extends BaseTaskHandler<FileIndexParams> {
    private final FileIndexRepository fileIndexRepository;

    @Data
    private static class TaskState {
        private final AtomicBoolean isCancelled = new AtomicBoolean(false);
        private final Map<String, Long> directorySizes = new HashMap<>();
        private long lastProgressUpdate = 0;
        private long processedFiles = 0;
        private long processedDirs = 0;
        private long totalFiles = 0;
        private long totalDirs = 0;
    }

    public FileIndexTaskHandler(FileIndexRepository fileIndexRepository, TaskRepository taskRepository) {
        super(taskRepository);
        this.fileIndexRepository = fileIndexRepository;
    }

    @Override
    public TaskType getTaskType() {
        return TaskType.FILE_INDEX;
    }

    @Override
    protected void doHandle(Task task) throws Exception {
        TaskState state = new TaskState();
        FileIndexParams params = parseTaskParam(task);
        Path rootPath = Paths.get(params.getSelectPath());

        // 记录开始时间，用于后续清理过期记录
        LocalDateTime indexStartTime = LocalDateTime.now();

        if (!Files.exists(rootPath)) {
            throw new IllegalArgumentException("目录不存在：" + params.getSelectPath());
        }

        log.info("开始扫描目录: {}, 最大深度: {}, 计算MD5: {}",
                params.getSelectPath(),
                params.getMaxDepth() > 0 ? params.getMaxDepth() : "无限制",
                params.getCalculateMd5());

        // 先统计总数
        updateProgress(task, 0, "正在统计文件总数...");
        countTotalItems(rootPath, params.getMaxDepth(), state);
        log.info("统计完成：总文件数: {}, 总目录数: {}", state.totalFiles, state.totalDirs);

        updateProgress(task, 0, "开始扫描...");
        state.lastProgressUpdate = System.currentTimeMillis();
        taskRepository.updateTask(task);

        // 第一次遍历：处理文件
        try (Stream<Path> paths = createPathStream(rootPath, params.getMaxDepth())) {
            for (Path path : (Iterable<Path>) paths::iterator) {
                assertNotCancelled(task);

                if (Files.isRegularFile(path)) {
                    log.debug("正在处理文件: {}", path);
                    long fileSize = processFile(path, params.getCalculateMd5());
                    updateParentDirectorySizes(path, fileSize, state);
                    state.processedFiles++;

                    updateProgressIfNeeded(task, state);
                }
            }
        }

        log.info("文件扫描完成，共处理 {} 个文件", state.processedFiles);

        // 第二次遍历：处理目录
        try (Stream<Path> dirPaths = createPathStream(rootPath, params.getMaxDepth())) {
            for (Path path : (Iterable<Path>) dirPaths::iterator) {
                assertNotCancelled(task);

                if (Files.isDirectory(path)) {
                    log.debug("正在处理目录: {}", path);
                    processDirectory(path, state);
                    state.processedDirs++;

                    updateProgressIfNeeded(task, state);
                }
            }
        }

        log.info("扫描完成，共处理 {} 个文件，{} 个目录", state.processedFiles, state.processedDirs);

        // 清理失效的文件索引
        int deletedCount = cleanupStaleIndexes(rootPath.toString(), indexStartTime);
        log.info("清理完成，删除了 {} 条失效的文件索引记录", deletedCount);

        task.markAsCompleted(StrUtil.format("索引完成，共处理 {} 个文件，{} 个目录，清理 {} 条失效记录",
                state.processedFiles, state.processedDirs, deletedCount));
    }

    private void updateParentDirectorySizes(Path path, long size, TaskState state) {
        Path parent = path.getParent();
        while (parent != null) {
            String parentPath = parent.toString();
            state.directorySizes.merge(parentPath, size, Long::sum);
            parent = parent.getParent();
        }
    }

    private void processDirectory(Path path, TaskState state) {
        try {
            log.debug("开始处理目录: {}", path);
            File dir = path.toFile();
            FileIndex fileIndex = new FileIndex();
            fileIndex.setPath(path.toString());
            fileIndex.setName(dir.getName());
            fileIndex.setLastModified(toLocalDateTime(dir.lastModified()));
            fileIndex.setIsDir(true);
            fileIndex.setParentPath(path.getParent() != null ? path.getParent().toString() : null);

            fileIndex.setSize(state.directorySizes.getOrDefault(path.toString(), 0L));

            FileIndex existing = fileIndexRepository.findByPath(path.toString());
            if (existing != null) {
                fileIndex.setId(existing.getId());
                fileIndexRepository.updateFileIndex(fileIndex);
            } else {
                fileIndexRepository.save(fileIndex);
            }
        } catch (Exception e) {
            log.error("处理目录失败: {}", path, e);
        }
    }

    private void updateProgressIfNeeded(Task task, TaskState state) {
        double progress = (state.totalFiles + state.totalDirs) > 0 ?
                (double) (state.processedFiles + state.processedDirs) / (state.totalFiles + state.totalDirs) * 100 : 0;

        String progressMessage = String.format(
                "进度: %.1f%% - 已处理: %d/%d (文件: %d/%d, 目录: %d/%d)",
                progress,
                state.processedFiles + state.processedDirs,
                state.totalFiles + state.totalDirs,
                state.processedFiles,
                state.totalFiles,
                state.processedDirs,
                state.totalDirs
        );

        updateProgress(task, (int) progress, progressMessage);
    }

    private void countTotalItems(Path rootPath, int maxDepth, TaskState state) {
        try (Stream<Path> paths = createPathStream(rootPath, maxDepth)) {
            paths.forEach(path -> {
                if (Files.isRegularFile(path)) {
                    state.totalFiles++;
                } else if (Files.isDirectory(path)) {
                    state.totalDirs++;
                }
            });
        } catch (IOException e) {
            log.error("统计文件总数时发生错误", e);
        }
    }

    @Override
    protected void onCancel(Task task) {

    }

    private void checkAccess(Path path) throws IOException {
        if (!Files.isReadable(path)) {
            throw new IOException("无法读取文件: " + path);
        }
    }

    private long processFile(Path path, boolean calculateMd5) {
        try {
            log.debug("开始处理文件: {}", path);
            checkAccess(path);
            File file = path.toFile();
            long fileSize = file.length();

            FileIndex fileIndex = new FileIndex();
            fileIndex.setPath(path.toString());
            fileIndex.setName(file.getName());
            fileIndex.setSize(fileSize);
            fileIndex.setLastModified(toLocalDateTime(file.lastModified()));
            fileIndex.setIsDir(false);
            fileIndex.setParentPath(path.getParent().toString());

            if (calculateMd5) {
                try {
                    fileIndex.setMd5(DigestUtil.md5Hex(file));
                } catch (Exception e) {
                    log.error("计算文件MD5失败: {}", path, e);
                }
            }

            FileIndex existing = fileIndexRepository.findByPath(path.toString());
            if (existing != null) {
                fileIndex.setId(existing.getId());
                fileIndexRepository.updateFileIndex(fileIndex);
            } else {
                fileIndexRepository.save(fileIndex);
            }

            return fileSize;
        } catch (Exception e) {
            log.error("处理文件失败: {}", path, e);
            return 0L;
        }
    }

    private Stream<Path> createPathStream(Path rootPath, int maxDepth) throws IOException {
        return maxDepth > 0 ? Files.walk(rootPath, maxDepth) : Files.walk(rootPath);
    }

    private LocalDateTime toLocalDateTime(long timestamp) {
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(timestamp), ZoneId.systemDefault());
    }

    /**
     * 清理失效的文件索引
     * @param rootPath 根路径
     * @param indexStartTime 索引开始时间
     * @return 清理的记录数
     */
    private int cleanupStaleIndexes(String rootPath, LocalDateTime indexStartTime) {
        log.info("开始清理失效的文件索引，根路径: {}", rootPath);
        try {
            // 删除在本次索引中未更新的记录（lastModified早于indexStartTime的记录）
            return fileIndexRepository.deleteStaleIndexes(rootPath, indexStartTime);
        } catch (Exception e) {
            log.error("清理失效文件索引时发生错误", e);
            return 0;
        }
    }

    @Override
    public String getTaskDesc(Task task) {
        FileIndexParams params = parseTaskParam(task);
        return StrUtil.format("索引目录: {} (最大层级: {})",
                params.getSelectPath(),
                params.getMaxDepth() > 0 ? params.getMaxDepth() : "无限制");
    }
} 
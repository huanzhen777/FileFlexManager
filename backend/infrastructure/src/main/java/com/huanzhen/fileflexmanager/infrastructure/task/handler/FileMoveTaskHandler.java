package com.huanzhen.fileflexmanager.infrastructure.task.handler;

import cn.hutool.core.lang.Assert;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSON;
import com.huanzhen.fileflexmanager.domain.model.entity.FileIndex;
import com.huanzhen.fileflexmanager.domain.model.entity.FileTag;
import com.huanzhen.fileflexmanager.domain.model.entity.Task;
import com.huanzhen.fileflexmanager.domain.model.enums.TaskType;
import com.huanzhen.fileflexmanager.domain.model.params.params.FileMoveParams;
import com.huanzhen.fileflexmanager.domain.repository.FileIndexRepository;
import com.huanzhen.fileflexmanager.domain.repository.FileTagRepository;
import com.huanzhen.fileflexmanager.domain.repository.TaskRepository;
import com.huanzhen.fileflexmanager.infrastructure.util.RsyncExecutor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
@Scope("prototype")
@Component
public class FileMoveTaskHandler extends BaseTaskHandler<FileMoveParams> {
    private long totalSize = 0;
    private long movedSize = 0;

    private final RsyncExecutor rsyncExecutor;
    private final FileIndexRepository fileIndexRepository;
    private final FileTagRepository fileTagRepository;

    public FileMoveTaskHandler(TaskRepository taskRepository, FileIndexRepository fileIndexRepository, FileTagRepository fileTagRepository) {
        super(taskRepository);
        this.rsyncExecutor = new RsyncExecutor();
        this.fileIndexRepository = fileIndexRepository;
        this.fileTagRepository = fileTagRepository;
    }

    @Override
    public TaskType getTaskType() {
        return TaskType.FILE_MOVE;
    }


    @Override
    protected void doHandle(Task task) throws Exception {
        FileMoveParams params = parseTaskParam(task);
        Assert.notEmpty(params.getSelectedPaths(), "选择文件夹/文件不能为空");

        Path targetDir = Paths.get(params.getTargetDir());
        List<Path> sourcePaths = new ArrayList<>(params.getSelectedPaths().stream().map(Paths::get).toList());

        // 验证所有路径是否存在
        for (Path sourcePath : sourcePaths) {
            if (!Files.exists(sourcePath)) {
                throw new IllegalArgumentException("源文件不存在：" + sourcePath);
            }
        }

        // 检查目标目录是否存在
        if (!Files.exists(targetDir)) {
            throw new IllegalArgumentException("目标目录不存在：" + targetDir);
        }

        // 检查目标文件是否已存在且不允许覆盖
        if (!params.isOverwrite()) {
            for (Path sourcePath : sourcePaths) {
                Path destinationPath = targetDir.resolve(sourcePath.getFileName());
                if (Files.exists(destinationPath)) {
                    throw new IllegalArgumentException("目标文件已存在且不允许覆盖：" + destinationPath);
                }
            }
        }

        // 计算总大小
        totalSize = 0;
        for (Path sourcePath : sourcePaths) {
            calculateTotalSize(sourcePath);
        }
        movedSize = 0;

        log.info("开始移动 {} 个文件到目标目录: {}", sourcePaths.size(), targetDir);
        updateProgress(task, 0, "开始移动...");
        taskRepository.updateTask(task);

        // 收集需要使用rsync移动的文件
        List<Path> rsyncPaths = new ArrayList<>();
        // 收集所有成功移动的文件路径和目标路径的映射
        Map<Path, Path> successfulMoves = new HashMap<>();
        Map<String, List<FileTag>> allSourceTagsMap = new HashMap<>();

        // 如果需要移动标签，先收集所有源文件的标签
        if (params.isMoveTags()) {
            for (Path sourcePath : sourcePaths) {
                boolean isDirectory = Files.isDirectory(sourcePath);
                collectSourceTags(sourcePath, isDirectory, allSourceTagsMap);
            }
        }

        // 首先尝试使用原子移动
        for (Path sourcePath : sourcePaths) {
            assertNotCancelled(task);
            Path destinationPath = targetDir.resolve(sourcePath.getFileName());
            boolean isDirectory = Files.isDirectory(sourcePath);

            // 计算当前文件/目录的大小（在移动前）
            long currentSize = calculateCurrentSize(sourcePath);

            // 尝试直接移动
            try {
                if (params.isOverwrite()) {
                    Files.move(sourcePath, destinationPath, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
                } else {
                    Files.move(sourcePath, destinationPath, StandardCopyOption.ATOMIC_MOVE);
                }
                movedSize += currentSize;
                updateProgressIfNeeded(task);
                
                // 记录成功移动的文件
                successfulMoves.put(sourcePath, destinationPath);
                // 删除文件索引
                deleteFileIndexes(sourcePath.toString(), isDirectory);
            } catch (AtomicMoveNotSupportedException e) {
                log.info("原子移动不支持，添加到rsync队列: {}", sourcePath);
                rsyncPaths.add(sourcePath);
            }
        }

        // 如果有需要用rsync移动的文件，批量处理
        if (!rsyncPaths.isEmpty()) {
            log.info("使用rsync批量移动 {} 文件", JSON.toJSONString(rsyncPaths));
            RsyncExecutor.RsyncOptions options = RsyncExecutor.RsyncOptions.builder()
                    .sourcePaths(rsyncPaths.stream().map(Path::toString).toList())
                    .destinationPath(targetDir.toString())
                    .archive(true)
                    .verbose(true)
                    .showProgress(true)
                    .removeSource(true)
                    .build();

            rsyncExecutor.execute(options, (rsyncProgress) -> {
                updateProgress(task, rsyncProgress.getPercentage(), rsyncProgress.generateMsg());
                taskRepository.updateTask(task);
            });

            // 记录rsync移动的文件
            for (Path sourcePath : rsyncPaths) {
                Path destinationPath = targetDir.resolve(sourcePath.getFileName());
                successfulMoves.put(sourcePath, destinationPath);
                // 删除文件索引
                deleteFileIndexes(sourcePath.toString(), Files.isDirectory(sourcePath));
            }
        }

        // 所有文件移动完成后，统一处理标签
        if (params.isMoveTags() && !allSourceTagsMap.isEmpty()) {
            log.info("开始处理文件标签...");
            // 一次性处理所有标签
            moveAllFileTags(allSourceTagsMap, successfulMoves);
            log.info("文件标签处理完成");
        }

        log.info("移动完成");
        task.markAsCompleted("移动完成");
    }

    private void collectSourceTags(Path sourcePath, boolean isDirectory, Map<String, List<FileTag>> sourceTagsMap) {
        // 如果已经收集过这个路径的标签，直接返回
        if (sourceTagsMap.containsKey(sourcePath.toString())) {
            return;
        }

        if (isDirectory) {
            // 获取目录本身的标签
            FileIndex dirIndex = fileIndexRepository.findByPath(sourcePath.toString());
            if (dirIndex != null) {
                List<FileTag> dirTags = fileTagRepository.findByFileId(dirIndex.getId());
                if (!dirTags.isEmpty()) {
                    sourceTagsMap.put(sourcePath.toString(), dirTags);
                }
            }

            // 获取目录下所有文件的索引和标签
            List<FileIndex> allIndexes = fileIndexRepository.findByParentPath(sourcePath.toString());
            for (FileIndex index : allIndexes) {
                // 如果已经收集过这个路径的标签，跳过
                if (sourceTagsMap.containsKey(index.getPath())) {
                    continue;
                }
                List<FileTag> tags = fileTagRepository.findByFileId(index.getId());
                if (!tags.isEmpty()) {
                    sourceTagsMap.put(index.getPath(), tags);
                }
            }
        } else {
            // 获取文件的标签
            FileIndex sourceFileIndex = fileIndexRepository.findByPath(sourcePath.toString());
            if (sourceFileIndex != null) {
                List<FileTag> tags = fileTagRepository.findByFileId(sourceFileIndex.getId());
                if (!tags.isEmpty()) {
                    sourceTagsMap.put(sourcePath.toString(), tags);
                }
            }
        }
    }

    private long calculateCurrentSize(Path path) throws IOException {
        if (Files.isDirectory(path)) {
            AtomicLong size = new AtomicLong(0);
            Files.walkFileTree(path, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                    size.addAndGet(attrs.size());
                    return FileVisitResult.CONTINUE;
                }
            });
            return size.get();
        } else {
            return Files.size(path);
        }
    }

    private void moveAllFileTags(Map<String, List<FileTag>> sourceTagsMap, Map<Path, Path> pathMappings) {
        Map<String, FileIndex> newIndexCache = new HashMap<>();
        
        for (Map.Entry<String, List<FileTag>> entry : sourceTagsMap.entrySet()) {
            String oldPath = entry.getKey();
            List<FileTag> tags = entry.getValue();
            
            // 找到对应的路径映射
            Path sourcePath = null;
            Path destinationPath = null;
            for (Map.Entry<Path, Path> mapping : pathMappings.entrySet()) {
                if (oldPath.startsWith(mapping.getKey().toString())) {
                    sourcePath = mapping.getKey();
                    destinationPath = mapping.getValue();
                    break;
                }
            }
            
            if (sourcePath == null || destinationPath == null) {
                continue;
            }

            // 计算新路径
            String newPath;
            if (oldPath.equals(sourcePath.toString())) {
                newPath = destinationPath.toString();
            } else {
                String relativePath = sourcePath.relativize(Paths.get(oldPath)).toString();
                newPath = destinationPath.resolve(relativePath).toString();
            }
            
            // 检查缓存中是否已存在新的文件索引
            FileIndex newIndex = newIndexCache.get(newPath);
            if (newIndex == null) {
                // 创建新的文件索引
                FileIndex oldIndex = fileIndexRepository.findByPath(oldPath);
                if (oldIndex != null) {
                    newIndex = new FileIndex();
                    newIndex.setPath(newPath);
                    newIndex.setName(Paths.get(newPath).getFileName().toString());
                    newIndex.setIsDir(oldIndex.getIsDir());
                    newIndex.setSize(oldIndex.getSize());
                    newIndex.setMd5(oldIndex.getMd5());
                    newIndex.setMimeType(oldIndex.getMimeType());
                    newIndex.setParentPath(Paths.get(newPath).getParent().toString());
                    newIndex.setLastModified(LocalDateTime.now());
                    newIndex = fileIndexRepository.save(newIndex);
                    newIndexCache.put(newPath, newIndex);
                }
            }

            // 为新文件创建标签关联
            if (newIndex != null) {
                for (FileTag sourceTag : tags) {
                    FileTag newFileTag = new FileTag(newIndex.getId(), sourceTag.getTagId());
                    fileTagRepository.save(newFileTag);
                }
            }
        }
    }

    private void moveUsingRsync(Path sourcePath, Path destinationPath, Task task) throws Exception {
        RsyncExecutor.RsyncOptions options = RsyncExecutor.RsyncOptions.builder()
                .sourcePaths(List.of(sourcePath.toString()))
                .destinationPath(destinationPath.toString())
                .archive(true)
                .verbose(true)
                .showProgress(true)
                .removeSource(true)
                .build();

        rsyncExecutor.execute(options, (rsyncProgress) -> {
            updateProgress(task, rsyncProgress.getPercentage(), rsyncProgress.generateMsg());
            taskRepository.updateTask(task);
        });
    }

    private void calculateTotalSize(Path path) throws IOException {
        if (Files.isDirectory(path)) {
            Files.walkFileTree(path, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                    totalSize += attrs.size();
                    return FileVisitResult.CONTINUE;
                }
            });
        } else {
            totalSize = Files.size(path);
        }
    }

    private void updateProgressIfNeeded(Task task) {
        int progress = totalSize > 0 ? (int) ((movedSize * 100) / totalSize) : 0;
        updateProgress(task, progress, String.format("已移动: %d/%d bytes (%.2f%%)",
                movedSize, totalSize, (double) progress));
    }

    private void deleteFileIndexes(String sourcePath, boolean isDirectory) {
        if (isDirectory) {
            // 如果是目录，删除该目录下所有文件的索引
            fileIndexRepository.deleteStaleIndexes(sourcePath, LocalDateTime.now());
        } else {
            // 如果是文件，只删除该文件的索引
            fileIndexRepository.deleteByPath(sourcePath);
        }
    }

    @Override
    protected void onCancel(Task task) {
        rsyncExecutor.cancel();
    }

    @Override
    public String getTaskDesc(Task task) {
        FileMoveParams params = parseTaskParam(task);
        return StrUtil.format("批量移动: {} 个文件/目录 -> {}", params.getSelectedPaths().size(), params.getTargetDir());
    }
} 
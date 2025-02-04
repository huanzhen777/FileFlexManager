package com.huanzhen.fileflexmanager.infrastructure.task.handler;

import cn.hutool.core.util.StrUtil;
import com.huanzhen.fileflexmanager.domain.model.entity.Task;
import com.huanzhen.fileflexmanager.domain.model.enums.TaskType;
import com.huanzhen.fileflexmanager.domain.model.params.params.FileDecompressParams;
import com.huanzhen.fileflexmanager.domain.repository.TaskRepository;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.io.*;
import java.nio.file.*;

@Slf4j
@Scope("prototype")
@Component
public class FileDecompressTaskHandler extends BaseTaskHandler<FileDecompressParams> {
    private long totalSize = 0;
    private long processedSize = 0;
    private long lastProgressUpdate = 0;
    private static final int UPDATE_PROGRESS_INTERVAL = 1000; // 毫秒
    private static final int BUFFER_SIZE = 8192;

    public FileDecompressTaskHandler(TaskRepository taskRepository) {
        super(taskRepository);
    }

    @Override
    public TaskType getTaskType() {
        return TaskType.FILE_DECOMPRESS;
    }

    @Override
    protected void doHandle(Task task) throws Exception {
        FileDecompressParams params = parseTaskParam(task);
        Path sourcePath = Paths.get(params.getSelectPath());
        Path destinationPath = Paths.get(params.getDestinationPath());
        boolean overwrite = params.getOverwrite() != null && params.getOverwrite();

        if (!Files.exists(sourcePath)) {
            throw new IllegalArgumentException("源文件不存在：" + params.getSelectPath());
        }

        // 创建目标目录
        Files.createDirectories(destinationPath);

        // 获取文件大小作为进度参考
        totalSize = Files.size(sourcePath);
        processedSize = 0;
        lastProgressUpdate = System.currentTimeMillis();

        log.info("开始解压: {} -> {}", params.getSelectPath(), params.getDestinationPath());
        updateProgress(task,0,"开始解压...");
        taskRepository.updateTask(task);

        // 根据文件扩展名选择解压方法
        String fileName = sourcePath.getFileName().toString().toLowerCase();
        if (fileName.endsWith(".zip")) {
            decompressZip(sourcePath, destinationPath, overwrite, task);
        } else if (fileName.endsWith(".tar")) {
            decompressTar(sourcePath, destinationPath, overwrite, task);
        } else if (fileName.endsWith(".tar.gz") || fileName.endsWith(".tgz")) {
            decompressTarGz(sourcePath, destinationPath, overwrite, task);
        } else {
            throw new IllegalArgumentException("不支持的压缩格式");
        }

        log.info("解压完成");
        task.markAsCompleted("解压完成");
    }

    private void decompressZip(Path source, Path destination, boolean overwrite, Task task) throws IOException {
        try (InputStream fileIn = Files.newInputStream(source);
             BufferedInputStream bufferedIn = new BufferedInputStream(fileIn);
             ZipArchiveInputStream zipIn = new ZipArchiveInputStream(bufferedIn)) {

            extractArchive(zipIn, destination, overwrite, task);
        }
    }

    private void decompressTar(Path source, Path destination, boolean overwrite, Task task) throws IOException {
        try (InputStream fileIn = Files.newInputStream(source);
             BufferedInputStream bufferedIn = new BufferedInputStream(fileIn);
             TarArchiveInputStream tarIn = new TarArchiveInputStream(bufferedIn)) {

            extractArchive(tarIn, destination, overwrite, task);
        }
    }

    private void decompressTarGz(Path source, Path destination, boolean overwrite, Task task) throws IOException {
        try (InputStream fileIn = Files.newInputStream(source);
             BufferedInputStream bufferedIn = new BufferedInputStream(fileIn);
             GzipCompressorInputStream gzipIn = new GzipCompressorInputStream(bufferedIn);
             TarArchiveInputStream tarIn = new TarArchiveInputStream(gzipIn)) {

            extractArchive(tarIn, destination, overwrite, task);
        }
    }

    private void extractArchive(ArchiveInputStream archiveIn, Path destination, boolean overwrite, Task task) throws IOException {
        byte[] buffer = new byte[BUFFER_SIZE];
        ArchiveEntry entry;

        while ((entry = archiveIn.getNextEntry()) != null) {
            assertNotCancelled(task);

            Path entryPath = destination.resolve(entry.getName());

            // 安全检查：确保解压路径不会超出目标目录
            if (!entryPath.normalize().startsWith(destination.normalize())) {
                log.warn("跳过不安全的路径: {}", entry.getName());
                continue;
            }

            if (entry.isDirectory()) {
                Files.createDirectories(entryPath);
            } else {
                // 检查父目录是否存在
                Files.createDirectories(entryPath.getParent());

                // 检查是否需要覆盖
                if (Files.exists(entryPath) && !overwrite) {
                    log.info("文件已存在，跳过: {}", entryPath);
                    continue;
                }

                // 解压文件
                try (OutputStream out = Files.newOutputStream(entryPath, StandardOpenOption.CREATE, 
                        StandardOpenOption.TRUNCATE_EXISTING)) {
                    int count;
                    while ((count = archiveIn.read(buffer)) != -1) {
                        assertNotCancelled(task);
                        out.write(buffer, 0, count);
                        processedSize += count;
                        updateProgressIfNeeded(task);
                    }
                }
            }
        }
    }

    private void updateProgressIfNeeded(Task task) {
        int progress = totalSize > 0 ? (int) ((processedSize * 100) / totalSize) : 0;
        updateProgress(task, progress, StrUtil.format("已处理: {}/{} bytes ({:.2f}%)",
                processedSize, totalSize, (double) progress));
    }

    @Override
    protected void onCancel(Task task) {

    }

    @Override
    public String getTaskDesc(Task task) {
        FileDecompressParams params = parseTaskParam(task);
        return StrUtil.format("解压: {} -> {}", params.getSelectPath(), params.getDestinationPath());
    }
} 
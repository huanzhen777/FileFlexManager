package com.huanzhen.fileflexmanager.infrastructure.task.handler;

import cn.hutool.core.util.StrUtil;
import com.huanzhen.fileflexmanager.domain.model.entity.Task;
import com.huanzhen.fileflexmanager.domain.model.enums.TaskType;
import com.huanzhen.fileflexmanager.domain.model.params.params.FileCompressParams;
import com.huanzhen.fileflexmanager.domain.repository.TaskRepository;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.compress.archivers.ArchiveOutputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorOutputStream;
import org.apache.commons.compress.compressors.gzip.GzipParameters;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
@Scope("prototype")
@Component
public class FileCompressTaskHandler extends BaseTaskHandler<FileCompressParams> {
    private final AtomicBoolean isCancelled = new AtomicBoolean(false);
    private long totalSize = 0;
    private long processedSize = 0;
    private static final int BUFFER_SIZE = 8192;

    public FileCompressTaskHandler(TaskRepository taskRepository) {
        super(taskRepository);
    }

    @Override
    public TaskType getTaskType() {
        return TaskType.FILE_COMPRESS;
    }

    @Override
    protected void doHandle(Task task) throws Exception {
        FileCompressParams params = parseTaskParam(task);
        Path sourcePath = Paths.get(params.getSelectPath());
        Path destinationDir = Paths.get(params.getDestinationPath());
        String format = params.getFormat().toLowerCase();
        int level = params.getLevel() != null ? params.getLevel() : 6;

        if (!Files.exists(sourcePath)) {
            throw new IllegalArgumentException("源路径不存在：" + params.getSelectPath());
        }

        // 创建目标目录
        Files.createDirectories(destinationDir);

        // 构建压缩文件名
        String baseName = sourcePath.getFileName().toString();
        Path compressedFile = destinationDir.resolve(baseName + "." + format);

        // 计算总大小
        calculateTotalSize(sourcePath);
        processedSize = 0;

        log.info("开始压缩: {} -> {}", params.getSelectPath(), compressedFile);
        updateProgress(task,0,"开始压缩...");
        taskRepository.updateTask(task);

        // 执行压缩
        switch (format) {
            case "zip":
                compressZip(sourcePath, compressedFile, level, task);
                break;
            case "tar":
                compressTar(sourcePath, compressedFile, task);
                break;
            case "tar.gz":
            case "tgz":
                compressTarGz(sourcePath, compressedFile, level, task);
                break;
            default:
                throw new IllegalArgumentException("不支持的压缩格式: " + format);
        }

        log.info("压缩完成");
        task.markAsCompleted("压缩完成");
    }

    private void calculateTotalSize(Path path) throws IOException {
        AtomicLong size = new AtomicLong(0);
        Files.walkFileTree(path, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                size.addAndGet(attrs.size());
                return FileVisitResult.CONTINUE;
            }
        });
        totalSize = size.get();
    }

    private void compressZip(Path source, Path zipFile, int level, Task task) throws IOException {
        try (ZipArchiveOutputStream zipOut = new ZipArchiveOutputStream(Files.newOutputStream(zipFile))) {
            zipOut.setLevel(level);
            String basePath = source.getFileName().toString();

            Files.walkFileTree(source, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    assertNotCancelled(task);

                    String entryName = basePath + "/" + source.relativize(file).toString();
                    ZipArchiveEntry entry = new ZipArchiveEntry(file.toFile(), entryName);
                    zipOut.putArchiveEntry(entry);

                    processFileToArchive(file, zipOut, task);
                    zipOut.closeArchiveEntry();

                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                    assertNotCancelled(task);

                    if (!dir.equals(source)) {
                        String entryName = basePath + "/" + source.relativize(dir).toString() + "/";
                        ZipArchiveEntry entry = new ZipArchiveEntry(entryName);
                        zipOut.putArchiveEntry(entry);
                        zipOut.closeArchiveEntry();
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
        }
    }

    private void compressTar(Path source, Path tarFile, Task task) throws IOException {
        try (TarArchiveOutputStream tarOut = new TarArchiveOutputStream(Files.newOutputStream(tarFile))) {
            tarOut.setLongFileMode(TarArchiveOutputStream.LONGFILE_GNU);
            String basePath = source.getFileName().toString();

            Files.walkFileTree(source, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    assertNotCancelled(task);

                    String entryName = basePath + "/" + source.relativize(file).toString();
                    TarArchiveEntry entry = new TarArchiveEntry(file.toFile(), entryName);
                    tarOut.putArchiveEntry(entry);

                    processFileToArchive(file, tarOut, task);
                    tarOut.closeArchiveEntry();

                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                    assertNotCancelled(task);

                    if (!dir.equals(source)) {
                        String entryName = basePath + "/" + source.relativize(dir).toString() + "/";
                        TarArchiveEntry entry = new TarArchiveEntry(entryName);
                        tarOut.putArchiveEntry(entry);
                        tarOut.closeArchiveEntry();
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
        }
    }

    private void compressTarGz(Path source, Path tarGzFile, int level, Task task) throws IOException {
        GzipParameters parameters = new GzipParameters();
        parameters.setCompressionLevel(level);

        try (OutputStream fOut = Files.newOutputStream(tarGzFile);
             GzipCompressorOutputStream gzOut = new GzipCompressorOutputStream(fOut, parameters);
             TarArchiveOutputStream tarOut = new TarArchiveOutputStream(gzOut)) {

            tarOut.setLongFileMode(TarArchiveOutputStream.LONGFILE_GNU);
            String basePath = source.getFileName().toString();

            Files.walkFileTree(source, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    assertNotCancelled(task);

                    String entryName = basePath + "/" + source.relativize(file).toString();
                    TarArchiveEntry entry = new TarArchiveEntry(file.toFile(), entryName);
                    tarOut.putArchiveEntry(entry);

                    processFileToArchive(file, tarOut, task);
                    tarOut.closeArchiveEntry();

                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                    assertNotCancelled(task);

                    if (!dir.equals(source)) {
                        String entryName = basePath + "/" + source.relativize(dir).toString() + "/";
                        TarArchiveEntry entry = new TarArchiveEntry(entryName);
                        tarOut.putArchiveEntry(entry);
                        tarOut.closeArchiveEntry();
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
        }
    }

    private void processFileToArchive(Path file, ArchiveOutputStream archiveOut, Task task) throws IOException {
        byte[] buffer = new byte[BUFFER_SIZE];
        try (InputStream input = Files.newInputStream(file)) {
            int count;
            while ((count = input.read(buffer)) != -1) {
                assertNotCancelled(task);
                archiveOut.write(buffer, 0, count);
                processedSize += count;
                updateProgressIfNeeded(task);
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
        FileCompressParams params = parseTaskParam(task);
        return StrUtil.format("压缩: {} -> {} ({})",
                params.getSelectPath(),
                params.getDestinationPath(),
                params.getFormat());
    }
} 
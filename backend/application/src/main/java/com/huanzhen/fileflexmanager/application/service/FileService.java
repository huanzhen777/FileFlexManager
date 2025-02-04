package com.huanzhen.fileflexmanager.application.service;

import cn.hutool.core.date.LocalDateTimeUtil;
import cn.hutool.core.lang.Assert;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.huanzhen.fileflexmanager.application.config.FileSystemConfig;
import com.huanzhen.fileflexmanager.domain.model.Constants;
import com.huanzhen.fileflexmanager.domain.model.entity.*;
import com.huanzhen.fileflexmanager.domain.model.enums.ConfigEnum;
import com.huanzhen.fileflexmanager.domain.repository.FileIndexRepository;
import com.huanzhen.fileflexmanager.domain.repository.FileTagRepository;
import com.huanzhen.fileflexmanager.domain.repository.TagFileHashRepository;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.apache.commons.vfs2.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.PosixFileAttributes;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.nio.file.attribute.UserPrincipal;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FileService implements DisposableBean {
    private static final Logger logger = LoggerFactory.getLogger(FileService.class);

    @NonNull
    private FileSystemConfig fileSystemConfig;
    @NonNull
    private final FileIndexRepository fileIndexRepository;
    private final FileTagRepository fileTagRepository;
    private final TagFileHashRepository tagFileHashRepository;

    private volatile FileSystemManager fsManager;

    @Value("${app.data.path}")
    private String dataPath;


    private synchronized FileSystemManager getFsManager() throws FileSystemException {
        if (fsManager == null) {
            // 设置文件系统编码
            System.setProperty("vfs.local.encoding", "UTF-8");
            fsManager = VFS.getManager();
        }
        return fsManager;
    }

    /**
     * 构建完整的文件信息
     */
    private FileInfo buildFileInfo(FileObject file, FileIndex fileIndex) {
        try {
            if (!file.exists()) {
                return null;
            }

            FileInfo fileInfo = new FileInfo();
            // 设置基本信息
            fileInfo.setId(fileIndex != null ? fileIndex.getId() : null);
            fileInfo.setName(file.getName().getBaseName());
            fileInfo.setPath(file.getName().getPath());
            fileInfo.setDirectory(file.getType() == FileType.FOLDER);
            if (file.isFile()) {
                fileInfo.setSize(file.getContent().getSize());
            } else {
                if (ConfigEnum.FILE_FOLDER_USE_INDEX_SIZE.getBooleanValue()) {
                    FileIndex byPath = fileIndexRepository.findByPath(fileInfo.getPath());
                    if (Objects.nonNull(byPath)) {
                        fileInfo.setSize(byPath.getSize());
                    }
                }
            }
            fileInfo.setLastModified(file.getContent().getLastModifiedTime());


            // 获取文件标签
            if (fileIndex != null) {
                List<Tag> fileTags = tagService.getFileTags(fileIndex.getId());
                fileInfo.setTags(fileTags);
                fileInfo.setHash(fileIndex.getMd5());
            }
            return fileInfo;
        } catch (Exception e) {
            logger.error("构建文件信息失败: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 获取或创建文件索引
     */
    private FileIndex getOrCreateFileIndex(FileInfo fileInfo) {
        FileIndex fileIndex = fileIndexRepository.findByPath(fileInfo.getPath());
        if (fileIndex == null) {
            fileIndex = new FileIndex();
            fileIndex.setPath(fileInfo.getPath());
            fileIndex.setName(fileInfo.getName());
            fileIndex.setIsDir(fileInfo.isDirectory());
            fileIndex.setSize(fileInfo.getSize());
            fileIndex.setLastModified(LocalDateTimeUtil.ofUTC(fileInfo.getLastModified()));
            fileIndex = fileIndexRepository.save(fileIndex);
            logger.debug("创建新的文件索引记录: {}", fileInfo.getPath());
        }
        return fileIndex;
    }

    public Page<FileInfo> listFiles(String path, int pageNum, int pageSize) {
        if (Objects.equals(path, Constants.DEFAULT_PATH_TAG) || Objects.equals("/", path)) {
            path = dataPath;
        }
        try (FileObject folder = getFsManager().resolveFile(path)) {
            if (!folder.exists() || folder.getType() != FileType.FOLDER) {
                return new Page<FileInfo>(pageNum, pageSize).setRecords(new ArrayList<>());
            }

            FileObject[] children = folder.getChildren();
            int total = children.length;
            int start = (Math.max(1, pageNum) - 1) * pageSize;
            int end = Math.min(start + pageSize, total);

            if (start >= total) {
                return new Page<FileInfo>(pageNum, pageSize)
                        .setTotal(total)
                        .setRecords(new ArrayList<>());
            }

            List<FileInfo> pageFiles = new ArrayList<>();
            for (int i = start; i < end; i++) {
                try (FileObject currentFile = children[i]) {
                    FileInfo fileInfo = buildFileInfo(currentFile, null);
                    if (fileInfo != null) {
                        FileIndex fileIndex = getOrCreateFileIndex(fileInfo);
                        fileInfo.setId(fileIndex.getId());
                        List<Tag> fileTags = tagService.getFileTags(fileIndex.getId());
                        fileInfo.setTags(fileTags);
                        pageFiles.add(fileInfo);
                    }
                }
            }

            return new Page<FileInfo>(pageNum, pageSize)
                    .setTotal(total)
                    .setRecords(pageFiles);
        } catch (Exception e) {
            logger.error("读取目录失败: {}", e.getMessage());
            throw new RuntimeException("读取目录失败", e);
        }
    }


    public List<String> getSystemUsers() {
        List<String> users = new ArrayList<>();
        try {
            // 从 /etc/passwd 文件读取用户列表
            try (BufferedReader reader = new BufferedReader(new FileReader("/etc/passwd"))) {
                // 过滤掉系统用户
                users = reader.lines()
                        .map(line -> line.split(":")[0]) // 获取用户名
                        .filter(user -> !user.startsWith("_"))
                        .collect(Collectors.toList());
            }
        } catch (Exception e) {
            logger.error("获取系统用户列表失败: ", e);
        }
        return users;
    }

    public boolean createDirectory(String path) {
        try (FileObject directory = getFsManager().resolveFile(path)) {
            directory.createFolder();
            try {
                Path dirPath = Paths.get(directory.getName().getPath());
                Set<PosixFilePermission> permissions = PosixFilePermissions.fromString(
                        convertModeToString(fileSystemConfig.getDirectoryMode())
                );
                Files.setPosixFilePermissions(dirPath, permissions);
            } catch (Exception e) {
                logger.error("设置文件夹权限失败: ", e);
            }
            return true;
        } catch (Exception e) {
            logger.error("创建文件夹失败: ", e);
            return false;
        }
    }

    /**
     * 将数字模式转换为权限字符串
     * 例如：0755 -> "rwxr-xr-x"
     */
    private String convertModeToString(int mode) {
        StringBuilder result = new StringBuilder();
        // 用户权限
        result.append((mode & 0400) != 0 ? 'r' : '-');
        result.append((mode & 0200) != 0 ? 'w' : '-');
        result.append((mode & 0100) != 0 ? 'x' : '-');
        // 组权限
        result.append((mode & 0040) != 0 ? 'r' : '-');
        result.append((mode & 0020) != 0 ? 'w' : '-');
        result.append((mode & 0010) != 0 ? 'x' : '-');
        // 其他用户权限
        result.append((mode & 0004) != 0 ? 'r' : '-');
        result.append((mode & 0002) != 0 ? 'w' : '-');
        result.append((mode & 0001) != 0 ? 'x' : '-');
        return result.toString();
    }

    public String readFileContent(String path) {
        try (FileObject file = getFsManager().resolveFile(path)) {
            if (!file.exists() || file.getType() != FileType.FILE) {
                throw new RuntimeException("文件不存在或不是普通文件");
            }
            return new String(file.getContent().getByteArray(), StandardCharsets.UTF_8);
        } catch (Exception e) {
            logger.error("读取文件内容失败: ", e);
            throw new RuntimeException("读取文件内容失败", e);
        }
    }

    public boolean saveFileContent(String path, String content) {
        try (FileObject file = getFsManager().resolveFile(path)) {
            if (!file.exists() || file.getType() != FileType.FILE) {
                throw new RuntimeException("文件不存在或不是普通文件");
            }
            Files.writeString(
                    Paths.get(file.getName().getPath()),
                    content,
                    StandardCharsets.UTF_8,
                    StandardOpenOption.TRUNCATE_EXISTING
            );
            return true;
        } catch (Exception e) {
            logger.error("保存文件内容失败: ", e);
            throw new RuntimeException("保存文件内容失败", e);
        }
    }

    public boolean uploadFile(MultipartFile file, String path) {
        try (FileObject targetFile = getFsManager().resolveFile(path)) {
            // 确保父目录存在
            FileObject parent = targetFile.getParent();
            if (parent != null) {
                parent.createFolder();
            }

            // 保存文件
            try (var inputStream = file.getInputStream()) {
                try (FileContent content = targetFile.getContent();
                     OutputStream outputStream = content.getOutputStream()) {
                    byte[] buffer = new byte[8192];
                    int bytesRead;
                    while ((bytesRead = inputStream.read(buffer)) != -1) {
                        outputStream.write(buffer, 0, bytesRead);
                    }
                    outputStream.flush();
                }
            }

            // 设置文件权限
            try {
                Path filePath = Paths.get(targetFile.getName().getPath());
                Set<PosixFilePermission> permissions = PosixFilePermissions.fromString(
                        convertModeToString(fileSystemConfig.getFileMode())
                );
                Files.setPosixFilePermissions(filePath, permissions);
            } catch (Exception e) {
                logger.error("设置文件权限失败", e);
            }

            return true;
        } catch (Exception e) {
            logger.error("上传文件失败", e);
            return false;
        }
    }

    public void downloadFile(String path, OutputStream outputStream) {
        try (FileObject file = getFsManager().resolveFile(path)) {
            if (!file.exists() || file.getType() != FileType.FILE) {
                throw new RuntimeException("文件不存在或不是普通文件");
            }

            try (FileContent content = file.getContent();
                 InputStream inputStream = content.getInputStream()) {
                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                }
                outputStream.flush();
            }
        } catch (Exception e) {
            logger.error("下载文件失败", e);
            throw new RuntimeException("下载文件失败", e);
        }
    }

    public Page<FileInfo> searchFiles(String keyword, int pageNum, int pageSize) {
        try {
            // 调用仓储层的搜索方法
            Page<FileIndex> fileIndexPage = fileIndexRepository.search(keyword, pageNum, pageSize);

            // 将 FileIndex 转换为 FileInfo，并过滤掉不存在的文件
            List<FileInfo> fileInfoList = fileIndexPage.getRecords().stream()
                    .map(this::convertToFileInfo)
                    .filter(fileInfo -> fileInfo != null)  // 过滤掉不存在的文件
                    .collect(Collectors.toList());

            // 创建新的分页对象并返回
            Page<FileInfo> result = new Page<>(fileIndexPage.getCurrent(), fileIndexPage.getSize());
            result.setTotal(fileInfoList.size());  // 更新总数为实际存在的文件数
            result.setRecords(fileInfoList);

            return result;
        } catch (Exception e) {
            logger.error("搜索文件失败: ", e);
            throw new RuntimeException("搜索文件失败: " + e.getMessage(), e);
        }
    }

    // 修改转换方法，当文件不存在时返回 null
    private FileInfo convertToFileInfo(FileIndex fileIndex) {
        try (FileObject file = getFsManager().resolveFile(fileIndex.getPath())) {
            if (!file.exists()) {
                logger.debug("文件不存在，忽略搜索结果: {}", fileIndex.getPath());
                return null;
            }

            FileInfo fileInfo = new FileInfo();
            fileInfo.setName(file.getName().getBaseName());
            fileInfo.setPath(file.getName().getPath());
            fileInfo.setDirectory(file.getType() == FileType.FOLDER);
            if (file.isFile()) {
                fileInfo.setSize(file.getContent().getSize());
            }
            fileInfo.setLastModified(file.getContent().getLastModifiedTime());

            // 获取文件所有者
            try {
                Path filePath = Paths.get(file.getName().getPath());
                PosixFileAttributes attrs = Files.readAttributes(filePath, PosixFileAttributes.class);
                UserPrincipal owner = attrs.owner();
                fileInfo.setOwner(owner != null ? owner.getName() : null);
            } catch (Exception e) {
                logger.debug("获取文件所有者失败: {}", e.getMessage());
                fileInfo.setOwner(null);
            }

            return fileInfo;
        } catch (Exception e) {
            logger.warn("处理文件信息失败: {}, 错误: {}", fileIndex.getPath(), e.getMessage());
            return null;  // 如果出现任何错误，返回 null
        }
    }


    private final TagService tagService;

    public List<Tag> getFileTags(String path) {
        logger.debug("获取文件标签, 文件路径: {}", path);
        FileInfo fileInfo = getFileInfo(path);
        if (fileInfo == null) {
            throw new IllegalArgumentException("文件不存在");
        }
        return tagService.getFileTags(fileInfo.getId());
    }

    public void updateFileTags(String path, List<Long> tagIds) {
        logger.debug("更新文件标签, 文件路径: {}, 标签IDs: {}", path, tagIds);
        FileInfo fileInfo = getFileInfo(path);
        Assert.notNull(fileInfo);
        tagService.updateFileTags(fileInfo, path, tagIds);
    }

    // 获取文件信息的辅助方法
    @SneakyThrows
    private FileInfo getFileInfo(String path) {
        FileObject file = getFsManager().resolveFile(path);
        FileIndex fileIndex = fileIndexRepository.findByPath(path);
        FileInfo fileInfo = buildFileInfo(file, fileIndex);
        Assert.notNull(fileInfo);
        if (fileIndex == null) {
            fileIndex = getOrCreateFileIndex(fileInfo);
            fileInfo.setId(fileIndex.getId());
        }
        return fileInfo;
    }

    public Page<FileInfo> getFilesContainAllTags(List<Long> tagIds, int pageNum, int pageSize) {
        // 1. 获取普通标签关联的文件ID和对应的标签
        Map<Long, Set<Long>> fileTagMap = new HashMap<>();
        fileTagRepository.findByTagIds(tagIds).forEach(fileTag -> {
            fileTagMap.computeIfAbsent(fileTag.getFileId(), k -> new HashSet<>())
                    .add(fileTag.getTagId());
        });

        // 2. 获取绑定文件标签对应的文件hash
        Map<String, Set<Long>> hashTagMap = new HashMap<>();
        tagFileHashRepository.findByTagIds(tagIds).forEach(tagFileHash -> {
            hashTagMap.computeIfAbsent(tagFileHash.getFileHash(), k -> new HashSet<>())
                    .add(tagFileHash.getTagId());
        });

        // 3. 根据hash获取文件，并合并到fileTagMap中
        if (!hashTagMap.isEmpty()) {
            List<FileIndex> hashFiles = fileIndexRepository.findByMd5List(new ArrayList<>(hashTagMap.keySet()));
            hashFiles.forEach(file -> {
                if (file.getMd5() != null && hashTagMap.containsKey(file.getMd5())) {
                    Set<Long> fileTags = fileTagMap.computeIfAbsent(file.getId(), k -> new HashSet<>());
                    fileTags.addAll(hashTagMap.get(file.getMd5()));
                }
            });
        }

        // 4. 过滤出包含所有标签的文件ID
        List<Long> matchedFileIds = fileTagMap.entrySet().stream()
                .filter(entry -> entry.getValue().containsAll(tagIds))
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());

        // 5. 查询最终的分页结果
        if (matchedFileIds.isEmpty()) {
            return new Page<>(pageNum, pageSize);
        }

        Page<FileIndex> fileIndexPage = fileIndexRepository.findByIds(matchedFileIds, pageNum, pageSize);
        return convertToFileInfoPage(fileIndexPage);
    }

    public Page<FileInfo> getFilesContainAnyTags(List<Long> tagIds, int page, int size) {
        // 1. 获取普通标签关联的文件ID
        List<Long> normalFileIds = fileTagRepository.findByTagIds(tagIds).stream()
                .map(FileTag::getFileId)
                .collect(Collectors.toList());

        // 2. 获取绑定文件标签对应的文件hash
        List<String> fileHashes = tagFileHashRepository.findByTagIds(tagIds).stream()
                .map(TagFileHash::getFileHash)
                .collect(Collectors.toList());

        // 3. 根据hash获取文件
        List<FileIndex> hashFiles = fileHashes.isEmpty() ? Collections.emptyList() :
                fileIndexRepository.findByMd5List(fileHashes);

        // 4. 合并文件ID
        Set<Long> allFileIds = new HashSet<>(normalFileIds);
        hashFiles.forEach(file -> allFileIds.add(file.getId()));

        // 5. 查询最终的分页结果
        if (allFileIds.isEmpty()) {
            return new Page<>(page, size);
        }

        Page<FileIndex> fileIndexPage = fileIndexRepository.findByIds(new ArrayList<>(allFileIds), page, size);
        return convertToFileInfoPage(fileIndexPage);
    }

    // 工具方法：转换分页结果
    private Page<FileInfo> convertToFileInfoPage(Page<FileIndex> fileIndexPage) {
        Page<FileInfo> page = new Page<>(fileIndexPage.getCurrent(), fileIndexPage.getSize(), fileIndexPage.getTotal());
        page.setRecords(fileIndexPage.getRecords().stream()
                .map(fileIndex -> {
                    try (FileObject file = getFsManager().resolveFile(fileIndex.getPath())) {
                        return buildFileInfo(file, fileIndex);
                    } catch (Exception e) {
                        logger.error("处理文件信息失败: {}", e.getMessage());
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList()));
        return page;
    }

    @Override
    public void destroy() throws Exception {
        if (fsManager != null) {
            try {
                fsManager.close();
            } catch (Exception e) {
                logger.error("关闭文件系统管理器失败: ", e);
                throw e;
            } finally {
                fsManager = null;
            }
        }
    }
}
package com.huanzhen.fileflexmanager.application.service;

import cn.hutool.crypto.digest.DigestUtil;
import com.huanzhen.fileflexmanager.domain.model.entity.*;
import com.huanzhen.fileflexmanager.domain.repository.FileIndexRepository;
import com.huanzhen.fileflexmanager.domain.repository.FileTagRepository;
import com.huanzhen.fileflexmanager.domain.repository.TagFileHashRepository;
import com.huanzhen.fileflexmanager.domain.repository.TagRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TagService {
    private static final Logger logger = LoggerFactory.getLogger(TagService.class);

    private final TagRepository tagRepository;
    private final FileTagRepository fileTagRepository;
    private final TagFileHashRepository tagFileHashRepository;
    private final FileIndexRepository fileIndexRepository;

    public List<Tag> getAllTags() {
        logger.debug("获取所有标签");
        List<Tag> tags = tagRepository.findAll(1, 1000).getRecords();

        for (Tag tag : tags) {
            // 计算直接关联的文件数量
            int directFileCount = fileTagRepository.findByTagId(tag.getId()).size();

            // 计算通过文件哈希关联的文件数量
            int hashFileCount = fileIndexRepository.findFileByHash(
                            tagFileHashRepository.findByTagId(tag.getId()).stream().map(TagFileHash::getFileHash)
                                    .collect(Collectors.toSet()))
                    .size();

            // 设置标签的文件数量
            tag.setFileCount((long) (directFileCount + hashFileCount));
        }

        return tags;
    }

    @Transactional
    public Tag createTag(Tag tag) {
        logger.debug("创建标签: {}", tag.getName());
        // 校验标签名称
        validateTagName(tag.getName());

        if (tag.getParentId() != null) {
            Tag parent = tagRepository.findById(tag.getParentId());
            if (parent == null) {
                throw new IllegalArgumentException("父标签不存在");
            }
            // 构建父级路径，使用标签名称构建层级
            String parentPath = parent.getPath();
            if (parentPath.isEmpty()) {
                tag.setPath(parent.getName());
            } else {
                tag.setPath(parentPath + "/" + parent.getName());
            }
        } else {
            tag.setPath(""); // 根标签路径为空
        }

        // 检查同级目录下是否存在同名标签
        Tag existingTag = tagRepository.findByNameAndParentId(tag.getName(), tag.getParentId());
        if (existingTag != null) {
            throw new IllegalArgumentException("同级目录下已存在同名标签");
        }

        return tagRepository.save(tag);
    }

    /**
     * 校验标签名称
     *
     * @param name 标签名称
     * @throws IllegalArgumentException 如果标签名称不合法
     */
    private void validateTagName(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("标签名称不能为空");
        }
        if (name.contains("/")) {
            throw new IllegalArgumentException("标签名称不能包含'/'字符");
        }
        // 可以添加其他限制，比如长度限制
        if (name.length() > 50) {
            throw new IllegalArgumentException("标签名称不能超过50个字符");
        }
        // 可以添加其他特殊字符的限制
        if (!name.matches("^[\\w\\u4e00-\\u9fa5\\s-]+$")) {
            throw new IllegalArgumentException("标签名称只能包含字母、数字、中文、空格和连字符");
        }
    }

    @Transactional
    public void deleteTag(Long id) {
        logger.debug("删除标签及其子标签, ID: {}", id);
        // 删除标签及其子标签
        List<Tag> subTags = tagRepository.findByParentId(id);
        for (Tag tag : subTags) {
            fileTagRepository.deleteByTagId(tag.getId());
            tagRepository.deleteById(tag.getId());
        }

        // 删除当前标签
        fileTagRepository.deleteByTagId(id);
        tagRepository.deleteById(id);
    }

    public List<Tag> getFileTags(Long fileId) {
        logger.debug("获取文件标签, 文件ID: {}", fileId);
        // 1. 获取直接关联的标签
        List<FileTag> fileTags = fileTagRepository.findByFileId(fileId);
        List<Tag> directTags = fileTags.stream()
                .map(fileTag -> tagRepository.findById(fileTag.getTagId()))
                .collect(Collectors.toList());

        // 2. 获取文件的MD5
        FileIndex fileIndex = fileIndexRepository.findById(fileId);
        if (fileIndex != null && fileIndex.getMd5() != null) {
            // 3. 通过MD5查找匹配的标签
            List<TagFileHash> tagFileHashes = tagFileHashRepository.findByFileHash(fileIndex.getMd5());
            List<Tag> hashMatchedTags = tagFileHashes.stream()
                    .map(hash -> tagRepository.findById(hash.getTagId()))
                    .collect(Collectors.toList());

            // 4. 合并两种标签并去重
            directTags.addAll(hashMatchedTags);
            return directTags.stream()
                    .distinct()
                    .collect(Collectors.toList());
        }

        return directTags;
    }

    @Transactional
    public void updateFileTags(FileInfo fileInfo, String path, List<Long> tagIds) {
        Long fileId = fileInfo.getId();

        // 获取文件当前的所有标签
        List<FileTag> existingFileTags = fileTagRepository.findByFileId(fileId);
        if (fileInfo.getHash() != null) {
            List<TagFileHash> existingTagFileHashes = tagFileHashRepository.findByFileHash(fileInfo.getHash());
            List<Long> existingTagIds = existingTagFileHashes.stream()
                    .map(TagFileHash::getTagId)
                    .toList();
            // 找出被移除的标签
            List<Long> removedTagIds = existingTagIds.stream()
                    .filter(id -> !tagIds.contains(id))
                    .toList();

            // 处理被移除的标签
            if (!removedTagIds.isEmpty()) {
                FileIndex fileIndex = fileIndexRepository.findById(fileId);
                if (fileIndex != null && fileIndex.getMd5() != null) {
                    String fileHash = fileIndex.getMd5();
                    for (Long tagId : removedTagIds) {
                        Tag tag = tagRepository.findById(tagId);
                        if (tag != null && Boolean.TRUE.equals(tag.getBindFile())) {
                            // 删除hash关联
                            tagFileHashRepository.deleteByTagIdAndFileHash(tagId, fileHash);
                            logger.warn("删除文件hash关联, 标签ID: {}, 文件hash: {}", tagId, fileHash);
                        }
                    }
                }
            }

        }


        // 删除原有标签关联
        int deleted = fileTagRepository.deleteByFileId(fileId);
        logger.warn("删除文件标签关联, 文件ID: {}, 删除数量: {}", fileId, deleted);

        // 添加新的标签关联
        for (Long tagId : tagIds) {
            // 创建文件标签关联
            FileTag fileTag = new FileTag();
            fileTag.setFileId(fileId);
            fileTag.setTagId(tagId);
            fileTagRepository.save(fileTag);
            logger.warn("创建文件标签关联, 文件ID: {}, 标签ID: {}", fileId, tagId);

            // 检查标签是否需要绑定文件hash
            Tag tag = tagRepository.findById(tagId);
            if (tag != null && Boolean.TRUE.equals(tag.getBindFile())) {
                File file = Paths.get(path).toFile();
                if (file.isFile()) {
                    String fileHash = DigestUtil.md5Hex(file);
                    // 检查hash是否已存在
                    TagFileHash existingHash = tagFileHashRepository.findByTagIdAndFileHash(tagId, fileHash);
                    if (existingHash == null) {
                        // 只有当hash不存在时才创建新的关联
                        TagFileHash tagFileHash = new TagFileHash(tagId, fileHash, "MD5");
                        tagFileHashRepository.save(tagFileHash);
                        logger.warn("创建文件hash关联, 标签ID: {}, 文件hash: {}", tagId, fileHash);
                    }
                }
            }
        }
    }

    /**
     * 根据ID获取标签
     *
     * @param id 标签ID
     * @return 标签对象，如果不存在返回null
     */
    public Tag getTagById(Long id) {
        logger.debug("获取标签, ID: {}", id);
        return tagRepository.findById(id);
    }

    /**
     * 更新标签
     *
     * @param tag 要更新的标签
     * @return 更新后的标签
     */
    @Transactional
    public Tag updateTag(Tag tag) {
        logger.debug("更新标签: {}", tag);
        // 获取原标签
        Tag existingTag = tagRepository.findById(tag.getId());
        if (existingTag == null) {
            throw new IllegalArgumentException("标签不存在");
        }
        tag.setId(existingTag.getId());

        // 保存更新
        return tagRepository.update(tag);
    }
}
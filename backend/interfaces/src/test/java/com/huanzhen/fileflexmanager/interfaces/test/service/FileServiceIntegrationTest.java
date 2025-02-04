package com.huanzhen.fileflexmanager.interfaces.test.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.huanzhen.fileflexmanager.application.service.FileService;
import com.huanzhen.fileflexmanager.domain.model.entity.*;
import com.huanzhen.fileflexmanager.domain.repository.FileIndexRepository;
import com.huanzhen.fileflexmanager.domain.repository.FileTagRepository;
import com.huanzhen.fileflexmanager.domain.repository.TagFileHashRepository;
import com.huanzhen.fileflexmanager.domain.repository.TagRepository;
import com.huanzhen.fileflexmanager.interfaces.test.BaseIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.nio.file.Path;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FileServiceIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private FileService fileService;

    @Autowired
    private FileIndexRepository fileIndexRepository;

    @Autowired
    private FileTagRepository fileTagRepository;

    @Autowired
    private TagFileHashRepository tagFileHashRepository;

    @Autowired
    private TagRepository tagRepository;

    private FileIndex file1;
    private FileIndex file2;
    private FileIndex file3;
    private Tag tag1;
    private Tag tag2;
    private Tag tag3;
    private Tag bindFileTag;

    @BeforeEach
    void setUp() throws Exception {
        // 创建测试目录和文件
        Path testPath = createTestDirectory("test");
        
        createTestFile(testPath.resolve("file1.txt"), "content1");
        createTestFile(testPath.resolve("file2.txt"), "content2");
        createTestFile(testPath.resolve("file3.txt"), "content3");

        // 创建文件索引
        file1 = createFileIndex(getTestFilePath("test", "file1.txt"), "hash1");
        file2 = createFileIndex(getTestFilePath("test", "file2.txt"), "hash2");
        file3 = createFileIndex(getTestFilePath("test", "file3.txt"), "hash3");

        // 创建测试标签
        tag1 = createTag("tag1", false);
        tag2 = createTag("tag2", false);
        tag3 = createTag("tag3", false);
        bindFileTag = createTag("bindFileTag", true);

        // 创建文件标签关联
        createFileTag(file1.getId(), tag1.getId());
        createFileTag(file1.getId(), tag2.getId());
        createFileTag(file2.getId(), tag2.getId());
        createFileTag(file3.getId(), tag3.getId());

        // 创建文件hash标签关联
        createTagFileHash(bindFileTag.getId(), "hash1");
        createTagFileHash(bindFileTag.getId(), "hash2");
    }

    private FileIndex createFileIndex(String path, String md5) {
        FileIndex fileIndex = new FileIndex();
        fileIndex.setPath(path);
        fileIndex.setName(path.substring(path.lastIndexOf('/') + 1));
        fileIndex.setIsDir(false);
        fileIndex.setSize(1000L);
        fileIndex.setMd5(md5);
        fileIndex.setLastModified(now());
        return fileIndexRepository.save(fileIndex);
    }

    private Tag createTag(String name, boolean bindFile) {
        Tag tag = new Tag();
        tag.setName(name);
        tag.setBindFile(bindFile);
        tag.setPath("");
        return tagRepository.save(tag);
    }

    private FileTag createFileTag(Long fileId, Long tagId) {
        FileTag fileTag = new FileTag();
        fileTag.setFileId(fileId);
        fileTag.setTagId(tagId);
        return fileTagRepository.save(fileTag);
    }

    private TagFileHash createTagFileHash(Long tagId, String fileHash) {
        TagFileHash tagFileHash = new TagFileHash();
        tagFileHash.setTagId(tagId);
        tagFileHash.setFileHash(fileHash);
        tagFileHash.setHashType("MD5");
        return tagFileHashRepository.save(tagFileHash);
    }

    @Test
    void getFilesContainAllTags_WithNormalTags() {
        // 测试包含所有普通标签的查询
        Page<FileInfo> result = fileService.getFilesContainAllTags(
                Arrays.asList(tag1.getId(), tag2.getId()), 1, 10);

        assertEquals(1, result.getRecords().size());
        assertEquals(file1.getPath(), result.getRecords().get(0).getPath());
    }

    @Test
    void getFilesContainAllTags_WithBindFileTags() {
        // 测试包含绑定文件标签的查询
        Page<FileInfo> result = fileService.getFilesContainAllTags(
                Arrays.asList(bindFileTag.getId()), 1, 10);

        assertEquals(2, result.getRecords().size());
        assertTrue(result.getRecords().stream()
                .anyMatch(file -> file.getPath().equals(file1.getPath())));
        assertTrue(result.getRecords().stream()
                .anyMatch(file -> file.getPath().equals(file2.getPath())));
    }

    @Test
    void getFilesContainAllTags_WithMixedTags() {
        // 测试同时包含普通标签和绑定文件标签的查询
        Page<FileInfo> result = fileService.getFilesContainAllTags(
                Arrays.asList(tag2.getId(), bindFileTag.getId()), 1, 10);

        assertEquals(2, result.getRecords().size());
        assertTrue(result.getRecords().stream()
                .anyMatch(file -> file.getPath().equals(file1.getPath())));
        assertTrue(result.getRecords().stream()
                .anyMatch(file -> file.getPath().equals(file2.getPath())));
    }

    @Test
    void getFilesContainAnyTags_WithNormalTags() {
        // 测试包含任意普通标签的查询
        Page<FileInfo> result = fileService.getFilesContainAnyTags(
                Arrays.asList(tag1.getId(), tag3.getId()), 1, 10);

        assertEquals(2, result.getRecords().size());
        assertTrue(result.getRecords().stream()
                .anyMatch(file -> file.getPath().equals(file1.getPath())));
        assertTrue(result.getRecords().stream()
                .anyMatch(file -> file.getPath().equals(file3.getPath())));
    }

    @Test
    void getFilesContainAnyTags_WithBindFileTags() {
        // 测试包含绑定文件标签的查询
        Page<FileInfo> result = fileService.getFilesContainAnyTags(
                Arrays.asList(bindFileTag.getId()), 1, 10);

        assertEquals(2, result.getRecords().size());
        assertTrue(result.getRecords().stream()
                .anyMatch(file -> file.getPath().equals(file1.getPath())));
        assertTrue(result.getRecords().stream()
                .anyMatch(file -> file.getPath().equals(file2.getPath())));
    }

    @Test
    void getFilesContainAnyTags_WithMixedTags() {
        // 测试同时包含普通标签和绑定文件标签的查询
        Page<FileInfo> result = fileService.getFilesContainAnyTags(
                Arrays.asList(tag3.getId(), bindFileTag.getId()), 1, 10);

        assertEquals(3, result.getRecords().size());
        assertTrue(result.getRecords().stream()
                .anyMatch(file -> file.getPath().equals(file1.getPath())));
        assertTrue(result.getRecords().stream()
                .anyMatch(file -> file.getPath().equals(file2.getPath())));
        assertTrue(result.getRecords().stream()
                .anyMatch(file -> file.getPath().equals(file3.getPath())));
    }
} 
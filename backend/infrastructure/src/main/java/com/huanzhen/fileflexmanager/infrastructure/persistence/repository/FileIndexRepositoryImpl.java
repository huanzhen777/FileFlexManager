package com.huanzhen.fileflexmanager.infrastructure.persistence.repository;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.huanzhen.fileflexmanager.domain.model.entity.FileIndex;
import com.huanzhen.fileflexmanager.domain.repository.FileIndexRepository;
import com.huanzhen.fileflexmanager.infrastructure.persistence.converter.FileIndexConverter;
import com.huanzhen.fileflexmanager.infrastructure.persistence.entity.FileIndexDO;
import com.huanzhen.fileflexmanager.infrastructure.persistence.mapper.FileIndexMapper;
import com.huanzhen.fileflexmanager.domain.repository.FileTagRepository;
import org.apache.commons.compress.utils.Lists;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.time.LocalDateTime;
import java.util.Collections;

@Repository
public class FileIndexRepositoryImpl implements FileIndexRepository {
    private final FileIndexMapper fileIndexMapper;
    private final FileIndexConverter fileIndexConverter;
    private final FileTagRepository fileTagRepository;

    public FileIndexRepositoryImpl(FileIndexMapper fileIndexMapper,
                                   FileIndexConverter fileIndexConverter,
                                   FileTagRepository fileTagRepository) {
        this.fileIndexMapper = fileIndexMapper;
        this.fileIndexConverter = fileIndexConverter;
        this.fileTagRepository = fileTagRepository;
    }

    @Override
    public FileIndex save(FileIndex fileIndex) {
        fileIndex.setUpdateTime(LocalDateTime.now());
        fileIndex.setCreateTime(LocalDateTime.now());
        FileIndexDO fileIndexDO = fileIndexConverter.toFileIndexDO(fileIndex);
        fileIndexMapper.insert(fileIndexDO);
        return fileIndexConverter.toFileIndex(fileIndexDO);
    }

    @Override
    public FileIndex findById(Long id) {
        return fileIndexConverter.toFileIndex(fileIndexMapper.selectById(id));
    }

    @Override
    public FileIndex findByPath(String path) {
        LambdaQueryWrapper<FileIndexDO> query = new LambdaQueryWrapper<>();
        query.eq(FileIndexDO::getPath, path);
        return fileIndexConverter.toFileIndex(fileIndexMapper.selectOne(query));
    }

    @Override
    public List<FileIndex> findByParentPath(String parentPath) {
        LambdaQueryWrapper<FileIndexDO> query = new LambdaQueryWrapper<>();
        query.eq(FileIndexDO::getParentPath, parentPath);
        return fileIndexMapper.selectList(query).stream()
                .map(fileIndexConverter::toFileIndex)
                .collect(Collectors.toList());
    }

    @Override
    public void updateFileIndex(FileIndex fileIndex) {
        fileIndex.setUpdateTime(LocalDateTime.now());
        fileIndexMapper.updateById(fileIndexConverter.toFileIndexDO(fileIndex));
    }

    @Override
    public void deleteByPath(String path) {
        LambdaQueryWrapper<FileIndexDO> query = new LambdaQueryWrapper<>();
        query.eq(FileIndexDO::getPath, path);
        fileIndexMapper.delete(query);
    }

    @Override
    public void deleteById(Long id) {
        fileIndexMapper.deleteById(id);
    }

    @Override
    public Page<FileIndex> findAll(int page, int size) {
        Page<FileIndexDO> pageParam = new Page<>(page, size);
        Page<FileIndexDO> result = fileIndexMapper.selectPage(pageParam, null);
        return convertToFileIndexPage(result);
    }

    @Override
    public List<FileIndex> findByMd5(String md5) {
        LambdaQueryWrapper<FileIndexDO> query = new LambdaQueryWrapper<>();
        query.eq(FileIndexDO::getMd5, md5);
        return fileIndexMapper.selectList(query).stream()
                .map(fileIndexConverter::toFileIndex)
                .collect(Collectors.toList());
    }

    @Override
    public List<FileIndex> findByMd5List(List<String> md5List) {
        if (md5List.isEmpty()) {
            return Collections.emptyList();
        }
        LambdaQueryWrapper<FileIndexDO> query = new LambdaQueryWrapper<>();
        query.in(FileIndexDO::getMd5, md5List);
        return fileIndexMapper.selectList(query).stream()
                .map(fileIndexConverter::toFileIndex)
                .collect(Collectors.toList());
    }

    @Override
    public Page<FileIndex> search(String keyword, int page, int size) {
        Page<FileIndexDO> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<FileIndexDO> query = new LambdaQueryWrapper<>();
        query.like(FileIndexDO::getName, keyword)
                .or()
                .like(FileIndexDO::getPath, keyword);

        Page<FileIndexDO> result = fileIndexMapper.selectPage(pageParam, query);
        return convertToFileIndexPage(result);
    }

    @Override
    public Page<FileIndex> findByIds(List<Long> ids, int page, int size) {
        if (ids.isEmpty()) {
            return new Page<>(page, size);
        }
        Page<FileIndexDO> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<FileIndexDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(FileIndexDO::getId, ids);
        
        Page<FileIndexDO> result = fileIndexMapper.selectPage(pageParam, wrapper);
        return convertToFileIndexPage(result);
    }

    @Override
    public int deleteStaleIndexes(String rootPath, LocalDateTime beforeTime) {
        deleteByPath(rootPath);
        LambdaQueryWrapper<FileIndexDO> query = new LambdaQueryWrapper<>();
        String path = rootPath.endsWith("/") ? rootPath : rootPath + "/";

        // 构建查询条件
        query.likeRight(FileIndexDO::getPath, path)
                .lt(FileIndexDO::getUpdateTime, beforeTime);

        // 先查询要删除的文件ID列表
        List<Long> fileIds = fileIndexMapper.selectList(query)
                .stream()
                .map(FileIndexDO::getId)
                .collect(Collectors.toList());

        if (!fileIds.isEmpty()) {
            // 删除文件标签关联
            fileTagRepository.deleteByFileIds(fileIds);
            // 删除文件索引
            return fileIndexMapper.delete(query);
        }

        return 0;
    }

    @Override
    public List<FileIndex> findFileByHash(Set<String> hashes) {
        if (CollUtil.isEmpty(hashes)) {
            return Lists.newArrayList();
        }
        LambdaQueryWrapper<FileIndexDO> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.in(FileIndexDO::getMd5, hashes);
        return fileIndexConverter.toFileIndexes(fileIndexMapper.selectList(queryWrapper));
    }

    // 工具方法：转换分页结果
    private Page<FileIndex> convertToFileIndexPage(Page<FileIndexDO> result) {
        Page<FileIndex> fileIndexPage = new Page<>(result.getCurrent(), result.getSize(), result.getTotal());
        fileIndexPage.setRecords(result.getRecords().stream()
                .map(fileIndexConverter::toFileIndex)
                .collect(Collectors.toList()));
        return fileIndexPage;
    }
} 
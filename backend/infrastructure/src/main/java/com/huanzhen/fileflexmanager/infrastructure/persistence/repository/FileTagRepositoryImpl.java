package com.huanzhen.fileflexmanager.infrastructure.persistence.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.huanzhen.fileflexmanager.domain.model.entity.FileTag;
import com.huanzhen.fileflexmanager.domain.repository.FileTagRepository;
import com.huanzhen.fileflexmanager.infrastructure.persistence.converter.FileTagConverter;
import com.huanzhen.fileflexmanager.infrastructure.persistence.entity.FileTagDO;
import com.huanzhen.fileflexmanager.infrastructure.persistence.mapper.FileTagMapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.stream.Collectors;
import java.util.Collections;

@Repository
public class FileTagRepositoryImpl implements FileTagRepository {
    private final FileTagMapper fileTagMapper;
    private final FileTagConverter fileTagConverter;

    public FileTagRepositoryImpl(FileTagMapper fileTagMapper, FileTagConverter fileTagConverter) {
        this.fileTagMapper = fileTagMapper;
        this.fileTagConverter = fileTagConverter;
    }

    @Override
    public FileTag save(FileTag fileTag) {
        FileTagDO fileTagDO = fileTagConverter.toFileTagDO(fileTag);
        fileTagMapper.insert(fileTagDO);
        return fileTagConverter.toFileTag(fileTagDO);
    }

    @Override
    public List<FileTag> findByFileId(Long fileId) {
        LambdaQueryWrapper<FileTagDO> query = new LambdaQueryWrapper<>();
        query.eq(FileTagDO::getFileId, fileId);
        return fileTagMapper.selectList(query).stream()
                .map(fileTagConverter::toFileTag)
                .collect(Collectors.toList());
    }

    @Override
    public List<FileTag> findByTagId(Long tagId) {
        LambdaQueryWrapper<FileTagDO> query = new LambdaQueryWrapper<>();
        query.eq(FileTagDO::getTagId, tagId);
        return fileTagMapper.selectList(query).stream()
                .map(fileTagConverter::toFileTag)
                .collect(Collectors.toList());
    }

    @Override
    public int deleteByFileId(Long fileId) {
        LambdaQueryWrapper<FileTagDO> query = new LambdaQueryWrapper<>();
        query.eq(FileTagDO::getFileId, fileId);
       return fileTagMapper.delete(query);
    }

    @Override
    public void deleteByTagId(Long tagId) {
        LambdaQueryWrapper<FileTagDO> query = new LambdaQueryWrapper<>();
        query.eq(FileTagDO::getTagId, tagId);
        fileTagMapper.delete(query);
    }

    @Override
    public void deleteByFileIdAndTagId(Long fileId, Long tagId) {
        LambdaQueryWrapper<FileTagDO> query = new LambdaQueryWrapper<>();
        query.eq(FileTagDO::getFileId, fileId)
             .eq(FileTagDO::getTagId, tagId);
        fileTagMapper.delete(query);
    }

    @Override
    public void deleteByFileIds(List<Long> fileIds) {
        if (!fileIds.isEmpty()) {
            LambdaQueryWrapper<FileTagDO> query = new LambdaQueryWrapper<>();
            query.in(FileTagDO::getFileId, fileIds);
            fileTagMapper.delete(query);
        }
    }

    @Override
    public List<FileTag> findByTagIds(List<Long> tagIds) {
        if (tagIds.isEmpty()) {
            return Collections.emptyList();
        }
        LambdaQueryWrapper<FileTagDO> query = new LambdaQueryWrapper<>();
        query.in(FileTagDO::getTagId, tagIds);
        return fileTagMapper.selectList(query).stream()
                .map(fileTagConverter::toFileTag)
                .collect(Collectors.toList());
    }
} 
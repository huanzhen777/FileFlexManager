package com.huanzhen.fileflexmanager.infrastructure.persistence.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.huanzhen.fileflexmanager.domain.model.entity.TagFileHash;
import com.huanzhen.fileflexmanager.domain.repository.TagFileHashRepository;
import com.huanzhen.fileflexmanager.infrastructure.persistence.converter.TagFileHashConverter;
import com.huanzhen.fileflexmanager.infrastructure.persistence.entity.TagFileHashDO;
import com.huanzhen.fileflexmanager.infrastructure.persistence.mapper.TagFileHashMapper;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Repository
public class TagFileHashRepositoryImpl implements TagFileHashRepository {
    private final TagFileHashMapper tagFileHashMapper;
    private final TagFileHashConverter tagFileHashConverter;

    public TagFileHashRepositoryImpl(TagFileHashMapper tagFileHashMapper, TagFileHashConverter tagFileHashConverter) {
        this.tagFileHashMapper = tagFileHashMapper;
        this.tagFileHashConverter = tagFileHashConverter;
    }

    @Override
    public TagFileHash save(TagFileHash tagFileHash) {
        TagFileHashDO tagFileHashDO = tagFileHashConverter.toTagFileHashDO(tagFileHash);
        tagFileHashMapper.insert(tagFileHashDO);
        return tagFileHashConverter.toTagFileHash(tagFileHashDO);
    }

    @Override
    public List<TagFileHash> findByTagId(Long tagId) {
        LambdaQueryWrapper<TagFileHashDO> query = new LambdaQueryWrapper<>();
        query.eq(TagFileHashDO::getTagId, tagId);
        return tagFileHashMapper.selectList(query).stream()
                .map(tagFileHashConverter::toTagFileHash)
                .collect(Collectors.toList());
    }

    @Override
    public List<TagFileHash> findByFileHash(String fileHash) {
        LambdaQueryWrapper<TagFileHashDO> query = new LambdaQueryWrapper<>();
        query.eq(TagFileHashDO::getFileHash, fileHash);
        return tagFileHashMapper.selectList(query).stream()
                .map(tagFileHashConverter::toTagFileHash)
                .collect(Collectors.toList());
    }

    @Override
    public void deleteByTagId(Long tagId) {
        LambdaQueryWrapper<TagFileHashDO> query = new LambdaQueryWrapper<>();
        query.eq(TagFileHashDO::getTagId, tagId);
        tagFileHashMapper.delete(query);
    }

    @Override
    public int deleteByFileHash(String fileHash) {
        LambdaQueryWrapper<TagFileHashDO> query = new LambdaQueryWrapper<>();
        query.eq(TagFileHashDO::getFileHash, fileHash);
        return tagFileHashMapper.delete(query);
    }

    @Override
    public void deleteByTagIdAndFileHash(Long tagId, String fileHash) {
        LambdaQueryWrapper<TagFileHashDO> query = new LambdaQueryWrapper<>();
        query.eq(TagFileHashDO::getTagId, tagId)
                .eq(TagFileHashDO::getFileHash, fileHash);
        tagFileHashMapper.delete(query);
    }

    @Override
    @Transactional
    public List<TagFileHash> saveAll(List<TagFileHash> tagFileHashes) {
        return tagFileHashes.stream()
                .map(this::save)
                .collect(Collectors.toList());
    }

    @Override
    public List<TagFileHash> findByTagIds(List<Long> tagIds) {
        if (tagIds.isEmpty()) {
            return Collections.emptyList();
        }
        LambdaQueryWrapper<TagFileHashDO> query = new LambdaQueryWrapper<>();
        query.in(TagFileHashDO::getTagId, tagIds);
        return tagFileHashMapper.selectList(query).stream()
                .map(tagFileHashConverter::toTagFileHash)
                .collect(Collectors.toList());
    }

    @Override
    public TagFileHash findByTagIdAndFileHash(Long tagId, String fileHash) {
        LambdaQueryWrapper<TagFileHashDO> query = new LambdaQueryWrapper<>();
        query.eq(TagFileHashDO::getTagId, tagId)
             .eq(TagFileHashDO::getFileHash, fileHash);
        TagFileHashDO tagFileHashDO = tagFileHashMapper.selectOne(query);
        return tagFileHashDO != null ? tagFileHashConverter.toTagFileHash(tagFileHashDO) : null;
    }
} 
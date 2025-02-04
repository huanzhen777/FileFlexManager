package com.huanzhen.fileflexmanager.infrastructure.persistence.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.huanzhen.fileflexmanager.domain.model.entity.Tag;
import com.huanzhen.fileflexmanager.domain.repository.TagRepository;
import com.huanzhen.fileflexmanager.infrastructure.persistence.converter.TagConverter;
import com.huanzhen.fileflexmanager.infrastructure.persistence.entity.TagDO;
import com.huanzhen.fileflexmanager.infrastructure.persistence.mapper.TagMapper;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Repository
public class TagRepositoryImpl implements TagRepository {
    private final TagMapper tagMapper;
    private final TagConverter tagConverter;

    public TagRepositoryImpl(TagMapper tagMapper, TagConverter tagConverter) {
        this.tagMapper = tagMapper;
        this.tagConverter = tagConverter;
    }

    @Override
    public Tag save(Tag tag) {
        TagDO tagDO = tagConverter.toTagDO(tag);
        tagMapper.insert(tagDO);
        return tagConverter.toTag(tagDO);
    }

    @Override
    public Tag findById(Long id) {
        return tagConverter.toTag(tagMapper.selectById(id));
    }

    @Override
    public List<Tag> findByParentId(Long parentId) {
        // 首先查询直接父节点的标签信息，获取其path
        LambdaQueryWrapper<TagDO> parentQuery = new LambdaQueryWrapper<>();
        parentQuery.eq(TagDO::getId, parentId);
        TagDO parentTag = tagMapper.selectOne(parentQuery);
        
        if (parentTag == null) {
            return new ArrayList<>();
        }

        // 使用 LIKE 查询获取所有子节点
        LambdaQueryWrapper<TagDO> query = new LambdaQueryWrapper<>();
        query.likeRight(TagDO::getPath, parentTag.getPath() + "/")
             .or()
             .eq(TagDO::getParentId, parentId); // 包含直接子节点

        return tagMapper.selectList(query).stream()
                .map(tagConverter::toTag)
                .collect(Collectors.toList());
    }

    @Override
    public void updateTag(Tag tag) {
        tagMapper.updateById(tagConverter.toTagDO(tag));
    }

    @Override
    public void deleteById(Long id) {
        tagMapper.deleteById(id);
    }

    @Override
    public Page<Tag> findAll(int page, int size) {
        Page<TagDO> pageParam = new Page<>(page, size);
        Page<TagDO> result = tagMapper.selectPage(pageParam, null);
        
        Page<Tag> tagPage = new Page<>(result.getCurrent(), result.getSize(), result.getTotal());
        tagPage.setRecords(result.getRecords().stream()
                .map(tagConverter::toTag)
                .collect(Collectors.toList()));
        return tagPage;
    }

    @Override
    public Tag findByNameAndParentId(String name, Long parentId) {
        LambdaQueryWrapper<TagDO> query = new LambdaQueryWrapper<>();
        query.eq(TagDO::getName, name)
             .eq(TagDO::getParentId, parentId);
        return tagConverter.toTag(tagMapper.selectOne(query));
    }

    @Override
    public Tag update(Tag tag) {
        TagDO tagDO = tagConverter.toTagDO(tag);
        tagMapper.updateById(tagDO);
        return tagConverter.toTag(tagDO);
    }
} 
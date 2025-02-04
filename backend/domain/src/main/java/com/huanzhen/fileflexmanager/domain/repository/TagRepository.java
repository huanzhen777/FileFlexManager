package com.huanzhen.fileflexmanager.domain.repository;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.huanzhen.fileflexmanager.domain.model.entity.Tag;
import java.util.List;

public interface TagRepository {
    Tag save(Tag tag);
    Tag findById(Long id);
    List<Tag> findByParentId(Long parentId);
    void updateTag(Tag tag);
    void deleteById(Long id);
    Page<Tag> findAll(int page, int size);
    Tag findByNameAndParentId(String name, Long parentId);
    /**
     * 更新标签
     * @param tag 要更新的标签
     * @return 更新后的标签
     */
    Tag update(Tag tag);
} 
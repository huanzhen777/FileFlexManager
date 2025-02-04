package com.huanzhen.fileflexmanager.infrastructure.persistence.converter;

import com.huanzhen.fileflexmanager.domain.model.entity.Tag;
import com.huanzhen.fileflexmanager.infrastructure.persistence.entity.TagDO;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring", uses = TimeConverter.class)
public interface TagConverter {
    TagDO toTagDO(Tag tag);
    Tag toTag(TagDO tagDO);
} 
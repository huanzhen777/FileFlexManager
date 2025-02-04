package com.huanzhen.fileflexmanager.infrastructure.persistence.converter;

import com.huanzhen.fileflexmanager.domain.model.entity.TagFileHash;
import com.huanzhen.fileflexmanager.infrastructure.persistence.entity.TagFileHashDO;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring", uses = TimeConverter.class)
public interface TagFileHashConverter {
    TagFileHashDO toTagFileHashDO(TagFileHash tagFileHash);
    TagFileHash toTagFileHash(TagFileHashDO tagFileHashDO);
} 
package com.huanzhen.fileflexmanager.infrastructure.persistence.converter;

import com.huanzhen.fileflexmanager.domain.model.entity.FileTag;
import com.huanzhen.fileflexmanager.infrastructure.persistence.entity.FileTagDO;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring", uses = TimeConverter.class)
public interface FileTagConverter {
    FileTagDO toFileTagDO(FileTag fileTag);
    FileTag toFileTag(FileTagDO fileTagDO);
} 
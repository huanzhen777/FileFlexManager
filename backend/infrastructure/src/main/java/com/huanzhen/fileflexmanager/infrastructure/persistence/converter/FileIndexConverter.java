package com.huanzhen.fileflexmanager.infrastructure.persistence.converter;

import com.huanzhen.fileflexmanager.domain.model.entity.FileIndex;
import com.huanzhen.fileflexmanager.infrastructure.persistence.entity.FileIndexDO;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

import java.util.List;

@Mapper(componentModel = "spring", uses = TimeConverter.class)
public interface FileIndexConverter {
    FileIndexDO toFileIndexDO(FileIndex fileIndex);
    FileIndex toFileIndex(FileIndexDO fileIndexDO);
    List<FileIndex> toFileIndexes(List<FileIndexDO> fileIndexDO);

    void updateFileIndexDO(@MappingTarget FileIndexDO target, FileIndex source);
    void updateFileIndex(@MappingTarget FileIndex target, FileIndexDO source);
} 
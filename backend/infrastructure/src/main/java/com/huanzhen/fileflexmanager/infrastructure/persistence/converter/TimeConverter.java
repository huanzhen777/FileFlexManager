package com.huanzhen.fileflexmanager.infrastructure.persistence.converter;

import org.mapstruct.Mapper;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

@Mapper(componentModel = "spring")
public interface TimeConverter {
    
    default Long toTimestamp(LocalDateTime time) {
        return time == null ? null : time.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
    }

    default LocalDateTime toLocalDateTime(Long timestamp) {
        return timestamp == null ? null : LocalDateTime.ofInstant(Instant.ofEpochMilli(timestamp), ZoneId.systemDefault());
    }
} 
package com.huanzhen.fileflexmanager.infrastructure.persistence.converter;

import org.mapstruct.Mapper;

import com.huanzhen.fileflexmanager.domain.model.entity.Config;
import com.huanzhen.fileflexmanager.infrastructure.persistence.entity.ConfigDO;

@Mapper(componentModel = "spring", uses = TimeConverter.class)
public interface ConfigDOConvert {

    ConfigDO toDO(Config config);

    Config toDomain(ConfigDO configDO);

}

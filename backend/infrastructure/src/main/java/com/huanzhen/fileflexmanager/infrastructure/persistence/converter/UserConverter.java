package com.huanzhen.fileflexmanager.infrastructure.persistence.converter;

import com.huanzhen.fileflexmanager.domain.model.entity.User;
import com.huanzhen.fileflexmanager.infrastructure.persistence.entity.UserDO;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper(componentModel = "spring")
public interface UserConverter {
    
    UserConverter INSTANCE = Mappers.getMapper(UserConverter.class);

    User toDomain(UserDO userDO);

    UserDO toDO(User user);
} 
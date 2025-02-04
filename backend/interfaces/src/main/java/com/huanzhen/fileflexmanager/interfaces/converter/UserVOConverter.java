package com.huanzhen.fileflexmanager.interfaces.converter;

import com.huanzhen.fileflexmanager.domain.model.entity.User;
import com.huanzhen.fileflexmanager.interfaces.model.vo.UserVO;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper(componentModel = "spring")
public interface UserVOConverter {
    
    UserVOConverter INSTANCE = Mappers.getMapper(UserVOConverter.class);

    UserVO toVO(User user);
} 
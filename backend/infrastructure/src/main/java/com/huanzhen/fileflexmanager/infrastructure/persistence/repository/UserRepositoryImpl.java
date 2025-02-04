package com.huanzhen.fileflexmanager.infrastructure.persistence.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.huanzhen.fileflexmanager.domain.model.entity.User;
import com.huanzhen.fileflexmanager.domain.repository.UserRepository;
import com.huanzhen.fileflexmanager.infrastructure.persistence.converter.UserConverter;
import com.huanzhen.fileflexmanager.infrastructure.persistence.entity.UserDO;
import com.huanzhen.fileflexmanager.infrastructure.persistence.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class UserRepositoryImpl implements UserRepository {

    private final UserMapper userMapper;
    private final UserConverter userConverter;

    @Override
    public User findByUsername(String username) {
        UserDO userDO = userMapper.selectOne(
                new LambdaQueryWrapper<UserDO>()
                        .eq(UserDO::getUsername, username)
        );
        return userConverter.toDomain(userDO);
    }

    @Override
    public boolean exists() {
        return userMapper.exists(new LambdaQueryWrapper<>());
    }

    @Override
    public void save(User admin) {
        userMapper.insert(userConverter.toDO(admin));
    }
} 
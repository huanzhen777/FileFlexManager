package com.huanzhen.fileflexmanager.domain.repository;

import com.huanzhen.fileflexmanager.domain.model.entity.User;

public interface UserRepository {
    User findByUsername(String username);

    boolean exists();

    void save(User admin);
}
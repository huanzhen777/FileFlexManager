package com.huanzhen.fileflexmanager.application.service;

import com.huanzhen.fileflexmanager.domain.model.entity.User;
import com.huanzhen.fileflexmanager.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {
    
    private final UserRepository userRepository;

    public User getUserByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    public boolean hasAnyUser() {
        return userRepository.exists();
    }

    public User createAdmin(String username, String password) {
        User existingUser = getUserByUsername(username);
        if (existingUser != null) {
            throw new RuntimeException("用户名已存在");
        }

        User admin = new User(null, username, password, "ADMIN", true);
        userRepository.save(admin);
        return admin;
    }
} 
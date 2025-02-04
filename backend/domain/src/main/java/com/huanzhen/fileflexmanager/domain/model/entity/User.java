package com.huanzhen.fileflexmanager.domain.model.entity;

import lombok.Getter;

@Getter
public class User {
    private final Long id;
    private final String username;
    private final String password;
    private String role;
    private boolean enabled;

    public User(Long id, String username, String password, String role, boolean enabled) {
        this.id = id;
        this.username = username;
        this.password = password;
        this.role = role;
        this.enabled = enabled;
    }

    // 领域行为
    public void changeRole(String newRole) {
        this.role = newRole;
    }

    public void disable() {
        this.enabled = false;
    }

    public void enable() {
        this.enabled = true;
    }
} 
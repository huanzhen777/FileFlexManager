package com.huanzhen.fileflexmanager.interfaces.model.vo;

import lombok.Data;

@Data
public class UserVO {
    private Long id;
    private String username;
    private String role;
    private boolean enabled;
} 
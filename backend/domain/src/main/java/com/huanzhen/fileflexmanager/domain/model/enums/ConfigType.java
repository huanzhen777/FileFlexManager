package com.huanzhen.fileflexmanager.domain.model.enums;

import lombok.Getter;

/**
 * 配置类型枚举
 */
@Getter
public enum ConfigType {
    SECURITY("安全设置"), // 安全相关配置
    SYSTEM("系统设置"),   // 系统相关配置
    USER("用户设置"),
    FILE("文件设置"),

    ;     // 用户相关配置

    private final String desc;

    ConfigType(String desc) {
        this.desc = desc;
    }
} 
package com.huanzhen.fileflexmanager.domain.model.enums;

public enum TaskStatus {
    PENDING("待处理"),
    RUNNING("处理中"),
    COMPLETED("已完成"),
    FAILED("失败"),
    CANCELLED("已取消");

    private final String description;

    TaskStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    public boolean isTerminal() {
        return this == FAILED || this == CANCELLED || this == COMPLETED;
    }

    public boolean canCancel() {
        return this == PENDING || this == RUNNING;
    }
} 
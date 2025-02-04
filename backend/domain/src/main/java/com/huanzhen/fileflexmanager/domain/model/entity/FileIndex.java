package com.huanzhen.fileflexmanager.domain.model.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
public class FileIndex {
    private Long id;
    private String path;
    private String name;
    private Long size;
    private String mimeType;
    private String md5;
    private Boolean isDir;
    private String parentPath;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private LocalDateTime lastModified;
    private Boolean hidden;
    private String permissions;

    public FileIndex(String path, String name, Boolean isDir) {
        this.path = path;
        this.name = name;
        this.isDir = isDir;
        this.createTime = LocalDateTime.now();
        this.updateTime = this.createTime;
        this.hidden = false;
    }

    public void updateFileInfo(Long size, String mimeType, String md5) {
        this.size = size;
        this.mimeType = mimeType;
        this.md5 = md5;
        this.updateTime = LocalDateTime.now();
    }

    public void updateLastModified(LocalDateTime lastModified) {
        this.lastModified = lastModified;
        this.updateTime = LocalDateTime.now();
    }
} 
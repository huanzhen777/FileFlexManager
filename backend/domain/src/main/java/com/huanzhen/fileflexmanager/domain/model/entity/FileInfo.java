package com.huanzhen.fileflexmanager.domain.model.entity;

import lombok.Data;

import java.util.List;

@Data
public class FileInfo {
    private Long id;
    private String name;
    private String path;
    private boolean isDirectory;
    private Long size;
    private Long lastModified;
    private String owner;
    private List<Tag> tags;
    private String hash;
}
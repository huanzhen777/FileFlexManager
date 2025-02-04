package com.huanzhen.fileflexmanager.domain.model.vo;

import lombok.Data;

@Data
public class TagVO {
    private Long id;
    private String name;
    private Long parentId;
    private String path;
    private Boolean quickAccess;
    private Boolean bindFile;
    private Long fileCount;

} 
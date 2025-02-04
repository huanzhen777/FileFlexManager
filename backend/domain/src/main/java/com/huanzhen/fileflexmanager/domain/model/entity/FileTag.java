package com.huanzhen.fileflexmanager.domain.model.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
public class FileTag {
    private Long id;
    private Long fileId;
    private Long tagId;
    private LocalDateTime createTime;

    public FileTag(Long fileId, Long tagId) {
        this.fileId = fileId;
        this.tagId = tagId;
        this.createTime = LocalDateTime.now();
    }
} 
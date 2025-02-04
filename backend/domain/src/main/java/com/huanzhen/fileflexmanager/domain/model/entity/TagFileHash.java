package com.huanzhen.fileflexmanager.domain.model.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
public class TagFileHash {
    private Long id;
    private Long tagId;
    private String fileHash;
    private String hashType;
    private LocalDateTime createTime;

    public TagFileHash(Long tagId, String fileHash, String hashType) {
        this.tagId = tagId;
        this.fileHash = fileHash;
        this.hashType = hashType;
        this.createTime = LocalDateTime.now();
    }
} 
package com.huanzhen.fileflexmanager.infrastructure.persistence.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("file_index")
public class FileIndexDO {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String path;
    private String name;
    private Long size;
    @TableField("mime_type")
    private String mimeType;
    private String md5;
    @TableField("is_dir")
    private Boolean isDir;
    @TableField("parent_path")
    private String parentPath;
    @TableField("create_time")
    private LocalDateTime createTime;
    @TableField("update_time")
    private LocalDateTime updateTime;
    @TableField("last_modified")
    private LocalDateTime lastModified;
    private Boolean hidden;
    private String permissions;
} 
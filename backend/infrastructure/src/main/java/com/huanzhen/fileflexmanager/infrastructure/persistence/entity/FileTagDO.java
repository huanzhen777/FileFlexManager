package com.huanzhen.fileflexmanager.infrastructure.persistence.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("file_tag")
public class FileTagDO {
    @TableId(type = IdType.AUTO)
    private Long id;
    @TableField("file_id")
    private Long fileId;
    @TableField("tag_id")
    private Long tagId;
    @TableField("create_time")
    private LocalDateTime createTime;
} 
package com.huanzhen.fileflexmanager.infrastructure.persistence.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("tag_file_hash")
public class TagFileHashDO {
    @TableId(type = IdType.AUTO)
    private Long id;
    
    @TableField("tag_id")
    private Long tagId;
    
    @TableField("file_hash")
    private String fileHash;

    @TableField("create_time")
    private LocalDateTime createTime;
} 
package com.huanzhen.fileflexmanager.infrastructure.persistence.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("tag")
public class TagDO {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;
    @TableField("parent_id")
    private Long parentId;
    private String path;
    @TableField("create_time")
    private LocalDateTime createTime;
    @TableField("update_time")
    private LocalDateTime updateTime;
    @TableField("quick_access")
    private Boolean quickAccess;
    @TableField("bind_file")
    private Boolean bindFile;
} 
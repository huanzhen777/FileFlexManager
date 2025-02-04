package com.huanzhen.fileflexmanager.infrastructure.persistence.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("t_config")
public class ConfigDO {
    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;

    @TableField("`value`")
    private String value;

    private String description;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}

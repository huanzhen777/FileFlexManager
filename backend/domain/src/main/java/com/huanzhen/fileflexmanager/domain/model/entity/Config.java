package com.huanzhen.fileflexmanager.domain.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
public class Config {
    private Long id;
    
    private String name;
    
    private String value;
    
    private String description;
    
    private Long createTime;
    
    private Long updateTime;
} 
package com.huanzhen.fileflexmanager.interfaces.model.dto;

import com.huanzhen.fileflexmanager.domain.model.config.ParamConfig;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ConfigDTO {
    private String name;
    private Object value;
    private String description;
    private String title;
    private String type;
    private String typeDesc;
    private ParamConfig paramConfig;
} 
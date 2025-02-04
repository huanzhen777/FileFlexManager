package com.huanzhen.fileflexmanager.domain.model.config;

import com.huanzhen.fileflexmanager.domain.model.params.ParamType;
import lombok.Builder;
import lombok.Value;
import java.util.List;

@Value
@Builder
public class ParamConfig {
    String name;
    String key;
    ParamType type;
    boolean required;
    String description;
    Object defaultValue;
    List<Option> options;
    List<ParamConfig> paramConfigs;

    @Value
    public static class Option {
        String label;
        String value;
    }
} 
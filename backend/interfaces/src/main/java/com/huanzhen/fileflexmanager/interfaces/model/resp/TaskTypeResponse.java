package com.huanzhen.fileflexmanager.interfaces.model.resp;

import com.huanzhen.fileflexmanager.domain.model.config.ParamConfig;
import lombok.Data;
import java.util.List;

@Data
public class TaskTypeResponse {
    private String type;
    private String description;
    private boolean supportScheduled;
    private List<ParamConfig> paramConfigs;
    private final boolean manuallyAdd;
} 
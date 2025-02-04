package com.huanzhen.fileflexmanager.interfaces.model.vo;

import com.huanzhen.fileflexmanager.domain.model.config.ParamConfig;
import com.huanzhen.fileflexmanager.domain.model.enums.FileOperationType;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class FileOperationVO {
    private FileOperationType type;
    private String description;
    private boolean supportFile;
    private boolean supportDirectory;
    private List<String> supportedExtensions;
    private boolean isTask;
    private List<ParamConfig> paramConfigs;
    private boolean isSync;
    private boolean supportMultiSelect;
} 
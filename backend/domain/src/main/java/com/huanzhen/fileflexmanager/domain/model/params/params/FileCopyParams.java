package com.huanzhen.fileflexmanager.domain.model.params.params;

import java.util.List;

import com.huanzhen.fileflexmanager.domain.model.params.ParamMeta;
import com.huanzhen.fileflexmanager.domain.model.params.ParamType;
import lombok.Data;

@Data
public class FileCopyParams {
    @ParamMeta(
            name = "源文件",
            description = "请选择要复制的源目录/文件",
            type = ParamType.FOLDER_FILE_MULTI_SELECT
    )
    private List<String> selectedPaths;

    @ParamMeta(
            name = "目标目录",
            description = "请选择复制的目标目录",
            type = ParamType.FOLDER
    )
    private String targetDir;

    @ParamMeta(
            name = "同步删除",
            description = "是否同步删除操作，原文件夹删除的文件，目标文件夹也会删除",
            type = ParamType.BOOLEAN,
            required = false
    )
    private Boolean syncDelete = false;
} 
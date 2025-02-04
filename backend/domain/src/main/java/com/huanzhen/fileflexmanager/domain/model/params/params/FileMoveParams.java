package com.huanzhen.fileflexmanager.domain.model.params.params;

import java.util.List;

import com.huanzhen.fileflexmanager.domain.model.params.ParamMeta;
import com.huanzhen.fileflexmanager.domain.model.params.ParamType;

import lombok.Data;

@Data
public class FileMoveParams {

    @ParamMeta(
            name = "源文件",
            description = "请选择要移动的源目录/文件",
            type = ParamType.FOLDER_FILE_MULTI_SELECT
    )
    private List<String> selectedPaths;

    @ParamMeta(
            name = "目标目录",
            description = "请选择移动的目标目录",
            type = ParamType.FOLDER
    )
    private String targetDir;

    @ParamMeta(
            name = "覆盖已存在的文件",
            description = "如果目标位置已存在同名文件，是否覆盖",
            type = ParamType.BOOLEAN,
            required = false
    )
    private boolean overwrite = false;

    @ParamMeta(
            name = "移动标签",
            description = "是否将源文件的标签移动到目标文件",
            type = ParamType.BOOLEAN,
            required = false
    )
    private boolean moveTags = true;
}

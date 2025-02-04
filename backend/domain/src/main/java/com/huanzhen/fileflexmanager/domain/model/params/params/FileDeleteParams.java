package com.huanzhen.fileflexmanager.domain.model.params.params;

import java.util.List;

import com.huanzhen.fileflexmanager.domain.model.params.ParamMeta;
import com.huanzhen.fileflexmanager.domain.model.params.ParamType;
import lombok.Data;

@Data
public class FileDeleteParams {
    @ParamMeta(name = "删除文件/目录", type = ParamType.FOLDER_FILE_MULTI_SELECT, required = true, description = "删除文件/目录")
    private List<String> selectedPaths;
}

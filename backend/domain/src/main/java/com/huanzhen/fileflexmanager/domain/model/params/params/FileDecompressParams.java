package com.huanzhen.fileflexmanager.domain.model.params.params;

import com.huanzhen.fileflexmanager.domain.model.params.ParamMeta;
import com.huanzhen.fileflexmanager.domain.model.params.ParamType;
import lombok.Data;

@Data
public class FileDecompressParams {
    @ParamMeta(name = "源文件", type = ParamType.FILE, required = true, description = "要解压的文件路径")
    private String selectPath;

    @ParamMeta(name = "目标目录", type = ParamType.FOLDER, required = true, description = "解压到的目标目录")
    private String destinationPath;

    @ParamMeta(name = "覆盖已存在文件", type = ParamType.BOOLEAN, required = false, description = "是否覆盖已存在的文件")
    private Boolean overwrite = false;
}
package com.huanzhen.fileflexmanager.domain.model.params.params;

import com.huanzhen.fileflexmanager.domain.model.params.ParamMeta;
import com.huanzhen.fileflexmanager.domain.model.params.ParamType;
import lombok.Data;

@Data
public class FileIndexParams {
    @ParamMeta(name = "目录路径", type = ParamType.FOLDER, required = true, description = "要索引的目录路径")
    private String selectPath;

    @ParamMeta(name = "最大递归层级", type = ParamType.NUMBER, required = false, description = "最大递归层级，0表示无限制")
    private Integer maxDepth = 0;

    @ParamMeta(name = "计算MD5", type = ParamType.BOOLEAN, required = false, description = "是否计算文件MD5")
    private Boolean calculateMd5 = false;

}

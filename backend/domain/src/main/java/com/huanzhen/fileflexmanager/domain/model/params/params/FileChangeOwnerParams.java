package com.huanzhen.fileflexmanager.domain.model.params.params;

import com.huanzhen.fileflexmanager.domain.model.params.ParamMeta;
import com.huanzhen.fileflexmanager.domain.model.params.ParamType;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class FileChangeOwnerParams {
    @ParamMeta(
            name = "文件路径",
            description = "请选择要修改权限的文件或目录",
            type = ParamType.FOLDER_FILE
    )
    private String selectPath;

    @ParamMeta(
            name = "所有者",
            description = "请输入新的所有者用户名",
            type = ParamType.TEXT
    )
    private String owner;
}

package com.huanzhen.fileflexmanager.domain.model.params.params;

import com.huanzhen.fileflexmanager.domain.model.params.ParamMeta;
import com.huanzhen.fileflexmanager.domain.model.params.ParamType;
import lombok.Data;

@Data
public class FileCompressParams {
    @ParamMeta(name = "源路径", type = ParamType.FOLDER_FILE, required = true, description = "要压缩的文件或目录路径")
    private String selectPath;

    @ParamMeta(name = "目标路径", type = ParamType.FOLDER, required = true, description = "压缩文件的保存路径")
    private String destinationPath;

    @ParamMeta(name = "压缩格式", type = ParamType.SELECT, required = true, description = "压缩文件格式，如zip、tar.gz等", options = "zip:zip;tar.gz:tar.gz")
    private String format;

    @ParamMeta(name = "压缩级别", type = ParamType.NUMBER, required = false, description = "压缩级别(1-9)，默认为6")
    private Integer level = 6;
}
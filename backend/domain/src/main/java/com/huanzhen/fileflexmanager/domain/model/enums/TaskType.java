package com.huanzhen.fileflexmanager.domain.model.enums;

import com.huanzhen.fileflexmanager.domain.model.config.ParamConfig;
import com.huanzhen.fileflexmanager.domain.model.params.params.*;
import com.huanzhen.fileflexmanager.domain.model.params.params.*;
import com.huanzhen.fileflexmanager.domain.utils.ParamConfigUtils;
import lombok.Getter;

import java.util.List;

@Getter
public enum TaskType {
    FILE_COPY("文件复制", true, FileCopyParams.class, true),
    FILE_MOVE("文件移动", true, FileMoveParams.class),
    FILE_DELETE("文件删除", true, FileDeleteParams.class),
    FILE_CHANGE_OWNER("文件用户修改", false, FileChangeOwnerParams.class),
    FILE_INDEX("文件索引", true, FileIndexParams.class, true),
    FILE_COMPRESS("文件压缩", true, FileCompressParams.class),
    FILE_DECOMPRESS("文件解压", true, FileDecompressParams.class),
    TEST("测试任务", false, null);

    private final String description;
    private final boolean supportScheduled;
    private final Class<?> paramsClass;
    private final boolean manuallyAdd;

    TaskType(String description, boolean supportScheduled, Class<?> paramsClass) {
        this.description = description;
        this.supportScheduled = supportScheduled;
        this.paramsClass = paramsClass;
        this.manuallyAdd = false;
    }

    TaskType(String description, boolean supportScheduled, Class<?> paramsClass, boolean manuallyAdd) {
        this.description = description;
        this.supportScheduled = supportScheduled;
        this.paramsClass = paramsClass;
        this.manuallyAdd = manuallyAdd;
    }

    // 获取任务参数配置列表
    public List<ParamConfig> getParamConfigs() {
        return paramsClass != null ? ParamConfigUtils.getParamConfigs(paramsClass) : List.of();
    }
}
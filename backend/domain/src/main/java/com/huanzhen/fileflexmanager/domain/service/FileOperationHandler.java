package com.huanzhen.fileflexmanager.domain.service;

import com.alibaba.fastjson2.JSONObject;
import com.huanzhen.fileflexmanager.domain.model.enums.FileOperationType;

/**
 * 文件操作处理器接口
 */
public interface FileOperationHandler {
    /**
     * 获取处理器支持的操作类型
     */
    FileOperationType getOperationType();

    /**
     * 执行文件操作
     * @param params 操作参数
     * @return 操作结果
     */
    boolean handle(JSONObject params);

    /**
     * 获取操作描述
     * @param params 操作参数
     * @return 操作描述
     */
    default String getOperationDesc(JSONObject params) {
        return getOperationType().getDescription();
    }
} 
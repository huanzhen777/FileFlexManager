package com.huanzhen.fileflexmanager.domain.model.params;

public enum ParamType {
    FOLDER,    // 文件夹选择器
    FILE,      // 文件选择器
    FOLDER_FILE,//文件或者文件夹
    TEXT,      // 文本输入
    NUMBER,    // 数字输入
    BOOLEAN,  // 布尔开关
    /**
     * 选项
     */
    SELECT,
    LIST,
    FOLDER_MULTI_SELECT,
    FOLDER_FILE_MULTI_SELECT;
}

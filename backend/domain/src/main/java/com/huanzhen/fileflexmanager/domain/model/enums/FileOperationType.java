package com.huanzhen.fileflexmanager.domain.model.enums;

import com.huanzhen.fileflexmanager.domain.model.config.ParamConfig;
import com.huanzhen.fileflexmanager.domain.utils.ParamConfigUtils;
import lombok.Getter;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Getter
public enum FileOperationType {
    // 基础文件操作 - 需要任务管理的操作
    COPY("复制", true, true, null, TaskType.FILE_COPY, true),
    MOVE("移动", true, true, null, TaskType.FILE_MOVE, true),
    DELETE("删除", true, true, null, TaskType.FILE_DELETE, true),
    /*FILE_CHANGE_OWNER("文件用户修改", true, true, null, TaskType.FILE_CHANGE_OWNER),
*/
    // 压缩文件操作 - 需要任务管理
    COMPRESS("压缩", true, true, null, TaskType.FILE_COMPRESS),
    DECOMPRESS("解压缩", true, false, Arrays.asList(".zip", ".rar", ".7z", ".tar", ".gz"),
            TaskType.FILE_DECOMPRESS),
    FILE_INDEX("文件索引", false, true, null, TaskType.FILE_INDEX),
    ;

    private final String description;
    private final boolean supportFile; // 是否支持文件
    private final boolean supportDirectory; // 是否支持目录
    private final List<String> supportedExtensions; // 支持的文件扩展名，null表示支持所有
    private final TaskType taskType; // 对应的任务类型，null表示不是任务
    private final Class<?> paramsClass; // 参数格式定义类
    private final Boolean isSync;
    private Boolean supportMultiFile = false;

    FileOperationType(String description, boolean supportFile, boolean supportDirectory,
                      List<String> supportedExtensions, Class<?> paramsClass) {
        this.description = description;
        this.supportFile = supportFile;
        this.supportDirectory = supportDirectory;
        this.supportedExtensions = supportedExtensions;
        this.paramsClass = paramsClass;
        this.taskType = null;
        this.isSync = false;
    }

    FileOperationType(String description, boolean supportFile, boolean supportDirectory,
                      List<String> supportedExtensions, TaskType taskType) {
        this.description = description;
        this.supportFile = supportFile;
        this.supportDirectory = supportDirectory;
        this.supportedExtensions = supportedExtensions;
        this.taskType = taskType;
        this.paramsClass = taskType.getParamsClass();
        this.isSync = false;
    }

    FileOperationType(String description, boolean supportFile, boolean supportDirectory,
                      List<String> supportedExtensions, boolean isSync, TaskType taskType) {
        this.description = description;
        this.supportFile = supportFile;
        this.supportDirectory = supportDirectory;
        this.supportedExtensions = supportedExtensions;
        this.taskType = taskType;
        this.paramsClass = taskType.getParamsClass();
        this.isSync = isSync;
    }

    FileOperationType(String description, boolean supportFile, boolean supportDirectory,
                      List<String> supportedExtensions, TaskType taskType, boolean supportMultiFile) {
        this.description = description;
        this.supportFile = supportFile;
        this.supportDirectory = supportDirectory;
        this.supportedExtensions = supportedExtensions;
        this.taskType = taskType;
        this.paramsClass = taskType.getParamsClass();
        this.isSync = false;
        this.supportMultiFile = supportMultiFile;
    }

    /**
     * 检查是否支持指定的文件类型
     */
    public boolean isSupported(String path, boolean isDirectory) {
        // 检查是否支持文件/目录
        if (isDirectory && !supportDirectory)
            return false;
        if (!isDirectory && !supportFile)
            return false;

        // 如果没有指定支持的扩展名，则支持所有类型
        if (supportedExtensions == null)
            return true;

        // 如果是目录，且操作支持目录，则返回true
        if (isDirectory && supportDirectory)
            return true;

        // 检查文件扩展名
        return supportedExtensions.stream()
                .anyMatch(ext -> path.toLowerCase().endsWith(ext.toLowerCase()));
    }

    /**
     * 获取所有支持指定文件类型的操作
     */
    public static List<FileOperationType> getSupportedOperations(String path, boolean isDirectory) {
        return Arrays.stream(FileOperationType.values())
                .filter(op -> op.isSupported(path, isDirectory))
                .collect(Collectors.toList());
    }

    /**
     * 获取操作的参数配置
     */
    public List<ParamConfig> getParamConfigs() {
        return ParamConfigUtils.getParamConfigs(paramsClass);
    }

    /**
     * 判断是否为任务类型操作
     */
    public boolean isTask() {
        return taskType != null;
    }

    /**
     * 根据TaskType获取对应的文件操作类型
     */
    public static FileOperationType fromTaskType(TaskType taskType) {
        return Arrays.stream(FileOperationType.values())
                .filter(op -> op.taskType == taskType)
                .findFirst()
                .orElse(null);
    }
}
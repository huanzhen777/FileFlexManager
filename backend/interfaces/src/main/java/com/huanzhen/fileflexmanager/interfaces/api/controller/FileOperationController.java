package com.huanzhen.fileflexmanager.interfaces.api.controller;

import com.alibaba.fastjson2.JSONObject;
import com.huanzhen.fileflexmanager.application.service.FileOperationService;
import com.huanzhen.fileflexmanager.domain.model.BaseResponse;
import com.huanzhen.fileflexmanager.domain.model.enums.FileOperationType;
import com.huanzhen.fileflexmanager.interfaces.model.vo.FileOperationVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/file-operations")
@RequiredArgsConstructor
public class FileOperationController {
    private final FileOperationService fileOperationService;

    /**
     * 获取所有文件操作类型及其支持信息
     */
    @GetMapping("/types")
    public BaseResponse<List<FileOperationVO>> getAllOperationTypes() {
        try {
            List<FileOperationVO> operations = Arrays.stream(FileOperationType.values())
                    .map(type -> FileOperationVO.builder()
                            .type(type)
                            .description(type.getDescription())
                            .supportFile(type.isSupportFile())
                            .supportDirectory(type.isSupportDirectory())
                            .supportedExtensions(type.getSupportedExtensions())
                            .isTask(type.isTask())
                            .paramConfigs(type.getParamConfigs())
                            .isSync(type.getIsSync())
                            .supportMultiSelect(type.getSupportMultiFile())
                            .build())
                    .collect(Collectors.toList());
            return BaseResponse.success(operations);
        } catch (Exception e) {
            return BaseResponse.error("获取文件操作类型列表失败: " + e.getMessage());
        }
    }

    /**
     * 执行文件操作
     */
    @PostMapping("/{operationType}/execute")
    public BaseResponse<String> executeOperation(
            @PathVariable FileOperationType operationType,
            @RequestBody JSONObject params) {
        try {
            String result = fileOperationService.executeOperation(operationType, params);
            return BaseResponse.success(result);
        } catch (Exception e) {
            return BaseResponse.error("执行操作失败: " + e.getMessage());
        }
    }
} 
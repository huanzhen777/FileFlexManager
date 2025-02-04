package com.huanzhen.fileflexmanager.interfaces.api.controller;

import com.alibaba.fastjson2.JSON;
import com.huanzhen.fileflexmanager.domain.utils.ParamConfigUtils;
import com.huanzhen.fileflexmanager.interfaces.model.dto.ConfigDTO;
import com.huanzhen.fileflexmanager.interfaces.model.req.UpdateConfigRequest;
import com.huanzhen.fileflexmanager.application.service.ConfigService;
import com.huanzhen.fileflexmanager.domain.model.enums.ConfigEnum;
import com.huanzhen.fileflexmanager.domain.model.BaseResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/configs")
@RequiredArgsConstructor
public class ConfigController {

    private final ConfigService configService;

    @GetMapping
    public BaseResponse<List<ConfigDTO>> getAllConfigs() {
        List<ConfigDTO> dtos = Arrays.stream(ConfigEnum.values())
                .filter(ConfigEnum::isCanView)
                .map(configEnum -> ConfigDTO.builder()
                        .name(configEnum.name())
                        .value(configEnum.getValueWithType())
                        .description(configEnum.getDescription())
                        .title(configEnum.getTitle())
                        .type(configEnum.getType().name())
                        .typeDesc(configEnum.getType().getDesc())
                        .paramConfig(ParamConfigUtils.getConfigParamConfig(configEnum))
                        .build())
                .collect(Collectors.toList());
        return BaseResponse.success(dtos);
    }

    @PutMapping
    public BaseResponse<Void> updateConfig(@RequestBody UpdateConfigRequest request) {
        ConfigEnum configEnum = ConfigEnum.valueOf(request.getName());
        if (request.getValue() instanceof String) {
            configEnum.setValue((String) request.getValue());
        } else {
            configEnum.setValue(JSON.toJSONString(request.getValue()));
        }
        return BaseResponse.success(null);
    }
} 
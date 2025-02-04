package com.huanzhen.fileflexmanager.application.service;

import cn.hutool.core.util.StrUtil;
import com.huanzhen.fileflexmanager.domain.model.entity.Config;
import com.huanzhen.fileflexmanager.domain.model.enums.ConfigEnum;
import com.huanzhen.fileflexmanager.domain.repository.ConfigRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ConfigService {

    private final ConfigRepository configRepository;

    /**
     * 初始化默认配置
     */
    public void initDefaultConfigs() {
        log.info("开始初始化默认配置");

        // 初始化所有配置项
        for (ConfigEnum configEnum : ConfigEnum.values()) {
            String value = configEnum.getValue();
            if (StrUtil.isBlank(value)) {
                log.info("初始化配置项: {}", configEnum.name());
                configEnum.setDefaultValue();
            }
        }
    }

    /**
     * 获取所有配置
     */
    public List<Config> getAllConfigs() {
        return configRepository.findAll();
    }


    @PostConstruct
    public void init() {
        ConfigEnum.initRepository(configRepository);
        initDefaultConfigs();
    }

} 
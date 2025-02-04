package com.huanzhen.fileflexmanager.infrastructure.persistence.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.huanzhen.fileflexmanager.domain.model.entity.Config;
import com.huanzhen.fileflexmanager.domain.repository.ConfigRepository;
import com.huanzhen.fileflexmanager.infrastructure.persistence.converter.ConfigDOConvert;
import com.huanzhen.fileflexmanager.infrastructure.persistence.entity.ConfigDO;
import com.huanzhen.fileflexmanager.infrastructure.persistence.mapper.ConfigMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class ConfigRepositoryImpl implements ConfigRepository {

    private final ConfigMapper configMapper;
    private final ConfigDOConvert configDOConvert;

    @Override
    public Config save(Config config) {
        long currentTime = System.currentTimeMillis();
        config.setCreateTime(currentTime);
        config.setUpdateTime(currentTime);
        ConfigDO configDO = configDOConvert.toDO(config);
        configMapper.insert(configDO);
        return configDOConvert.toDomain(configDO);
    }

    @Override
    public Config findByName(String name) {
        LambdaQueryWrapper<ConfigDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ConfigDO::getName, name);
        return configDOConvert.toDomain(configMapper.selectOne(wrapper));
    }

    @Override
    public List<Config> findAll() {
        return configMapper.selectList(null)
                .stream()
                .map(configDOConvert::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public void update(Config config) {
        config.setUpdateTime(System.currentTimeMillis());
        ConfigDO configDO = configDOConvert.toDO(config);
        LambdaQueryWrapper<ConfigDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ConfigDO::getName, config.getName());
        configMapper.update(configDO, wrapper);
    }

    @Override
    public void deleteByName(String name) {
        LambdaQueryWrapper<ConfigDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ConfigDO::getName, name);
        configMapper.delete(wrapper);
    }

    @Override
    public String getValueOrDefault(String name, String defaultValue) {
        Config config = findByName(name);
        return config != null ? config.getValue() : defaultValue;
    }
} 
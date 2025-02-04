package com.huanzhen.fileflexmanager.domain.repository;

import com.huanzhen.fileflexmanager.domain.model.entity.Config;
import java.util.List;

public interface ConfigRepository {
    Config save(Config config);
    
    Config findByName(String name);
    
    List<Config> findAll();
    
    void update(Config config);
    
    void deleteByName(String name);
    
    /**
     * 获取配置值，如果不存在则返回默认值
     */
    String getValueOrDefault(String name, String defaultValue);
} 
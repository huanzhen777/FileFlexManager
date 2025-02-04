package com.huanzhen.fileflexmanager.domain.model.enums;

import cn.hutool.core.lang.Assert;
import cn.hutool.core.util.RandomUtil;
import com.huanzhen.fileflexmanager.domain.model.entity.Config;
import com.huanzhen.fileflexmanager.domain.model.params.ParamMeta;
import com.huanzhen.fileflexmanager.domain.model.params.ParamType;
import com.huanzhen.fileflexmanager.domain.repository.ConfigRepository;
import com.huanzhen.fileflexmanager.domain.utils.ParamConfigUtils;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 * 系统配置项枚举
 */
@Getter
@Slf4j
public enum ConfigEnum {

    JWT_SECRET("JWT密钥", null, false, ConfigType.SECURITY, "JWT密钥") {
        @Override
        public void setDefaultValue() {
            // 生成一个32位的随机字符串作为JWT密钥
            String randomKey = RandomUtil.randomString(32);
            setValue(randomKey);
        }
    },
    @ParamMeta(name = "", description = "", type = ParamType.NUMBER)
    JWT_TOKEN_EXPIRATION("登录过期时间", "86400", ConfigType.SECURITY, "登录token过期时间(秒) 下一次登录生效"), // 默认24小时
    @ParamMeta(name = "", description = "", type = ParamType.BOOLEAN)
    FILE_FOLDER_USE_INDEX_SIZE("文件夹大小使用文件索引数据", "true", ConfigType.USER, "文件索引时的数据可能不准确，如果关闭将不展示文件夹大小"),

    /*@ParamMeta(name = "", description = "", type = ParamType.LIST, paramClass = WebdavConfig.class)
    WEBDAV_CONFIG("webdav配置", "[]", ConfigType.USER, "配置挂载webdav"),*/
    ;

    private final String title;
    private final String defaultValue;
    private final boolean canView;
    private final ConfigType type;
    private final String description;

    private static ConfigRepository configRepository;

    ConfigEnum(String title, String defaultValue, ConfigType type, String description) {
        this.title = title;
        this.defaultValue = defaultValue;
        this.canView = true;
        this.type = type;
        this.description = description;
    }

    ConfigEnum(String title, String defaultValue, boolean canView, ConfigType type, String description) {
        this.title = title;
        this.defaultValue = defaultValue;
        this.canView = canView;
        this.type = type;
        this.description = description;
    }

    /**
     * 初始化配置仓储
     * 需要在应用启动时调用此方法注入ConfigRepository
     */
    public static void initRepository(ConfigRepository repository) {
        configRepository = repository;
    }

    /**
     * 获取配置值
     */
    public String getValue() {
        Assert.notNull(configRepository);
        return configRepository.getValueOrDefault(this.name(), this.defaultValue);
    }

    public Object getValueWithType() {
        Assert.notNull(configRepository);
        String value = configRepository.getValueOrDefault(this.name(), this.defaultValue);
        return ParamConfigUtils.tryConvertValue(this, value);
    }

    /**
     * 获取整数类型的配置值
     */
    public Integer getIntValue() {
        return Integer.parseInt(getValue());
    }

    /**
     * 获取布尔类型的配置值
     */
    public Boolean getBooleanValue() {
        return Boolean.parseBoolean(getValue());
    }

    /**
     * 获取长整型的配置值
     */
    public Long getLongValue() {
        return Long.parseLong(getValue());
    }

    /**
     * 更新配置值
     * @param value 新的配置值
     */
    public void setValue(String value) {
        Assert.notNull(configRepository);
        ParamConfigUtils.validateParam(this, value);

        log.debug("更新配置 {} = {}", this.name(), value);

        Config config = configRepository.findByName(this.name());
        if (config == null) {
            config = new Config();
            config.setName(this.name());
            config.setValue(value);
            config.setDescription(this.description);
            configRepository.save(config);
        } else {
            config.setValue(value);
            configRepository.update(config);
        }
    }

    /**
     * 重置配置为默认值
     */
    public void resetToDefault() {
        setValue(this.defaultValue);
    }

    /**
     * 生成并设置配置值的默认实现
     * 子类可以重写此方法以提供自定义的值生成逻辑
     */
    public void setDefaultValue() {

    }
} 
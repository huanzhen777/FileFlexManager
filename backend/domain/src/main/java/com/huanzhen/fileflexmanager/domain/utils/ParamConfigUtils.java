package com.huanzhen.fileflexmanager.domain.utils;

import cn.hutool.core.util.BooleanUtil;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.TypeReference;
import com.huanzhen.fileflexmanager.domain.model.config.ParamConfig;
import com.huanzhen.fileflexmanager.domain.model.enums.ConfigEnum;
import com.huanzhen.fileflexmanager.domain.model.params.ParamMeta;
import com.huanzhen.fileflexmanager.domain.model.params.ParamType;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 参数配置工具类
 * 基础设施层工具类，用于处理参数配置的技术性操作
 */
@Slf4j
public class ParamConfigUtils {

    /**
     * 获取类的参数配置列表
     *
     * @param paramsClass 参数类
     * @return 参数配置列表
     */
    public static List<ParamConfig> getParamConfigs(Class<?> paramsClass) {
        if (paramsClass == null) {
            return null;
        }
        return Arrays.stream(paramsClass.getDeclaredFields())
                .filter(field -> field.isAnnotationPresent(ParamMeta.class))
                .map(ParamConfigUtils::convertToParamConfig)
                .collect(Collectors.toList());
    }

    /**
     * 获取枚举实例的参数配置
     *
     * @param enumInstance 枚举实例
     * @return 参数配置
     */
    @SneakyThrows
    public static ParamConfig getConfigParamConfig(ConfigEnum enumInstance) {
        if (enumInstance == null) {
            return null;
        }

        Field field = enumInstance.getClass().getField(enumInstance.name());
        return convertToParamConfig(field);
    }

    /**
     * 将字段注解转换为参数配置
     *
     * @param field 字段
     * @return 参数配置
     */
    public static ParamConfig convertToParamConfig(Field field) {
        ParamMeta meta = field.getAnnotation(ParamMeta.class);
        if (meta == null) {
            return null;
        }

        String options = meta.options();
        List<ParamConfig.Option> optionList = null;
        if (StringUtils.isNotBlank(options)) {
            optionList = Arrays.stream(options.split(";"))
                    .map(s -> {
                        String[] split = s.split(":");
                        return new ParamConfig.Option(split[0], split[1]);
                    })
                    .collect(Collectors.toList());
        }

        // 获取字段的默认值
        Object defaultValue = null;
        try {
            field.setAccessible(true);
            // 创建一个新实例来获取默认值
            if (field.getDeclaringClass() != ConfigEnum.class) {
                Object instance = field.getDeclaringClass().getDeclaredConstructor().newInstance();
                defaultValue = field.get(instance);
            }
        } catch (Exception e) {
            log.error("获取字段默认值失败: {}", field.getName(), e);
        }

        List<ParamConfig> paramConfigs = null;
        if (meta.type() == ParamType.LIST && meta.paramClass() != String.class) {
            paramConfigs = getParamConfigs(meta.paramClass());
        }

        return ParamConfig.builder()
                .name(meta.name())
                .key(field.getName())
                .type(meta.type())
                .required(meta.required())
                .description(meta.description())
                .options(optionList)
                .defaultValue(defaultValue)
                .paramConfigs(paramConfigs)
                .build();
    }

    public static void validateParam(ConfigEnum configEnum, String value) {
        convertValue(configEnum, value);
    }

    @SneakyThrows
    public static Object convertValue(ConfigEnum configEnum, String value) {
        Field field = configEnum.getClass().getField(configEnum.name());
        ParamMeta meta = field.getAnnotation(ParamMeta.class);
        if (meta == null) {
            return value;
        }
        switch (meta.type()) {
            case FILE:
            case FOLDER:
            case FOLDER_FILE:
            case TEXT:
            case SELECT:
                return value;
            case NUMBER:
                return Long.valueOf(value);
            case BOOLEAN:
                return BooleanUtil.toBoolean(value);
            case LIST:
                Class aClass = meta.paramClass();
                if (aClass == String.class) {
                    return JSON.parseObject(configEnum.getValue(), new TypeReference<List<String>>() {
                    });
                } else {
                    return JSON.parseObject(configEnum.getValue(), new TypeReference<List<Object>>() {
                    });
                }
            default:
        }
        return value;
    }

    public static Object tryConvertValue(ConfigEnum configEnum, String value) {
        try {
            return convertValue(configEnum, value);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        return value;
    }


}

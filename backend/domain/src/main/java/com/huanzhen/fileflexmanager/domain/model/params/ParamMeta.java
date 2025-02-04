package com.huanzhen.fileflexmanager.domain.model.params;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ParamMeta {
    String name();             // 参数显示名称

    String description();      // 参数描述

    ParamType type();         // 参数类型

    boolean required() default true;  // 是否必填

    /**
     * "lable:value;1:2"
     */
    String options() default "";


    Class paramClass() default String.class;
}
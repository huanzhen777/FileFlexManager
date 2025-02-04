package com.huanzhen.fileflexmanager.interfaces.model.req;

import lombok.Data;

@Data
public class UpdateConfigRequest {
    private String name;
    private Object value;
} 
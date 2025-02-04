package com.huanzhen.fileflexmanager.interfaces.model.req;

public record CreateTagRequest(
    String name,
    Long parentId,
    Boolean quickAccess,
    Boolean bindFile
) {}

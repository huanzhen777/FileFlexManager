package com.huanzhen.fileflexmanager.interfaces.model.req;

import java.util.List;

public record UpdateFileTagsRequest(List<Long> tagIds) {
}

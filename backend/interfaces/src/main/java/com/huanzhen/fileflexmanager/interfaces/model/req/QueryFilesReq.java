package com.huanzhen.fileflexmanager.interfaces.model.req;

import cn.hutool.core.util.StrUtil;
import com.huanzhen.fileflexmanager.domain.model.Constants;

public record QueryFilesReq(int page, int size, String path) {
    public QueryFilesReq{
        if (StrUtil.isBlank(path)) {
            path = Constants.DEFAULT_PATH_TAG;
        }
    }

}

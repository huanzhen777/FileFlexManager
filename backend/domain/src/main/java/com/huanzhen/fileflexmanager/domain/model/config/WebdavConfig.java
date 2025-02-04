package com.huanzhen.fileflexmanager.domain.model.config;

import com.huanzhen.fileflexmanager.domain.model.params.ParamMeta;
import com.huanzhen.fileflexmanager.domain.model.params.ParamType;
import lombok.Data;

@Data
public class WebdavConfig {
    @ParamMeta(name = "url", description = "url", type = ParamType.TEXT)
    private String url;
    @ParamMeta(name = "用户名", description = "用户名", type = ParamType.TEXT)
    private String user;
    @ParamMeta(name = "密码", description = "密码", type = ParamType.TEXT)
    private String password;
    @ParamMeta(name = "挂载路径", description = "挂载路径", type = ParamType.FOLDER)
    private String mountPath;

    @ParamMeta(name = "挂载名称", description = "挂载名称", type = ParamType.TEXT)
    private String mountName;
}

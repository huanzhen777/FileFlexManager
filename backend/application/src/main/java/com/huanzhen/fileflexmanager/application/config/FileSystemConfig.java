package com.huanzhen.fileflexmanager.application.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "app.filesystem")
public class FileSystemConfig {
    /**
     * 文件掩码，默认值 022 
     * - 文件最大权限 666 减去 022 得到 644 (rw-r--r--)
     * - 目录最大权限 777 减去 022 得到 755 (rwxr-xr-x)
     */
    private int umask = 022;
    
    /**
     * 获取新建文件的权限
     * @return 文件权限值
     */
    public int getFileMode() {
        return 0666 & ~umask;
    }
    
    /**
     * 获取新建目录的权限
     * @return 目录权限值
     */
    public int getDirectoryMode() {
        return 0777 & ~umask;
    }
} 
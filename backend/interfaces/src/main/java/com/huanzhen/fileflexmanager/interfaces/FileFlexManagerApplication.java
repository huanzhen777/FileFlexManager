package com.huanzhen.fileflexmanager.interfaces;

import org.mybatis.spring.annotation.MapperScan;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.nio.charset.Charset;

@EnableScheduling
@SpringBootApplication
@MapperScan("com.huanzhen.fileflexmanager.infrastructure.persistence.mapper")
@ComponentScan(basePackages = "com.huanzhen.fileflexmanager")
public class FileFlexManagerApplication {
    private static final Logger logger = LoggerFactory.getLogger(FileFlexManagerApplication.class);

    public static void main(String[] args) {
        // 在最开始设置系统属性
        System.setProperty("file.encoding", "UTF-8");
        System.setProperty("sun.jnu.encoding", "UTF-8");

        // 设置文件系统编码
        System.setProperty("sun.nio.fs.defaultEncoding", "UTF-8");
        SpringApplication.run(FileFlexManagerApplication.class, args);
        logger.info("应用启动成功！");
        logger.info("file.encoding is {}", Charset.defaultCharset().displayName());
    }
}

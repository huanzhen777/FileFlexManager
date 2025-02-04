package com.huanzhen.fileflexmanager.interfaces.test.config;

import com.huanzhen.fileflexmanager.domain.repository.TaskRepository;
import org.mockito.Mockito;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Primary;

@TestConfiguration
@ComponentScan(basePackages = {
    "com.huanzhen.fileflexmanager.interfaces.test.mock",
    "com.huanzhen.fileflexmanager.application.service"
})
public class TestConfig {

    @Bean
    @Primary
    public TaskRepository taskRepository() {
        return Mockito.mock(TaskRepository.class);
    }

}
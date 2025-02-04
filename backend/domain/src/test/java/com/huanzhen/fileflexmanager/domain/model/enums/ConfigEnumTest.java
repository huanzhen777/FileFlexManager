package com.huanzhen.fileflexmanager.domain.model.enums;

import com.huanzhen.fileflexmanager.domain.repository.ConfigRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ConfigEnumTest {

    @Mock
    private ConfigRepository configRepository;

    @BeforeEach
    void setUp() {
        ConfigEnum.initRepository(configRepository);
    }

    @Test
    void shouldGenerateDefaultValueForJwtSecret() {
        // when
        ConfigEnum.JWT_SECRET.setDefaultValue();

        // then
        verify(configRepository).findByName(ConfigEnum.JWT_SECRET.name());
        verify(configRepository).save(argThat(config -> 
            config.getName().equals(ConfigEnum.JWT_SECRET.name()) &&
            config.getValue().length() == 32 &&
            config.getDescription().equals(ConfigEnum.JWT_SECRET.getDescription())
        ));
    }

    @Test
    void shouldReturnExistingConfigValue() {
        // given
        String configValue = "testValue";
        when(configRepository.getValueOrDefault(
            ConfigEnum.JWT_TOKEN_EXPIRATION.name(),
            ConfigEnum.JWT_TOKEN_EXPIRATION.getDefaultValue()
        )).thenReturn(configValue);

        // when
        String result = ConfigEnum.JWT_TOKEN_EXPIRATION.getValue();

        // then
        assertThat(result).isEqualTo(configValue);
    }

    @Test
    void shouldReturnDefaultValueWhenConfigNotExists() {
        // given
        String defaultValue = "86400";
        when(configRepository.getValueOrDefault(
            ConfigEnum.JWT_TOKEN_EXPIRATION.name(),
            ConfigEnum.JWT_TOKEN_EXPIRATION.getDefaultValue()
        )).thenReturn(defaultValue);

        // when
        String result = ConfigEnum.JWT_TOKEN_EXPIRATION.getValue();

        // then
        assertThat(result).isEqualTo(defaultValue);
    }



    @Test
    void shouldCorrectlyConvertTypes() {
        // given
        when(configRepository.getValueOrDefault(eq(ConfigEnum.JWT_TOKEN_EXPIRATION.name()), any()))
            .thenReturn("100");

        // when & then
        assertThat(ConfigEnum.JWT_TOKEN_EXPIRATION.getIntValue()).isEqualTo(100);
        assertThat(ConfigEnum.JWT_TOKEN_EXPIRATION.getLongValue()).isEqualTo(100L);
    }

    @Test
    void shouldResetToDefaultValue() {
        // when
        ConfigEnum.JWT_TOKEN_EXPIRATION.resetToDefault();

        // then
        verify(configRepository).findByName(ConfigEnum.JWT_TOKEN_EXPIRATION.name());
        verify(configRepository).save(argThat(config ->
            config.getName().equals(ConfigEnum.JWT_TOKEN_EXPIRATION.name()) &&
            config.getValue().equals(ConfigEnum.JWT_TOKEN_EXPIRATION.getDefaultValue()) &&
            config.getDescription().equals(ConfigEnum.JWT_TOKEN_EXPIRATION.getDescription())
        ));
    }
} 
package com.ieum.dodam.welfare.infrastructure.gov24;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Bean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.web.client.RestClient;
import org.springframework.boot.ApplicationRunner;

/** 정부24 복지 API 설정을 애플리케이션 설정으로 등록한다. */
@Configuration
@EnableConfigurationProperties(Gov24WelfareProperties.class)
public class Gov24WelfareConfiguration {
    @Bean
    Gov24WelfareClient gov24WelfareClient(RestClient.Builder builder, Gov24WelfareProperties properties) {
        return new HttpGov24WelfareClient(builder, properties);
    }

    @Bean
    @ConditionalOnBean(com.ieum.dodam.welfare.command.application.WelfareSyncService.class)
    ApplicationRunner welfareInitialSync(Gov24WelfareProperties properties,
                                         com.ieum.dodam.welfare.command.application.WelfareSyncService syncService) {
        return args -> {
            if (properties.isSyncEnabled() && !properties.getServiceKey().isBlank()) {
                syncService.synchronizeAll(properties.getPageSize());
            }
        };
    }
}

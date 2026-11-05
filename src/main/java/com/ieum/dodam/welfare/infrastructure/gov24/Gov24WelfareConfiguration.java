package com.ieum.dodam.welfare.infrastructure.gov24;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/** 정부24 복지 API 설정을 애플리케이션 설정으로 등록한다. */
@Configuration
@EnableConfigurationProperties(Gov24WelfareProperties.class)
public class Gov24WelfareConfiguration {
}

package com.ieum.dodam.welfare.infrastructure.gov24;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import java.net.URI;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class Gov24WelfareConfigurationTest {
    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withUserConfiguration(Gov24WelfareConfiguration.class);

    @Test
    void bindsGov24Settings() {
        runner.withPropertyValues(
                        "dodam.welfare.gov24.base-url=https://example.test/api",
                        "dodam.welfare.gov24.service-key=top-secret",
                        "dodam.welfare.gov24.page-size=250",
                        "dodam.welfare.gov24.connect-timeout=3s",
                        "dodam.welfare.gov24.read-timeout=12s",
                        "dodam.welfare.gov24.sync-enabled=true")
                .run(context -> {
                    Gov24WelfareProperties properties = context.getBean(Gov24WelfareProperties.class);
                    assertThat(properties.getBaseUrl()).isEqualTo(URI.create("https://example.test/api"));
                    assertThat(properties.getServiceKey()).isEqualTo("top-secret");
                    assertThat(properties.getPageSize()).isEqualTo(250);
                    assertThat(properties.getConnectTimeout()).isEqualTo(Duration.ofSeconds(3));
                    assertThat(properties.getReadTimeout()).isEqualTo(Duration.ofSeconds(12));
                    assertThat(properties.isSyncEnabled()).isTrue();
                });
    }

    @Test
    void neverExposesServiceKeyInStringRepresentation() {
        Gov24WelfareProperties properties = new Gov24WelfareProperties();
        properties.setServiceKey("top-secret");

        assertThat(properties.toString()).doesNotContain("top-secret").contains("serviceKey=***");
    }

    @Test
    void rejectsPageSizeOutsidePortalLimit() {
        runner.withPropertyValues("dodam.welfare.gov24.page-size=0")
                .run(context -> assertThat(context).hasFailed());
    }
}

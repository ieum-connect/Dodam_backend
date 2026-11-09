package com.ieum.dodam.vaccination.infrastructure;

import com.ieum.dodam.global.exception.BusinessException;
import com.ieum.dodam.vaccination.domain.VaccinationSchedulePort;
import com.ieum.dodam.vaccination.error.VaccinationErrorCode;

import org.junit.jupiter.api.Test;
import org.springframework.boot.convert.ApplicationConversionService;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.web.client.RestClient;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class VaccinationScheduleConfigTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withInitializer(context -> context.getBeanFactory()
                    .setConversionService(ApplicationConversionService.getSharedInstance()))
            .withUserConfiguration(VaccinationScheduleConfig.class)
            .withBean(RestClient.Builder.class, RestClient::builder);

    @Test
    void emptyBaseUrlFailsEveryCallWith503() {
        runner.run(context -> assertThatThrownBy(() -> context.getBean(VaccinationSchedulePort.class)
                        .fetchSchedule(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 2, 1), List.of()))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(VaccinationErrorCode.AI_UNAVAILABLE)));
    }

    @Test
    void configuredBaseUrlSelectsHttp() {
        runner.withPropertyValues("dodam.ai.base-url=http://ai.test", "dodam.ai.internal-token=test-token")
                .run(context -> assertThat(context.getBean(VaccinationSchedulePort.class))
                        .isInstanceOf(HttpVaccinationScheduleClient.class));
    }

    @Test
    void baseUrlWithoutInternalTokenFailsStartup() {
        runner.withPropertyValues("dodam.ai.base-url=http://ai.test")
                .run(context -> assertThat(context.getStartupFailure())
                        .rootCause().hasMessageContaining("dodam.ai.internal-token"));
    }
}

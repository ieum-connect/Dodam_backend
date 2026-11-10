package com.ieum.dodam.vaccination.infrastructure;

import com.ieum.dodam.global.exception.BusinessException;
import com.ieum.dodam.vaccination.domain.VaccinationSchedulePort;
import com.ieum.dodam.vaccination.error.VaccinationErrorCode;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Duration;

/** 예방접종 일정 AI 클라이언트 설정입니다. DocumentAnalysisConfig와 같은 dodam.ai.* 값을 씁니다. */
@Configuration
public class VaccinationScheduleConfig {

    private static final Logger log = LoggerFactory.getLogger(VaccinationScheduleConfig.class);

    /**
     * AI 주소가 없으면 호출마다 503을 던지는 구현을 반환합니다(빈 일정으로 성공 처리하지 않음).
     * 주소만 있고 내부 토큰이 비면 기동을 실패시킵니다.
     */
    @Bean
    public VaccinationSchedulePort vaccinationSchedulePort(
            RestClient.Builder builder,
            @Value("${dodam.ai.base-url:}") String baseUrl,
            @Value("${dodam.ai.internal-token:}") String internalToken,
            @Value("${dodam.ai.connect-timeout:2s}") Duration connectTimeout,
            @Value("${dodam.ai.read-timeout:5s}") Duration readTimeout) {
        if (!StringUtils.hasText(baseUrl)) {
            log.warn("AI 서버 주소(dodam.ai.base-url)가 없어 예방접종 일정 조회는 503으로 응답합니다.");
            return (birthDate, asOf, completedDoses) -> {
                log.warn("AI 서버 주소가 없어 예방접종 일정을 조회할 수 없습니다.");
                throw new BusinessException(VaccinationErrorCode.AI_UNAVAILABLE);
            };
        }
        Assert.state(
                StringUtils.hasText(internalToken),
                "dodam.ai.base-url이 설정됐지만 dodam.ai.internal-token이 비어 있습니다.");
        builder.requestFactory(requestFactory(connectTimeout, readTimeout));
        return new HttpVaccinationScheduleClient(builder, baseUrl, internalToken);
    }

    // DocumentAnalysisConfig와 같은 이유로 JDK HttpClient를 씁니다(POST의 401을 연결 오류로 던지지 않게).
    private static JdkClientHttpRequestFactory requestFactory(Duration connectTimeout, Duration readTimeout) {
        HttpClient httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(connectTimeout)
                .build();
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(httpClient);
        factory.setReadTimeout(readTimeout);
        return factory;
    }
}

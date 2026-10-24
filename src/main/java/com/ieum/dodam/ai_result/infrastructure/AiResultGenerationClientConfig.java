package com.ieum.dodam.ai_result.infrastructure;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

import java.time.Duration;

@Configuration
public class AiResultGenerationClientConfig {

    /** 설정된 AI Backend 주소와 내부 토큰으로 연동 클라이언트를 등록한다. */
    @Bean
    @ConditionalOnProperty(prefix = "dodam.ai", name = "base-url")
    AiResultGenerationClient aiResultGenerationClient(
            RestClient.Builder builder,
            @Value("${dodam.ai.base-url:}") String baseUrl,
            @Value("${dodam.ai.internal-token:}") String internalToken,
            @Value("${dodam.ai.connect-timeout:2s}") Duration connectTimeout,
            @Value("${dodam.ai.result-read-timeout:330s}") Duration readTimeout
    ) {
        // Keep the default Spring request factory here: it preserves JSON request bodies
        // across the local FastAPI boundary. The AI provider applies its own model timeout.
        return new AiResultGenerationClient(builder, baseUrl, internalToken);
    }
}

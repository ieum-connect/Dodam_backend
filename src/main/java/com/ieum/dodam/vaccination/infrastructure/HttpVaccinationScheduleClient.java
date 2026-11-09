package com.ieum.dodam.vaccination.infrastructure;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.MissingNode;
import com.ieum.dodam.global.exception.BusinessException;
import com.ieum.dodam.vaccination.domain.UnknownDoseException;
import com.ieum.dodam.vaccination.domain.VaccinationSchedulePort;
import com.ieum.dodam.vaccination.error.VaccinationErrorCode;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** AI 서버의 예방접종 일정 내부 API를 호출합니다. 응답은 해석하지 않고 JSON 그대로 돌려줍니다. */
public class HttpVaccinationScheduleClient implements VaccinationSchedulePort {

    static final String TOKEN_HEADER = "X-Internal-Token";
    static final String SCHEDULE_PATH = "/internal/vaccinations/schedule";
    private static final String AI_UNKNOWN_DOSE_CODE = "VACCINATION_002";

    private static final Logger log = LoggerFactory.getLogger(HttpVaccinationScheduleClient.class);

    private final RestClient restClient;

    /** timeout은 builder에 설정된 request factory를 따릅니다. */
    public HttpVaccinationScheduleClient(RestClient.Builder builder, String baseUrl, String internalToken) {
        this.restClient = builder
                .baseUrl(baseUrl.replaceAll("/+$", ""))
                .defaultHeader(TOKEN_HEADER, internalToken)
                .build();
    }

    record ScheduleRequest(String birthDate, String asOf, List<String> completedDoses) {}

    @Override
    public JsonNode fetchSchedule(LocalDate birthDate, LocalDate asOf, List<String> completedDoses) {
        JsonNode body;
        try {
            body = restClient.post()
                    .uri(SCHEDULE_PATH)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(new ScheduleRequest(birthDate.toString(), asOf.toString(), completedDoses))
                    .retrieve()
                    .body(JsonNode.class);
        } catch (RestClientResponseException e) {
            JsonNode error = errorBody(e);
            String aiCode = error.path("code").asText(null);
            if (e.getStatusCode().value() == 400 && AI_UNKNOWN_DOSE_CODE.equals(aiCode)) {
                List<String> unknown = textValues(error.path("unknownDoseIds"));
                log.warn("예방접종 일정 요청 거절: 모르는 차수 포함, completedCount={}, unknownCount={}",
                        completedDoses.size(), unknown.size());
                throw new UnknownDoseException(unknown);
            }
            // 토큰·생년월일·차수 목록은 남기지 않고 상태 코드와 AI 오류 코드만 남깁니다.
            log.warn("예방접종 일정 요청 실패: status={}, aiCode={}", e.getStatusCode().value(), aiCode);
            throw unavailable();
        } catch (RestClientException e) {
            log.warn("예방접종 일정 요청 실패: error={}", e.getClass().getSimpleName());
            throw unavailable();
        }
        if (body == null || !body.isObject()) {
            log.warn("예방접종 일정 응답이 JSON 객체가 아닙니다.");
            throw unavailable();
        }
        return body;
    }

    /** 오류 본문 JSON입니다. 비었거나 JSON이 아니면 MissingNode라 path()가 안전합니다. */
    private static JsonNode errorBody(RestClientResponseException e) {
        try {
            JsonNode error = e.getResponseBodyAs(JsonNode.class);
            return error == null ? MissingNode.getInstance() : error;
        } catch (RuntimeException parseFailure) {
            return MissingNode.getInstance();
        }
    }

    /** 배열 안 문자열만 모읍니다. 배열이 아니면 빈 목록입니다. */
    private static List<String> textValues(JsonNode array) {
        List<String> values = new ArrayList<>();
        if (array.isArray()) {
            for (JsonNode value : array) {
                if (value.isTextual()) {
                    values.add(value.asText());
                }
            }
        }
        return values;
    }

    private static BusinessException unavailable() {
        return new BusinessException(VaccinationErrorCode.AI_UNAVAILABLE);
    }
}

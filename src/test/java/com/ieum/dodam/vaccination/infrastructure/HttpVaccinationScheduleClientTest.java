package com.ieum.dodam.vaccination.infrastructure;

import com.fasterxml.jackson.databind.JsonNode;
import com.ieum.dodam.global.exception.BusinessException;
import com.ieum.dodam.vaccination.domain.UnknownDoseException;
import com.ieum.dodam.vaccination.error.VaccinationErrorCode;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.client.ResponseCreator;
import org.springframework.web.client.RestClient;

import java.net.SocketTimeoutException;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withException;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

@ExtendWith(OutputCaptureExtension.class)
class HttpVaccinationScheduleClientTest {

    private static final String URL = "http://ai.test/internal/vaccinations/schedule";
    private static final LocalDate BIRTH = LocalDate.of(2026, 1, 15);
    private static final LocalDate AS_OF = LocalDate.of(2026, 11, 9);

    private MockRestServiceServer server;
    private HttpVaccinationScheduleClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        client = new HttpVaccinationScheduleClient(builder, "http://ai.test/", "test-token");
    }

    @Test
    void postsExactBodyWithTokenAndKeepsUnknownFields() {
        server.expect(requestTo(URL))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header(HttpVaccinationScheduleClient.TOKEN_HEADER, "test-token"))
                .andExpect(content().json("""
                        {"birthDate":"2026-01-15","asOf":"2026-11-09","completedDoses":["DTAP-1"]}
                        """, true))
                .andRespond(withSuccess("""
                        {"scheduleVersion":"2026.1","futureField":{"x":[1,2]},
                         "vaccines":[{"vaccineId":"DTAP","doses":[{"doseId":"DTAP-1","state":"DONE","extra":true}]}]}
                        """, MediaType.APPLICATION_JSON));

        JsonNode result = client.fetchSchedule(BIRTH, AS_OF, List.of("DTAP-1"));

        assertThat(result.path("futureField").path("x").get(1).asInt()).isEqualTo(2);
        assertThat(result.at("/vaccines/0/doses/0/extra").asBoolean()).isTrue();
        server.verify();
    }

    @Test
    void unknownDoseFromAiBecomesUnknownDose() {
        expectAndFail(withStatus(HttpStatus.BAD_REQUEST).contentType(MediaType.APPLICATION_JSON)
                        .body("{\"code\":\"VACCINATION_002\",\"message\":\"x\"}"),
                VaccinationErrorCode.UNKNOWN_DOSE);
    }

    @Test
    void unknownDoseIdsAreReadFromErrorBody() {
        assertUnknownDoseIds("{\"code\":\"VACCINATION_002\",\"unknownDoseIds\":[\"DTAP-9\",\"RV-7\"]}",
                List.of("DTAP-9", "RV-7"));
    }

    @Test
    void missingUnknownDoseIdsIsEmptyList() {
        assertUnknownDoseIds("{\"code\":\"VACCINATION_002\"}", List.of());
    }

    @Test
    void malformedUnknownDoseIdsIsEmptyOrTextOnly() {
        assertUnknownDoseIds("{\"code\":\"VACCINATION_002\",\"unknownDoseIds\":\"RV-7\"}", List.of());
        server.reset();
        assertUnknownDoseIds("{\"code\":\"VACCINATION_002\",\"unknownDoseIds\":[\"RV-7\",3,null]}",
                List.of("RV-7"));
    }

    private void assertUnknownDoseIds(String errorBody, List<String> expected) {
        server.expect(requestTo(URL)).andRespond(withStatus(HttpStatus.BAD_REQUEST)
                .contentType(MediaType.APPLICATION_JSON).body(errorBody));
        assertThatThrownBy(() -> client.fetchSchedule(BIRTH, AS_OF, List.of("RV-7")))
                .isInstanceOfSatisfying(UnknownDoseException.class,
                        e -> assertThat(e.unknownDoseIds()).isEqualTo(expected));
        server.verify();
    }

    @Test
    void otherBadRequestIsUnavailable() {
        expectAndFail(withStatus(HttpStatus.BAD_REQUEST).contentType(MediaType.APPLICATION_JSON)
                        .body("{\"code\":\"GLOBAL_002\"}"),
                VaccinationErrorCode.AI_UNAVAILABLE);
    }

    @Test
    void badRequestWithoutJsonIsUnavailable() {
        expectAndFail(withStatus(HttpStatus.BAD_REQUEST).body("oops"), VaccinationErrorCode.AI_UNAVAILABLE);
    }

    @Test
    void unauthorizedIsUnavailable() {
        expectAndFail(withStatus(HttpStatus.UNAUTHORIZED), VaccinationErrorCode.AI_UNAVAILABLE);
    }

    @Test
    void serverErrorIsUnavailable() {
        expectAndFail(withStatus(HttpStatus.INTERNAL_SERVER_ERROR), VaccinationErrorCode.AI_UNAVAILABLE);
    }

    @Test
    void timeoutIsUnavailable() {
        expectAndFail(withException(new SocketTimeoutException("timeout")), VaccinationErrorCode.AI_UNAVAILABLE);
    }

    @Test
    void emptyBodyIsUnavailable() {
        expectAndFail(withSuccess(), VaccinationErrorCode.AI_UNAVAILABLE);
    }

    @Test
    void nonObjectBodyIsUnavailable() {
        expectAndFail(withSuccess("[]", MediaType.APPLICATION_JSON), VaccinationErrorCode.AI_UNAVAILABLE);
    }

    @Test
    void failureLogHasNoTokenOrBirthDate(CapturedOutput output) {
        expectAndFail(withStatus(HttpStatus.INTERNAL_SERVER_ERROR), VaccinationErrorCode.AI_UNAVAILABLE);
        assertThat(output).contains("status=500").doesNotContain("test-token").doesNotContain("2026-01-15");
    }

    private void expectAndFail(ResponseCreator response, VaccinationErrorCode expected) {
        server.expect(requestTo(URL)).andRespond(response);
        assertThatThrownBy(() -> client.fetchSchedule(BIRTH, AS_OF, List.of()))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(expected));
        server.verify();
    }
}

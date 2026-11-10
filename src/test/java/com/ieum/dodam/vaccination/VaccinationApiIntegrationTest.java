package com.ieum.dodam.vaccination;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ieum.dodam.care_relation.command.application.CareRelationCommandService;
import com.ieum.dodam.global.auth.application.JwtService;
import com.ieum.dodam.global.exception.BusinessException;
import com.ieum.dodam.patient.domain.Patient;
import com.ieum.dodam.patient.domain.repository.PatientRepository;
import com.ieum.dodam.user.command.application.UserAccountService;
import com.ieum.dodam.user.domain.User;
import com.ieum.dodam.user.domain.UserRole;
import com.ieum.dodam.vaccination.domain.UnknownDoseException;
import com.ieum.dodam.vaccination.domain.VaccinationSchedulePort;
import com.ieum.dodam.vaccination.domain.entity.VaccinationDoseCheck;
import com.ieum.dodam.vaccination.domain.repository.VaccinationDoseCheckRepository;
import com.ieum.dodam.vaccination.error.VaccinationErrorCode;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

/**
 * 예방접종 API 통합 테스트입니다. AI 포트는 가짜로 바꿉니다.
 * 기존 WelfareExplanationApiController가 dodam.ai.base-url 없이 기동에 실패하므로 이 클래스에서만 가짜 값을 넣습니다.
 */
@SpringBootTest(properties = {
        "dodam.ai.base-url=http://ai.invalid",
        "dodam.ai.internal-token=test-token"
})
@AutoConfigureMockMvc
@Transactional
class VaccinationApiIntegrationTest {

    private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");
    private static final LocalDate BIRTH = LocalDate.of(2026, 1, 15);
    private static final ObjectMapper JSON = new ObjectMapper();

    @Autowired MockMvc mvc;
    @Autowired UserAccountService accounts;
    @Autowired JwtService jwt;
    @Autowired PatientRepository patients;
    @Autowired VaccinationDoseCheckRepository checks;
    @Autowired CareRelationCommandService cares;
    @MockBean VaccinationSchedulePort schedules;

    User owner;
    Patient patient;
    UUID patientId;
    String token;

    @BeforeEach
    void setup() throws Exception {
        owner = accounts.chooseRole(
                accounts.loginOrRegister(UUID.randomUUID().toString(), "접종 테스트").getKakaoId(), UserRole.PATIENT);
        patient = patients.findByUserId(owner.getId()).orElseThrow();
        patient.setBirthDate(BIRTH);
        patients.saveAndFlush(patient);
        patientId = patient.getId();
        token = jwt.create(owner);
        when(schedules.fetchSchedule(any(), any(), anyList())).thenReturn(schedule("DTAP-1", "DTAP-2"));
    }

    @Test
    void scheduleSendsStoredChecksAndSeoulTodayAndPassesUnknownFieldsThrough() throws Exception {
        checks.saveAndFlush(VaccinationDoseCheck.create(patientId, "DTAP-1", owner.getId()));
        when(schedules.fetchSchedule(any(), any(), anyList())).thenReturn(JSON.readTree("""
                {"scheduleVersion":"2026.1","futureField":{"nested":[7]},
                 "vaccines":[{"vaccineId":"DTAP","doses":[{"doseId":"DTAP-1","state":"DONE"}]}]}
                """));

        LocalDate before = LocalDate.now(SEOUL);
        mvc.perform(authed(get("/api/vaccinations")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.futureField.nested[0]").value(7))
                .andExpect(jsonPath("$.vaccines[0].doses[0].state").value("DONE"));
        LocalDate after = LocalDate.now(SEOUL);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<String>> doses = ArgumentCaptor.forClass(List.class);
        ArgumentCaptor<LocalDate> asOf = ArgumentCaptor.forClass(LocalDate.class);
        verify(schedules).fetchSchedule(eq(BIRTH), asOf.capture(), doses.capture());
        assertThat(doses.getValue()).containsExactly("DTAP-1");
        assertThat(asOf.getValue()).isIn(before, after);
    }

    @Test
    void repeatedCheckKeepsOneRowAndUncheckIsIdempotent() throws Exception {
        for (int i = 0; i < 2; i++) {
            mvc.perform(authed(put("/api/vaccinations/checks/DTAP-1"))).andExpect(status().isNoContent());
        }
        assertThat(checks.findDoseIds(patientId)).containsExactly("DTAP-1");

        mvc.perform(authed(delete("/api/vaccinations/checks/DTAP-1"))).andExpect(status().isNoContent());
        mvc.perform(authed(delete("/api/vaccinations/checks/DTAP-1"))).andExpect(status().isNoContent());
        assertThat(checks.findDoseIds(patientId)).isEmpty();
    }

    @Test
    void unknownDoseIsRejectedAndNotStored() throws Exception {
        mvc.perform(authed(put("/api/vaccinations/checks/XYZ-9")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(VaccinationErrorCode.UNKNOWN_DOSE.code()));
        assertThat(checks.findDoseIds(patientId)).isEmpty();
    }

    @Test
    void malformedDoseIdIsBadRequest() throws Exception {
        for (String bad : new String[] {"dtap-1", "DTAP_1", "A".repeat(49) + "-1"}) {
            mvc.perform(authed(put("/api/vaccinations/checks/" + bad)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value(VaccinationErrorCode.INVALID_DOSE_ID.code()));
            mvc.perform(authed(delete("/api/vaccinations/checks/" + bad)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value(VaccinationErrorCode.INVALID_DOSE_ID.code()));
        }
    }

    @Test
    void missingBirthDateIsBadRequestWithoutAiCall() throws Exception {
        patient.setBirthDate(null);
        patients.saveAndFlush(patient);

        mvc.perform(authed(get("/api/vaccinations")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(VaccinationErrorCode.BIRTH_DATE_REQUIRED.code()));
        mvc.perform(authed(put("/api/vaccinations/checks/DTAP-1")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(VaccinationErrorCode.BIRTH_DATE_REQUIRED.code()));
        verify(schedules, never()).fetchSchedule(any(), any(), anyList());
    }

    @Test
    void futureBirthDateIsBadRequest() throws Exception {
        patient.setBirthDate(LocalDate.now(SEOUL).plusDays(2));
        patients.saveAndFlush(patient);

        mvc.perform(authed(get("/api/vaccinations")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(VaccinationErrorCode.BIRTH_DATE_IN_FUTURE.code()));
    }

    @Test
    void aiFailureIs503ForScheduleAndCheck() throws Exception {
        when(schedules.fetchSchedule(any(), any(), anyList()))
                .thenThrow(new BusinessException(VaccinationErrorCode.AI_UNAVAILABLE));

        mvc.perform(authed(get("/api/vaccinations")))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value(VaccinationErrorCode.AI_UNAVAILABLE.code()));
        mvc.perform(authed(put("/api/vaccinations/checks/DTAP-1")))
                .andExpect(status().isServiceUnavailable());
        assertThat(checks.findDoseIds(patientId)).isEmpty();
        // 해제는 AI를 부르지 않으므로 AI 장애 중에도 된다.
        mvc.perform(authed(delete("/api/vaccinations/checks/DTAP-1"))).andExpect(status().isNoContent());
    }

    @Test
    void storedDoseUnknownToAiIsSkippedInsteadOfBreakingSchedule() throws Exception {
        checks.saveAndFlush(VaccinationDoseCheck.create(patientId, "DTAP-1", owner.getId()));
        checks.saveAndFlush(VaccinationDoseCheck.create(patientId, "OLD-1", owner.getId()));
        when(schedules.fetchSchedule(any(), any(), anyList())).thenAnswer(inv -> {
            List<String> doses = inv.getArgument(2);
            if (doses.contains("OLD-1")) {
                throw new UnknownDoseException(List.of("OLD-1"));
            }
            return doses.isEmpty() ? schedule("DTAP-1") : JSON.readTree("{\"marker\":\"final\"}");
        });

        mvc.perform(authed(get("/api/vaccinations")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.marker").value("final"));
        verify(schedules).fetchSchedule(eq(BIRTH), any(), eq(List.of("DTAP-1")));
        assertThat(checks.findDoseIds(patientId)).containsExactly("DTAP-1", "OLD-1");
    }

    @Test
    void linkedGuardianCanCheck() throws Exception {
        User guardian = caregiver("연결 보호자");
        var relation = cares.request(guardian, accounts.patientInviteCode(owner));
        cares.accept(owner, relation.getId());

        mvc.perform(put("/api/vaccinations/checks/DTAP-2")
                        .param("patientId", patientId.toString())
                        .header("Authorization", "Bearer " + jwt.create(guardian)))
                .andExpect(status().isNoContent());
        assertThat(checks.findDoseIds(patientId)).containsExactly("DTAP-2");
    }

    @Test
    void unrelatedGuardianCannotReadCheckOrUncheck() throws Exception {
        checks.saveAndFlush(VaccinationDoseCheck.create(patientId, "DTAP-1", owner.getId()));
        String other = jwt.create(caregiver("외부 보호자"));

        for (MockHttpServletRequestBuilder request : List.of(
                get("/api/vaccinations"),
                put("/api/vaccinations/checks/DTAP-2"),
                delete("/api/vaccinations/checks/DTAP-1"))) {
            mvc.perform(request.param("patientId", patientId.toString()).header("Authorization", "Bearer " + other))
                    .andExpect(status().isForbidden());
        }
        assertThat(checks.findDoseIds(patientId)).containsExactly("DTAP-1");
        verify(schedules, never()).fetchSchedule(any(), any(), anyList());
    }

    private MockHttpServletRequestBuilder authed(MockHttpServletRequestBuilder request) {
        return request.param("patientId", patientId.toString()).header("Authorization", "Bearer " + token);
    }

    private User caregiver(String name) {
        return accounts.chooseRole(
                accounts.loginOrRegister(UUID.randomUUID().toString(), name).getKakaoId(), UserRole.CAREGIVER);
    }

    private static JsonNode schedule(String... doseIds) {
        var doses = JSON.createArrayNode();
        for (String id : doseIds) {
            doses.addObject().put("doseId", id);
        }
        var root = JSON.createObjectNode();
        root.putArray("vaccines").addObject().set("doses", doses);
        return root;
    }
}

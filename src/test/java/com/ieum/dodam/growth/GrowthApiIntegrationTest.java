package com.ieum.dodam.growth;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ieum.dodam.global.auth.application.JwtService;
import com.ieum.dodam.patient.domain.repository.PatientRepository;
import com.ieum.dodam.user.command.application.UserAccountService;
import com.ieum.dodam.user.domain.*;

import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.util.*;

@SpringBootTest(properties = "dodam.schedule.reminders-enabled=false")
@AutoConfigureMockMvc
@Transactional
class GrowthApiIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired UserAccountService accounts;
    @Autowired JwtService jwt;
    @Autowired PatientRepository patients;
    @Autowired ObjectMapper json;
    @Autowired com.ieum.dodam.care_relation.command.application.CareRelationCommandService cares;
    User owner;
    String token;
    UUID patientId;

    @BeforeEach
    void setup() {
        owner =
                accounts.chooseRole(
                        accounts.loginOrRegister(UUID.randomUUID().toString(), "성장 검증")
                                .getKakaoId(),
                        UserRole.PATIENT);
        token = jwt.create(owner);
        var patient = patients.findByUserId(owner.getId()).orElseThrow();
        patient.setBirthDate(LocalDate.of(2024, 1, 1));
        patientId = patient.getId();
    }

    String body(String date, String height, String weight, int version) {
        return "{\"patientId\":\""
                + patientId
                + "\",\"measuredOn\":\""
                + date
                + "\",\"heightCm\":"
                + height
                + ",\"weightKg\":"
                + weight
                + ",\"version\":"
                + version
                + "}";
    }

    String create(String date) throws Exception {
        String result =
                mvc.perform(
                                post("/api/growth-records")
                                        .header("Authorization", "Bearer " + token)
                                        .contentType("application/json")
                                        .content(body(date, "85.2", "12.3", 0)))
                        .andExpect(status().isCreated())
                        .andReturn()
                        .getResponse()
                        .getContentAsString();
        return json.readTree(result).get("id").asText();
    }

    @Test
    void missingAndDeletedRecordsReturnCommonNotFoundResponse() throws Exception {
        for (String id : List.of(UUID.randomUUID().toString(), create("2026-10-02"))) {
            if (!id.isBlank()) {
                // Missing IDs and already removed IDs use the same error envelope.
                var existing = mvc.perform(delete("/api/growth-records/" + id)
                        .param("version", "0").header("Authorization", "Bearer " + token));
                int result = existing.andReturn().getResponse().getStatus();
                org.assertj.core.api.Assertions.assertThat(result).isIn(204, 404);
            }
            mvc.perform(delete("/api/growth-records/" + id).param("version", "0")
                    .header("Authorization", "Bearer " + token))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value("GROWTH_001"))
                    .andExpect(jsonPath("$.message").value("성장 기록을 찾을 수 없어요."));
        }
    }

    @Test
    void createsListsEditsAndSoftDeletesWithConflictProtection() throws Exception {
        String id = create("2026-10-01");
        create("2026-09-01");
        mvc.perform(
                        get("/api/growth-records")
                                .param("patientId", patientId.toString())
                                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].measuredOn").value("2026-10-01"))
                .andExpect(jsonPath("$.length()").value(2));
        mvc.perform(
                        put("/api/growth-records/" + id)
                                .header("Authorization", "Bearer " + token)
                                .contentType("application/json")
                                .content(body("2026-10-01", "86.1", "null", 0)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.heightCm").value(86.1))
                .andExpect(jsonPath("$.version").value(1));
        mvc.perform(
                        put("/api/growth-records/" + id)
                                .header("Authorization", "Bearer " + token)
                                .contentType("application/json")
                                .content(body("2026-10-01", "87", "13", 0)))
                .andExpect(status().isConflict());
        mvc.perform(
                        delete("/api/growth-records/" + id)
                                .param("version", "0")
                                .header("Authorization", "Bearer " + token))
                .andExpect(status().isConflict());
        mvc.perform(
                        delete("/api/growth-records/" + id)
                                .param("version", "1")
                                .header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());
        mvc.perform(
                        get("/api/growth-records")
                                .param("patientId", patientId.toString())
                                .header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.length()").value(1));
        create("2026-10-01");
    }

    @Test
    void rejectsMissingInvalidFutureAndBeforeBirthMeasurements() throws Exception {
        for (String invalid :
                List.of(
                        body("2026-10-01", "null", "null", 0),
                        body("2026-10-01", "0", "12", 0),
                        body("2026-10-01", "85.22", "12", 0),
                        body("2023-12-31", "85", "12", 0),
                        body(
                                LocalDate.now(ZoneId.of("Asia/Seoul")).plusDays(1).toString(),
                                "85",
                                "12",
                                0))) {
            mvc.perform(
                            post("/api/growth-records")
                                    .header("Authorization", "Bearer " + token)
                                    .contentType("application/json")
                                    .content(invalid))
                    .andExpect(status().isBadRequest());
        }
        create("2026-10-01");
        mvc.perform(
                        post("/api/growth-records")
                                .header("Authorization", "Bearer " + token)
                                .contentType("application/json")
                                .content(body("2026-10-01", "88", "14", 0)))
                .andExpect(status().isConflict());
    }

    @Test
    void forbidsUnrelatedUsersAndAnonymousRequests() throws Exception {
        String id = create("2026-10-01");
        User stranger =
                accounts.chooseRole(
                        accounts.loginOrRegister(UUID.randomUUID().toString(), "다른 계정")
                                .getKakaoId(),
                        UserRole.PATIENT);
        String other = jwt.create(stranger);
        mvc.perform(
                        get("/api/growth-records")
                                .param("patientId", patientId.toString())
                                .header("Authorization", "Bearer " + other))
                .andExpect(status().isForbidden());
        mvc.perform(
                        post("/api/growth-records")
                                .header("Authorization", "Bearer " + other)
                                .contentType("application/json")
                                .content(body("2026-09-01", "85", "12", 0)))
                .andExpect(status().isForbidden());
        mvc.perform(
                        put("/api/growth-records/" + id)
                                .header("Authorization", "Bearer " + other)
                                .contentType("application/json")
                                .content(body("2026-10-01", "85", "12", 0)))
                .andExpect(status().isForbidden());
        mvc.perform(
                        delete("/api/growth-records/" + id)
                                .param("version", "0")
                                .header("Authorization", "Bearer " + other))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/growth-records").param("patientId", patientId.toString()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void activeGuardianCanManageRecordsButRevokedGuardianCannot() throws Exception {
        String id = create("2026-10-01");
        User guardian =
                accounts.chooseRole(
                        accounts.loginOrRegister(UUID.randomUUID().toString(), "가족").getKakaoId(),
                        UserRole.CAREGIVER);
        String familyToken = jwt.create(guardian);
        var relation = cares.request(guardian, accounts.patientInviteCode(owner));
        cares.accept(owner, relation.getId());
        mvc.perform(
                        get("/api/growth-records")
                                .param("patientId", patientId.toString())
                                .header("Authorization", "Bearer " + familyToken))
                .andExpect(status().isOk());
        mvc.perform(
                        put("/api/growth-records/" + id)
                                .header("Authorization", "Bearer " + familyToken)
                                .contentType("application/json")
                                .content(body("2026-10-01", "86", "13", 0)))
                .andExpect(status().isOk());
        cares.revoke(owner, relation.getId());
        mvc.perform(
                        get("/api/growth-records")
                                .param("patientId", patientId.toString())
                                .header("Authorization", "Bearer " + familyToken))
                .andExpect(status().isForbidden());
        mvc.perform(
                        delete("/api/growth-records/" + id)
                                .param("version", "1")
                                .header("Authorization", "Bearer " + familyToken))
                .andExpect(status().isForbidden());
    }
}

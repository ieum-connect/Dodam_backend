package com.ieum.dodam.health_record.api;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ieum.dodam.care_relation.command.application.CareRelationCommandService;
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

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class HealthRecordApiIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired UserAccountService accounts;
    @Autowired JwtService jwt;
    @Autowired ObjectMapper json;
    @Autowired PatientRepository patients;
    @Autowired CareRelationCommandService cares;
    User personal;
    String token;

    @BeforeEach
    void setup() {
        personal =
                accounts.chooseRole(
                        accounts.loginOrRegister("http-" + java.util.UUID.randomUUID(), "본인")
                                .getKakaoId(),
                        UserRole.PATIENT);
        token = jwt.create(personal);
    }

    String record(String body) throws Exception {
        var result =
                mvc.perform(
                                post("/api/health-records")
                                        .header("Authorization", "Bearer " + token)
                                        .contentType("application/json")
                                        .content(
                                                json.writeValueAsString(
                                                        java.util.Map.of("body", body))))
                        .andExpect(status().isCreated())
                        .andExpect(jsonPath("$.authorUserId").value(personal.getId().toString()))
                        .andReturn();
        return json.readTree(result.getResponse().getContentAsString()).get("recordId").asText();
    }

    @Test
    void crudAndSoftDeleteThroughHttp() throws Exception {
        String id = record("내 기록");
        mvc.perform(
                        put("/api/health-records/" + id)
                                .header("Authorization", "Bearer " + token)
                                .contentType("application/json")
                                .content(
                                        "{\"body\":\"대상 변경\",\"patientId\":\""
                                                + java.util.UUID.randomUUID()
                                                + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("GLOBAL_002"));
        mvc.perform(get("/api/health-records/" + id).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.body").value("내 기록"));
        mvc.perform(get("/api/health-records").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].recordId").value(id));
        mvc.perform(
                        put("/api/health-records/" + id)
                                .header("Authorization", "Bearer " + token)
                                .contentType("application/json")
                                .content("{\"body\":\"수정 기록\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.body").value("수정 기록"));
        mvc.perform(delete("/api/health-records/" + id).header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());
        mvc.perform(get("/api/health-records/" + id).header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
        mvc.perform(get("/api/health-records").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isEmpty());
    }

    @Test
    void chosenRecordedAtIsStoredAndCanBeUpdated() throws Exception {
        var created =
                mvc.perform(
                                post("/api/health-records")
                                        .header("Authorization", "Bearer " + token)
                                        .contentType("application/json")
                                        .content(
                                                "{\"body\":\"기록 시간 테스트\","
                                                        + "\"recordedAt\":\"2026-10-24T00:30:00Z\"}"))
                        .andExpect(status().isCreated())
                        .andExpect(jsonPath("$.recordedAt").value("2026-10-24T00:30:00Z"))
                        .andReturn();
        String id = json.readTree(created.getResponse().getContentAsString()).get("recordId").asText();

        mvc.perform(
                        put("/api/health-records/" + id)
                                .header("Authorization", "Bearer " + token)
                                .contentType("application/json")
                                .content(
                                        "{\"body\":\"시간 수정\","
                                                + "\"recordedAt\":\"2026-10-24T03:45:00Z\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.recordedAt").value("2026-10-24T03:45:00Z"));
    }

    @Test
    void unauthenticatedAndInvalidInputsAreRejected() throws Exception {
        mvc.perform(get("/api/health-records")).andExpect(status().isUnauthorized());
        mvc.perform(
                        post("/api/health-records")
                                .header("Authorization", "Bearer " + token)
                                .contentType("application/json")
                                .content("{\"body\":\"  \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("GLOBAL_002"));
        mvc.perform(
                        post("/api/health-records")
                                .header("Authorization", "Bearer " + token)
                                .contentType("application/json")
                                .content(
                                        json.writeValueAsString(
                                                java.util.Map.of("body", "가".repeat(2001)))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("GLOBAL_002"));
        mvc.perform(get("/api/health-records?size=101").header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("GLOBAL_002"));
        mvc.perform(
                        get("/api/health-records?cursor=wrong")
                                .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("GLOBAL_002"));
        mvc.perform(
                        get("/api/health-records/not-a-uuid")
                                .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("GLOBAL_002"));
    }

    @Test
    void personalEditingProxyRecordKeepsOriginalAuthorAfterUnlink() throws Exception {
        User caregiver =
                accounts.chooseRole(
                        accounts.loginOrRegister("proxy-" + java.util.UUID.randomUUID(), "원래 보호자")
                                .getKakaoId(),
                        UserRole.CAREGIVER);
        var relation = cares.request(caregiver, accounts.patientInviteCode(personal));
        cares.accept(personal, relation.getId());
        String patientId = patients.findByUserId(personal.getId()).orElseThrow().getId().toString();
        var created =
                mvc.perform(
                                post("/api/health-records")
                                        .header("Authorization", "Bearer " + jwt.create(caregiver))
                                        .contentType("application/json")
                                        .content(
                                                json.writeValueAsString(
                                                        java.util.Map.of(
                                                                "body",
                                                                "보호자가 작성",
                                                                "patientId",
                                                                patientId))))
                        .andExpect(status().isCreated())
                        .andReturn();
        String id =
                json.readTree(created.getResponse().getContentAsString()).get("recordId").asText();
        cares.revoke(personal, relation.getId());
        mvc.perform(
                        put("/api/health-records/" + id)
                                .header("Authorization", "Bearer " + token)
                                .contentType("application/json")
                                .content("{\"body\":\"본인이 수정\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.body").value("본인이 수정"))
                .andExpect(jsonPath("$.authorUserId").value(caregiver.getId().toString()))
                .andExpect(jsonPath("$.authorName").value("원래 보호자"))
                .andExpect(jsonPath("$.proxyWritten").value(true));
    }

    @Test
    void bodyAndPathUuidUseSameCommonInputError() throws Exception {
        mvc.perform(
                        get("/api/health-records/not-a-uuid")
                                .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("GLOBAL_002"));
        mvc.perform(
                        post("/api/health-records")
                                .header("Authorization", "Bearer " + token)
                                .contentType("application/json")
                                .content("{\"body\":\"기록\",\"patientId\":\"not-a-uuid\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("GLOBAL_002"));
    }

    @Test
    void activeCaregiverMayManagePersonalRecordButRevokedCannot() throws Exception {
        String id = record("본인 기록");
        User caregiver =
                accounts.chooseRole(
                        accounts.loginOrRegister("other-" + java.util.UUID.randomUUID(), "보호자")
                                .getKakaoId(),
                        UserRole.CAREGIVER);
        var relation = cares.request(caregiver, accounts.patientInviteCode(personal));
        String caregiverToken = jwt.create(caregiver);
        mvc.perform(
                        get("/api/health-records/" + id)
                                .header("Authorization", "Bearer " + caregiverToken))
                .andExpect(status().isForbidden());
        cares.accept(personal, relation.getId());
        mvc.perform(
                        get("/api/health-records/" + id)
                                .header("Authorization", "Bearer " + caregiverToken))
                .andExpect(status().isOk());
        mvc.perform(
                        put("/api/health-records/" + id)
                                .header("Authorization", "Bearer " + caregiverToken)
                                .contentType("application/json")
                                .content("{\"body\":\"보호자가 수정\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.authorUserId").value(personal.getId().toString()))
                .andExpect(jsonPath("$.authorName").value("본인"))
                .andExpect(jsonPath("$.proxyWritten").value(false));
        String deletedId = record("삭제할 본인 기록");
        mvc.perform(
                        delete("/api/health-records/" + deletedId)
                                .header("Authorization", "Bearer " + caregiverToken))
                .andExpect(status().isNoContent());
        cares.revoke(personal, relation.getId());
        mvc.perform(
                        get("/api/health-records/" + id)
                                .header("Authorization", "Bearer " + caregiverToken))
                .andExpect(status().isForbidden());
        mvc.perform(
                        put("/api/health-records/" + id)
                                .header("Authorization", "Bearer " + caregiverToken)
                                .contentType("application/json")
                                .content("{\"body\":\"차단\"}"))
                .andExpect(status().isForbidden());
        mvc.perform(
                        delete("/api/health-records/" + id)
                                .header("Authorization", "Bearer " + caregiverToken))
                .andExpect(status().isForbidden());
    }
}

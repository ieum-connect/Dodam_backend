package com.ieum.dodam.patient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ieum.dodam.global.auth.application.JwtService;
import com.ieum.dodam.patient.domain.repository.PatientRepository;
import com.ieum.dodam.user.command.application.UserAccountService;
import com.ieum.dodam.user.domain.UserRole;
import jakarta.persistence.EntityManager;
import java.util.UUID;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(properties = "dodam.schedule.reminders-enabled=false")
@AutoConfigureMockMvc
@Transactional
class ChildGenderIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired UserAccountService accounts;
    @Autowired JwtService jwt;
    @Autowired PatientRepository patients;
    @Autowired EntityManager em;
    @Autowired ObjectMapper json;
    String token;

    @BeforeEach
    void setup() {
        var user = accounts.chooseRole(accounts.loginOrRegister(UUID.randomUUID().toString(), "보호자").getKakaoId(), UserRole.CAREGIVER);
        token = "Bearer " + jwt.create(user);
    }

    @Test
    void savesBothGendersAndReturnsThemOnList() throws Exception {
        for (String gender : new String[]{"MALE", "FEMALE"}) {
            String response = mvc.perform(post("/api/children").header("Authorization", token)
                    .contentType("application/json").content(body(",\"gender\":\"" + gender + "\"")))
                    .andExpect(status().isCreated()).andExpect(jsonPath("$.gender").value(gender))
                    .andReturn().getResponse().getContentAsString();
            UUID id = UUID.fromString(json.readTree(response).get("id").asText());
            em.flush(); em.clear();
            assertThat(patients.findById(id).orElseThrow().getGender()).isEqualTo(gender);
        }
        mvc.perform(get("/api/children").header("Authorization", token))
                .andExpect(status().isOk()).andExpect(jsonPath("$[*].gender", org.hamcrest.Matchers.containsInAnyOrder("MALE", "FEMALE")));
    }

    @Test
    void olderClientCanCreateAndDoesNotEraseGenderOnUpdate() throws Exception {
        String response = mvc.perform(post("/api/children").header("Authorization", token)
                .contentType("application/json").content(body("")))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        String id = json.readTree(response).get("id").asText();
        mvc.perform(put("/api/children/" + id).header("Authorization", token)
                .contentType("application/json").content(body(",\"gender\":\"FEMALE\"")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.gender").value("FEMALE"));
        mvc.perform(put("/api/children/" + id).header("Authorization", token)
                .contentType("application/json").content(body("")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.gender").value("FEMALE"));
    }

    @Test
    void rejectsUnknownGender() throws Exception {
        mvc.perform(post("/api/children").header("Authorization", token)
                .contentType("application/json").content(body(",\"gender\":\"invalid\"")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void editsOwnProfileAndRejectsEmptyName() throws Exception {
        mvc.perform(put("/api/me/profile").header("Authorization", token)
                .contentType("application/json").content("{\"name\":\"  수정한 보호자  \"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.name").value("수정한 보호자"));
        em.flush(); em.clear();
        mvc.perform(get("/api/me").header("Authorization", token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.name").value("수정한 보호자"));
        mvc.perform(put("/api/me/profile").header("Authorization", token)
                .contentType("application/json").content("{\"name\":\"  \"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void newChildHasNoGrowthOrMedicationFromSiblingAndStrangerCannotEdit() throws Exception {
        String first = mvc.perform(post("/api/children").header("Authorization", token)
                .contentType("application/json").content(body("")))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        String firstId = json.readTree(first).get("id").asText();
        mvc.perform(post("/api/medication-series").header("Authorization", token)
                .contentType("application/json").content(json.writeValueAsString(java.util.Map.of(
                        "patientId", firstId, "requestId", UUID.randomUUID().toString(),
                        "title", "첫째 복약", "startsAt", java.time.Instant.now().plusSeconds(3600).toString(),
                        "timeZone", "Asia/Seoul"))))
                .andExpect(status().isOk());
        mvc.perform(get("/api/medication-series").param("patientId", firstId).header("Authorization", token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1));
        mvc.perform(post("/api/growth-records").header("Authorization", token)
                .contentType("application/json").content("{\"patientId\":\""+firstId+"\",\"measuredOn\":\"2025-01-01\",\"heightCm\":80,\"weightKg\":10}"))
                .andExpect(status().isCreated());
        String second = mvc.perform(post("/api/children").header("Authorization", token)
                .contentType("application/json").content(body("")))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        String secondId = json.readTree(second).get("id").asText();
        mvc.perform(get("/api/growth-records").param("patientId", secondId).header("Authorization", token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(0));
        mvc.perform(get("/api/medication-series").param("patientId", secondId).header("Authorization", token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(0));
        var other = accounts.chooseRole(accounts.loginOrRegister(UUID.randomUUID().toString(), "다른 보호자").getKakaoId(), UserRole.CAREGIVER);
        mvc.perform(put("/api/children/"+firstId).header("Authorization", "Bearer "+jwt.create(other))
                .contentType("application/json").content(body("")))
                .andExpect(status().isForbidden());
    }

    private String body(String gender) {
        return "{\"name\":\"박도윤\",\"birthDate\":\"2024-01-01\"" + gender + "}";
    }
}

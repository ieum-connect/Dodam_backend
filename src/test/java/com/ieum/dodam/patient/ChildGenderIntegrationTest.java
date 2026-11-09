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

    private String body(String gender) {
        return "{\"name\":\"박도윤\",\"birthDate\":\"2024-01-01\"" + gender + "}";
    }
}

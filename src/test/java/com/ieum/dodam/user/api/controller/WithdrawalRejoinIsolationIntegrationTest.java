package com.ieum.dodam.user.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ieum.dodam.global.auth.application.JwtService;
import com.ieum.dodam.global.auth.oauth.KakaoUnlinkClient;
import com.ieum.dodam.user.command.application.UserAccountService;
import com.ieum.dodam.user.domain.UserRole;
import jakarta.persistence.EntityManager;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties="dodam.schedule.reminders-enabled=false")
@AutoConfigureMockMvc
@Transactional
class WithdrawalRejoinIsolationIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired UserAccountService accounts;
    @Autowired JwtService jwt;
    @Autowired ObjectMapper json;
    @Autowired EntityManager em;
    @MockBean KakaoUnlinkClient kakao;

    @Test
    void rejoiningSameKakaoAccountCannotRestoreChildrenRecordsOrOldToken() throws Exception {
        String kakaoId="withdraw-rejoin-"+UUID.randomUUID();
        var owner=accounts.chooseRole(accounts.loginOrRegister(kakaoId,"이전 보호자").getKakaoId(),UserRole.CAREGIVER);
        var oldId=owner.getId();
        String oldToken="Bearer "+jwt.create(owner);
        var child=mvc.perform(post("/api/children").header("Authorization",oldToken).contentType("application/json")
            .content("{\"name\":\"이전 아이\",\"birthDate\":\"2024-01-01\",\"gender\":\"MALE\"}"))
            .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        String childId=json.readTree(child).get("id").asText();
        String code=json.readTree(child).get("inviteCode").asText();
        var record=mvc.perform(post("/api/health-records").header("Authorization",oldToken).contentType("application/json")
            .content(json.writeValueAsString(Map.of("patientId",childId,"body","탈퇴 전 비공개 메모"))))
            .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        String recordId=json.readTree(record).get("recordId").asText();
        mvc.perform(delete("/api/me").header("Authorization",oldToken).contentType("application/json").content("{\"confirmed\":true}"))
            .andExpect(status().is2xxSuccessful());
        verify(kakao).unlink(kakaoId);
        em.flush(); em.clear();
        var rejoined=accounts.loginOrRegister(kakaoId,"새 보호자");
        assertThat(rejoined.getId()).isNotEqualTo(oldId);
        assertThat(rejoined.getRole()).isEqualTo(UserRole.PENDING);
        rejoined=accounts.chooseRole(kakaoId,UserRole.CAREGIVER);
        String fresh="Bearer "+jwt.create(rejoined);
        mvc.perform(get("/api/me").header("Authorization",oldToken)).andExpect(status().is4xxClientError());
        mvc.perform(get("/api/children").header("Authorization",fresh)).andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(0));
        for(String path:new String[]{"health-records","visits","documents","growth-records","memo-tags","medication-series","action-items"}) {
            mvc.perform(get("/api/"+path).param("patientId",childId).header("Authorization",fresh)).andExpect(status().is4xxClientError());
        }
        String recordPath = "/api/health-records/" + recordId;
        mvc.perform(put(recordPath).header("Authorization", fresh).contentType("application/json")
                .content(json.writeValueAsString(Map.of("patientId", childId, "body", "재가입 후 수정 시도"))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("HEALTH_RECORD_002"));
        mvc.perform(delete(recordPath).header("Authorization", fresh))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("HEALTH_RECORD_002"));
        mvc.perform(post("/api/children/join").header("Authorization",fresh).contentType("application/json")
            .content(json.writeValueAsString(Map.of("inviteCode",code)))).andExpect(status().is4xxClientError());
    }
}

package com.ieum.dodam.schedule;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.ieum.dodam.action_item.domain.*;
import com.ieum.dodam.action_item.domain.entity.ActionItem;
import com.ieum.dodam.action_item.domain.repository.ActionItemRepository;
import com.ieum.dodam.global.auth.application.JwtService;
import com.ieum.dodam.medical_document.domain.MedicalDocumentDeletedEvent;
import com.ieum.dodam.medical_document.domain.entity.MedicalDocument;
import com.ieum.dodam.medical_document.domain.repository.MedicalDocumentRepository;
import com.ieum.dodam.patient.domain.repository.PatientRepository;
import com.ieum.dodam.schedule.domain.repository.ScheduleRepository;
import com.ieum.dodam.user.command.application.UserAccountService;
import com.ieum.dodam.user.domain.*;
import com.ieum.dodam.visit.domain.*;
import com.ieum.dodam.visit.domain.entity.Visit;
import com.ieum.dodam.visit.domain.repository.VisitRepository;

import jakarta.persistence.EntityManager;

import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.util.UUID;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class CalendarApiIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired UserAccountService accounts;
    @Autowired JwtService jwt;
    @Autowired PatientRepository patients;
    @Autowired VisitRepository visits;
    @Autowired MedicalDocumentRepository documents;
    @Autowired ActionItemRepository actions;
    @Autowired ScheduleRepository schedules;
    @Autowired EntityManager em;
    @Autowired ApplicationEventPublisher events;
    @Autowired com.ieum.dodam.care_relation.command.application.CareRelationCommandService cares;
    User owner;
    UUID patientId;
    String token;
    MedicalDocument document;
    ActionItem candidate;

    @BeforeEach
    void setup() throws Exception {
        String key = UUID.randomUUID().toString();
        owner =
                accounts.chooseRole(
                        accounts.loginOrRegister(key, "일정 테스트").getKakaoId(), UserRole.PATIENT);
        patientId = patients.findByUserId(owner.getId()).orElseThrow().getId();
        token = jwt.create(owner);
        var visit =
                visits.saveAndFlush(
                        Visit.create(
                                patientId,
                                LocalDate.now(),
                                "테스트 병원",
                                null,
                                null,
                                VisitStatus.DONE));
        document =
                documents.saveAndFlush(
                        MedicalDocument.uploaded(
                                patientId,
                                visit.getId(),
                                owner.getId(),
                                "test.png",
                                "test",
                                "image/png",
                                1,
                                "test"));
        var constructor = ActionItem.class.getDeclaredConstructor();
        constructor.setAccessible(true);
        candidate = constructor.newInstance();
        ReflectionTestUtils.setField(candidate, "id", UUID.randomUUID());
        ReflectionTestUtils.setField(candidate, "visitId", visit.getId());
        ReflectionTestUtils.setField(candidate, "documentId", document.getId());
        ReflectionTestUtils.setField(candidate, "type", ActionItemType.REVISIT);
        ReflectionTestUtils.setField(candidate, "title", "재검진 권고");
        ReflectionTestUtils.setField(candidate, "approvalStatus", ActionItemApprovalStatus.PENDING);
        ReflectionTestUtils.setField(candidate, "createdAt", Instant.now());
        actions.saveAndFlush(candidate);
    }

    @Test
    void approvalRequiresDateAndRepeatedApprovalCreatesOneSchedule() throws Exception {
        String path = "/api/action-items/" + candidate.getId() + "/approve";
        mvc.perform(post(path).header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest());
        for (int i = 0; i < 2; i++)
            mvc.perform(
                            post(path)
                                    .header("Authorization", "Bearer " + token)
                                    .contentType("application/json")
                                    .content("{\"startsAt\":\"2026-12-03T00:00:00Z\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.actionItemId").value(candidate.getId().toString()));
        assertThat(schedules.findAll()).hasSize(1);
        mvc.perform(
                        get("/api/action-items")
                                .header("Authorization", "Bearer " + token)
                                .param("patientId", patientId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].approvalStatus").value("APPROVED"))
                .andExpect(jsonPath("$[0].documentId").value(document.getId().toString()))
                .andExpect(jsonPath("$[0].approvedByUserId").doesNotHaveJsonPath())
                .andExpect(jsonPath("$[0].cancelReason").doesNotHaveJsonPath());
        mvc.perform(
                        get("/api/schedules")
                                .header("Authorization", "Bearer " + token)
                                .param("patientId", patientId.toString())
                                .param("from", "2026-12-01T00:00:00Z")
                                .param("to", "2027-01-01T00:00:00Z"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("재검진 권고"));
    }

    @Test
    void repeatedApprovalReturnsOriginalScheduleAfterSourceDeletion() throws Exception {
        String path = "/api/action-items/" + candidate.getId() + "/approve";
        mvc.perform(
                        post(path)
                                .header("Authorization", "Bearer " + token)
                                .contentType("application/json")
                                .content("{\"startsAt\":\"2026-12-03T00:00:00Z\"}"))
                .andExpect(status().isOk());
        var original = schedules.findByActionItemId(candidate.getId()).orElseThrow();
        mvc.perform(
                        delete("/api/documents/" + document.getId())
                                .header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());
        em.flush();
        em.clear();

        mvc.perform(post(path).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(original.getId().toString()));
        assertThat(schedules.findAll()).hasSize(1);
    }

    @Test
    void rejectedCandidateDoesNotCreateSchedule() throws Exception {
        String path = "/api/action-items/" + candidate.getId();
        mvc.perform(post(path + "/reject").header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());
        mvc.perform(
                        delete("/api/documents/" + document.getId())
                                .header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());
        em.flush();
        em.clear();
        mvc.perform(post(path + "/reject").header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());
        mvc.perform(post(path + "/approve").header("Authorization", "Bearer " + token))
                .andExpect(status().isConflict());
        assertThat(schedules.findAll()).isEmpty();
    }

    @Test
    void linkedGuardianCanApproveButLosesAccessAfterRevocation() throws Exception {
        var guardian =
                accounts.chooseRole(
                        accounts.loginOrRegister(UUID.randomUUID().toString(), "연결 보호자")
                                .getKakaoId(),
                        UserRole.CAREGIVER);
        var relation = cares.request(guardian, accounts.patientInviteCode(owner));
        cares.accept(owner, relation.getId());
        String guardianToken = jwt.create(guardian);
        mvc.perform(
                        post("/api/action-items/" + candidate.getId() + "/approve")
                                .header("Authorization", "Bearer " + guardianToken)
                                .contentType("application/json")
                                .content("{\"startsAt\":\"2026-12-03T00:00:00Z\"}"))
                .andExpect(status().isOk());
        assertThat(schedules.findAll()).hasSize(1);
        cares.revoke(owner, relation.getId());
        mvc.perform(
                        get("/api/schedules")
                                .header("Authorization", "Bearer " + guardianToken)
                                .param("patientId", patientId.toString())
                                .param("from", "2026-12-01T00:00:00Z")
                                .param("to", "2027-01-01T00:00:00Z"))
                .andExpect(status().isForbidden());
    }

    @Test
    void unrelatedGuardianCannotReadOrApprove() throws Exception {
        var stranger =
                accounts.chooseRole(
                        accounts.loginOrRegister(UUID.randomUUID().toString(), "외부 보호자")
                                .getKakaoId(),
                        UserRole.CAREGIVER);
        var other = jwt.create(stranger);
        mvc.perform(
                        get("/api/action-items")
                                .param("patientId", patientId.toString())
                                .header("Authorization", "Bearer " + other))
                .andExpect(status().isForbidden());
        mvc.perform(
                        post("/api/action-items/" + candidate.getId() + "/approve")
                                .header("Authorization", "Bearer " + other))
                .andExpect(status().isForbidden());
        mvc.perform(
                        get("/api/schedules")
                                .param("patientId", patientId.toString())
                                .param("from", "2026-12-01T00:00:00Z")
                                .param("to", "2027-01-01T00:00:00Z")
                                .header("Authorization", "Bearer " + other))
                .andExpect(status().isForbidden());
    }

    @Test
    void sourceDeletionCancelsCandidate() throws Exception {
        events.publishEvent(
                new MedicalDocumentDeletedEvent(
                        document.getId(), patientId, owner.getId(), Instant.now()));
        em.clear();
        mvc.perform(
                        post("/api/action-items/" + candidate.getId() + "/approve")
                                .header("Authorization", "Bearer " + token))
                .andExpect(status().isConflict());
        assertThat(schedules.findAll()).isEmpty();
    }

    @Test
    void manualScheduleAndHalfOpenDateRange() throws Exception {
        String body =
                "{\"patientId\":\""
                        + patientId
                        + "\",\"title\":\"상담"
                        + " 준비\",\"scheduleType\":\"PERSONAL\",\"startsAt\":\"2026-12-01T00:00:00Z\"}";
        for (String invalidType : new String[] {"\"UNKNOWN\"", "0"}) {
            mvc.perform(
                            post("/api/schedules")
                                    .header("Authorization", "Bearer " + token)
                                    .contentType("application/json")
                                    .content(body.replace("\"PERSONAL\"", invalidType)))
                    .andExpect(status().isBadRequest());
        }
        mvc.perform(
                        post("/api/schedules")
                                .header("Authorization", "Bearer " + token)
                                .contentType("application/json")
                                .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.scheduleType").value("PERSONAL"))
                .andExpect(jsonPath("$.actionItemId").doesNotExist());
        mvc.perform(
                        get("/api/schedules")
                                .header("Authorization", "Bearer " + token)
                                .param("patientId", patientId.toString())
                                .param("from", "2026-11-01T00:00:00Z")
                                .param("to", "2026-12-01T00:00:00Z"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
        mvc.perform(
                        post("/api/schedules")
                                .header("Authorization", "Bearer " + token)
                                .contentType("application/json")
                                .content(body.replace("PERSONAL", "INVALID")))
                .andExpect(status().isBadRequest());
    }
}

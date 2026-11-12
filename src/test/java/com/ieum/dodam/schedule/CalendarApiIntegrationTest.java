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

@SpringBootTest(properties = "dodam.schedule.reminders-enabled=false")
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
    @Autowired com.ieum.dodam.schedule.command.application.MedicationSeriesService recurring;
    @Autowired com.ieum.dodam.schedule.domain.repository.MedicationSeriesRepository series;
    @Autowired com.ieum.dodam.schedule.command.application.ScheduleReminderService reminders;

    @Autowired
    com.ieum.dodam.notification.domain.repository.SystemNotificationRepository notifications;

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
    void recurringQueryMaterializesMonthAndRejectsOversizedRange() throws Exception {
        var start = LocalDate.now(ZoneId.of("Asia/Seoul")).plusDays(1)
                .atTime(9, 0).atZone(ZoneId.of("Asia/Seoul")).toInstant();
        recurring.create(owner, patientId, UUID.randomUUID(), "반복 복약", null,
                start, "Asia/Seoul", null);
        var from = start.plusSeconds(100L * 86400);
        mvc.perform(get("/api/schedules").header("Authorization", "Bearer " + token)
                .param("patientId", patientId.toString()).param("from", from.toString())
                .param("to", from.plusSeconds(30L * 86400).toString()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(30));
        var count = schedules.count();
        mvc.perform(get("/api/schedules").header("Authorization", "Bearer " + token)
                .param("patientId", patientId.toString()).param("from", from.toString())
                .param("to", from.plusSeconds(93L * 86400).toString()))
                .andExpect(status().isBadRequest());
        assertThat(schedules.count()).isEqualTo(count);
    }

    @Test
    void editingWeekdaysUpdatesFutureDosesAndPreservesCompletedRecords() {
        var zone = ZoneId.of("Asia/Seoul");
        var first = LocalDate.now(zone).plusDays(1);
        var start = first.atTime(9, 0).atZone(zone).toInstant();
        int firstWeekday = 1 << (first.getDayOfWeek().getValue() - 1);
        var rule = recurring.create(owner, patientId, UUID.randomUUID(), "요일 복약", null,
                start, zone.getId(), first.plusDays(7), firstWeekday);
        var original = schedules.findAll().stream()
                .filter(row -> rule.getId().equals(row.getSeriesId())).toList();
        assertThat(original).extracting(row -> row.getOccurrenceDate())
                .containsExactlyInAnyOrder(first, first.plusDays(7));
        var completed = original.stream().filter(row -> row.getOccurrenceDate().equals(first)).findFirst().orElseThrow();
        completed.finish(true);
        schedules.flush();

        int nextWeekday = 1 << (first.plusDays(1).getDayOfWeek().getValue() - 1);
        recurring.update(owner, rule.getId(), rule.getVersion(), nextWeekday, LocalTime.of(18, 30), first.plusDays(7));
        assertThat(schedules.findById(completed.getId()).orElseThrow().getStatus()).isEqualTo("COMPLETED");
        assertThat(schedules.findAll()).filteredOn(row -> rule.getId().equals(row.getSeriesId()) && "SCHEDULED".equals(row.getStatus()))
                .singleElement().satisfies(row -> {
                    assertThat(row.getOccurrenceDate()).isEqualTo(first.plusDays(1));
                    assertThat(row.getStartsAt()).isEqualTo(first.plusDays(1).atTime(18, 30).atZone(zone).toInstant());
                });
    }

    @Test
    void replenishmentQuerySkipsFinishedAndAlreadyCoveredRules() {
        var zone = ZoneId.of("Asia/Seoul");
        var day = LocalDate.now(zone).plusDays(1);
        var start = day.atTime(9, 0).atZone(zone).toInstant();
        var finite = recurring.create(owner, patientId, UUID.randomUUID(), "유한 복약", null,
                start, zone.getId(), day.plusDays(2));
        var endless = recurring.create(owner, patientId, UUID.randomUUID(), "계속 복약", null,
                start, zone.getId(), null);
        assertThat(series.dueIds(day.plusDays(31))).doesNotContain(finite.getId(), endless.getId());
        assertThat(series.dueIds(day.plusDays(40))).contains(endless.getId()).doesNotContain(finite.getId());
        recurring.stop(owner, finite.getId());
        assertThat(schedules.findAll()).filteredOn(s -> finite.getId().equals(s.getSeriesId()))
                .allSatisfy(s -> assertThat(s.getStatus()).isEqualTo("CANCELLED"));
    }

    @Test
    void dailyMedicationRetriesAreIdempotentAndTerminalSchedulesDoNotNotify() throws Exception {
        UUID requestId = UUID.randomUUID();
        Instant start =
                Instant.now().minusSeconds(60).truncatedTo(java.time.temporal.ChronoUnit.SECONDS);
        String body =
                "{\"patientId\":\""
                        + patientId
                        + "\",\"requestId\":\""
                        + requestId
                        + "\",\"title\":\"비타민 D\",\"scheduleType\":\"MEDICATION\",\"startsAt\":\""
                        + start
                        + "\",\"notes\":\"안내받은 용량"
                        + " 메모\",\"repeatDays\":3,\"timeZone\":\"Asia/Seoul\"}";
        for (int i = 0; i < 2; i++) {
            mvc.perform(
                            post("/api/schedule-batches")
                                    .header("Authorization", "Bearer " + token)
                                    .contentType("application/json")
                                    .content(body))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(3));
        }
        var rows = schedules.findByBatchIdOrderByStartsAtAsc(requestId);
        assertThat(rows).hasSize(3);
        assertThat(rows.get(1).getStartsAt()).isEqualTo(start.plusSeconds(86400));
        mvc.perform(
                        post("/api/schedule-batches")
                                .header("Authorization", "Bearer " + token)
                                .contentType("application/json")
                                .content(body.replace("비타민 D", "다른 약")))
                .andExpect(status().isConflict());
        mvc.perform(
                        post("/api/schedules/" + rows.get(1).getId() + "/complete")
                                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
        mvc.perform(
                        delete("/api/schedules/" + rows.get(2).getId())
                                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
        reminders.deliverDue(start.plusSeconds(3 * 86400));
        reminders.deliverDue(start.plusSeconds(3 * 86400));
        assertThat(notifications.findByRecipientUserIdAndReadAtIsNull(owner.getId()))
                .filteredOn(
                        n ->
                                n.getType()
                                        == com.ieum.dodam.notification.domain.SystemActivityType
                                                .SCHEDULE_DUE)
                .hasSize(1)
                .allSatisfy(n -> {
                    assertThat(n.getMessage()).contains("비타민 D", "복용 여부", "월", "일");
                    assertThat(n.getPath()).contains("patientId=" + patientId, "date=" + start);
                });
    }

    @Test
    void scheduleUpdatesCheckVersionAndReminderOnlyFollowsCurrentTime() throws Exception {
        Instant now = Instant.now().truncatedTo(java.time.temporal.ChronoUnit.SECONDS);
        var row =
                schedules.saveAndFlush(
                        com.ieum.dodam.schedule.domain.Schedule.create(
                                patientId,
                                null,
                                com.ieum.dodam.schedule.domain.ScheduleType.TEST,
                                "검진",
                                now.minusSeconds(60)));
        String body =
                "{\"version\":0,\"title\":\"변경한 검진\",\"scheduleType\":\"TEST\",\"startsAt\":\""
                        + now.plusSeconds(3600)
                        + "\"}";
        String path = "/api/schedules/" + row.getId();
        mvc.perform(
                        put(path)
                                .header("Authorization", "Bearer " + token)
                                .contentType("application/json")
                                .content(body))
                .andExpect(status().isOk());
        mvc.perform(
                        put(path)
                                .header("Authorization", "Bearer " + token)
                                .contentType("application/json")
                                .content(body))
                .andExpect(status().isConflict());
        reminders.deliverDue(now);
        assertThat(schedules.findById(row.getId()).orElseThrow().getRemindedAt()).isNull();
        reminders.deliverDue(now.plusSeconds(3601));
        assertThat(schedules.findById(row.getId()).orElseThrow().getRemindedAt()).isNotNull();
    }

    @Test
    void revokedGuardianCannotChangeScheduleOrReceiveItsReminder() throws Exception {
        var guardian =
                accounts.chooseRole(
                        accounts.loginOrRegister(UUID.randomUUID().toString(), "테스트 보호자")
                                .getKakaoId(),
                        UserRole.CAREGIVER);
        var relation = cares.request(guardian, accounts.patientInviteCode(owner));
        cares.accept(owner, relation.getId());
        cares.revoke(owner, relation.getId());
        var row =
                schedules.saveAndFlush(
                        com.ieum.dodam.schedule.domain.Schedule.create(
                                patientId,
                                null,
                                com.ieum.dodam.schedule.domain.ScheduleType.MEDICATION,
                                "테스트 복약",
                                Instant.now().minusSeconds(1)));
        String guardianToken = jwt.create(guardian);
        mvc.perform(
                        post("/api/schedules/" + row.getId() + "/complete")
                                .header("Authorization", "Bearer " + guardianToken))
                .andExpect(status().isForbidden());
        mvc.perform(
                        delete("/api/schedules/" + row.getId())
                                .header("Authorization", "Bearer " + guardianToken))
                .andExpect(status().isForbidden());
        reminders.deliverDue(Instant.now());
        assertThat(notifications.findByRecipientUserIdAndReadAtIsNull(guardian.getId()))
                .noneMatch(
                        n ->
                                n.getType()
                                        == com.ieum.dodam.notification.domain.SystemActivityType
                                                .SCHEDULE_DUE);
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

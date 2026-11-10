package com.ieum.dodam.vaccination.command.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ieum.dodam.global.exception.BusinessException;
import com.ieum.dodam.medical_document.query.application.CareRelationPermissionService;
import com.ieum.dodam.user.domain.User;
import com.ieum.dodam.vaccination.domain.entity.VaccinationDoseCheck;
import com.ieum.dodam.vaccination.domain.repository.VaccinationDoseCheckRepository;
import com.ieum.dodam.vaccination.error.VaccinationErrorCode;
import com.ieum.dodam.vaccination.query.application.VaccinationQueryService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class VaccinationCheckCommandServiceTest {

    private static final UUID PATIENT = UUID.randomUUID();

    private final CareRelationPermissionService permissions = mock(CareRelationPermissionService.class);
    private final VaccinationDoseCheckRepository checks = mock(VaccinationDoseCheckRepository.class);
    private final VaccinationQueryService queries = mock(VaccinationQueryService.class);
    private final VaccinationCheckCommandService service =
            new VaccinationCheckCommandService(permissions, checks, queries);
    private final User actor = mock(User.class);

    @BeforeEach
    void setUp() throws Exception {
        when(permissions.resolveTargetPatient(actor, PATIENT)).thenReturn(PATIENT);
        JsonNode schedule = new ObjectMapper().readTree(
                "{\"vaccines\":[{\"doses\":[{\"doseId\":\"DTAP-1\"},{\"doseId\":\"DTAP-2\"}]}]}");
        when(queries.scheduleOf(PATIENT)).thenReturn(schedule);
    }

    /** 순서 고정: 확인 시점엔 없음 → 다른 요청이 먼저 저장 → 저장 시 UNIQUE 위반 → 재확인 시 있음 = 성공. */
    @Test
    void concurrentDuplicateInsertIsSuccessWhenRowExistsOnRecheck() {
        when(checks.existsByPatientIdAndDoseId(PATIENT, "DTAP-1")).thenReturn(false, true);
        when(checks.saveAndFlush(any())).thenThrow(new DataIntegrityViolationException("unique"));

        assertThatCode(() -> service.check(actor, PATIENT, "DTAP-1")).doesNotThrowAnyException();
    }

    @Test
    void otherConstraintViolationIsRethrown() {
        when(checks.existsByPatientIdAndDoseId(PATIENT, "DTAP-1")).thenReturn(false, false);
        DataIntegrityViolationException fk = new DataIntegrityViolationException("fk");
        when(checks.saveAndFlush(any())).thenThrow(fk);

        assertThatThrownBy(() -> service.check(actor, PATIENT, "DTAP-1")).isSameAs(fk);
    }

    @Test
    void alreadyCheckedSkipsAiCall() {
        when(checks.existsByPatientIdAndDoseId(PATIENT, "DTAP-1")).thenReturn(true);

        service.check(actor, PATIENT, "DTAP-1");

        verifyNoInteractions(queries);
        verify(checks, never()).saveAndFlush(any());
    }

    @Test
    void doseNotInScheduleIsRejectedAndNotStored() {
        assertError(() -> service.check(actor, PATIENT, "XYZ-9"), VaccinationErrorCode.UNKNOWN_DOSE);
        verify(checks, never()).saveAndFlush(any());
    }

    @Test
    void aiFailureIsNotStored() {
        when(queries.scheduleOf(PATIENT)).thenThrow(new BusinessException(VaccinationErrorCode.AI_UNAVAILABLE));

        assertError(() -> service.check(actor, PATIENT, "DTAP-1"), VaccinationErrorCode.AI_UNAVAILABLE);
        verify(checks, never()).saveAndFlush(any());
    }

    @Test
    void scheduleWithoutDosesIsUnavailableNotUnknownDose() throws Exception {
        when(queries.scheduleOf(PATIENT)).thenReturn(new ObjectMapper().readTree("{\"foo\":1}"));

        assertError(() -> service.check(actor, PATIENT, "DTAP-1"), VaccinationErrorCode.AI_UNAVAILABLE);
    }

    @Test
    void malformedDoseIdIsRejectedBeforePermissionLookup() {
        for (String bad : new String[] {"dtap-1", "DTAP", "DTAP-", "DT AP-1", "A".repeat(49) + "-1"}) {
            assertError(() -> service.check(actor, PATIENT, bad), VaccinationErrorCode.INVALID_DOSE_ID);
            assertError(() -> service.uncheck(actor, PATIENT, bad), VaccinationErrorCode.INVALID_DOSE_ID);
        }
        verifyNoInteractions(permissions);
    }

    @Test
    void storesActorAsCheckedBy() {
        UUID actorId = UUID.randomUUID();
        when(actor.getId()).thenReturn(actorId);
        when(checks.saveAndFlush(any())).thenAnswer(inv -> {
            VaccinationDoseCheck saved = inv.getArgument(0);
            assertThat(saved.getCheckedBy()).isEqualTo(actorId);
            assertThat(saved.getPatientId()).isEqualTo(PATIENT);
            return saved;
        });

        service.check(actor, PATIENT, "DTAP-2");

        verify(checks).saveAndFlush(any());
    }

    private static void assertError(org.assertj.core.api.ThrowableAssert.ThrowingCallable call,
                                    VaccinationErrorCode expected) {
        assertThatThrownBy(call).isInstanceOfSatisfying(BusinessException.class,
                e -> assertThat(e.getErrorCode()).isEqualTo(expected));
    }
}

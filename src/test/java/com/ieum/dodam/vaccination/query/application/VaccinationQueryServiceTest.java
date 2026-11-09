package com.ieum.dodam.vaccination.query.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ieum.dodam.global.exception.BusinessException;
import com.ieum.dodam.medical_document.query.application.CareRelationPermissionService;
import com.ieum.dodam.patient.domain.Patient;
import com.ieum.dodam.patient.domain.repository.PatientRepository;
import com.ieum.dodam.vaccination.domain.UnknownDoseException;
import com.ieum.dodam.vaccination.domain.VaccinationSchedulePort;
import com.ieum.dodam.vaccination.domain.repository.VaccinationDoseCheckRepository;
import com.ieum.dodam.vaccination.error.VaccinationErrorCode;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** AI가 모르는 차수를 알려 줄 때 그 차수만 빼고 한 번만 다시 부르는지 확인합니다. */
class VaccinationQueryServiceTest {

    private static final UUID PATIENT = UUID.randomUUID();
    private static final LocalDate BIRTH = LocalDate.of(2026, 1, 15);

    private final PatientRepository patients = mock(PatientRepository.class);
    private final VaccinationDoseCheckRepository checks = mock(VaccinationDoseCheckRepository.class);
    private final VaccinationSchedulePort schedules = mock(VaccinationSchedulePort.class);
    private final VaccinationQueryService service = new VaccinationQueryService(
            mock(CareRelationPermissionService.class), patients, checks, schedules);

    @BeforeEach
    void setUp() {
        Patient patient = new Patient();
        patient.setBirthDate(BIRTH);
        when(patients.findById(PATIENT)).thenReturn(Optional.of(patient));
        when(checks.findDoseIds(PATIENT)).thenReturn(List.of("OLD-1", "RV-1"));
    }

    /** 회귀: 빈 목록으로 다시 부르면 만료 처리로 RV-1이 사라졌다. 이제 OLD-1만 빼고 RV-1은 그대로 보낸다. */
    @Test
    void retriesOnceWithoutOnlyUnknownDoses() throws Exception {
        JsonNode result = new ObjectMapper().readTree("{\"marker\":\"final\"}");
        when(schedules.fetchSchedule(eq(BIRTH), any(), eq(List.of("OLD-1", "RV-1"))))
                .thenThrow(new UnknownDoseException(List.of("OLD-1")));
        when(schedules.fetchSchedule(eq(BIRTH), any(), eq(List.of("RV-1")))).thenReturn(result);

        assertThat(service.scheduleOf(PATIENT)).isSameAs(result);

        verify(schedules, times(2)).fetchSchedule(any(), any(), anyList());
        verify(schedules).fetchSchedule(eq(BIRTH), any(), eq(List.of("RV-1")));
        verify(schedules, never()).fetchSchedule(any(), any(), eq(List.of()));
        verify(checks, never()).deleteByPatientIdAndDoseId(any(), anyString());
    }

    @Test
    void secondUnknownDoseStopsWithUnavailableAfterTwoCalls() {
        when(schedules.fetchSchedule(any(), any(), anyList()))
                .thenThrow(new UnknownDoseException(List.of("OLD-1")))
                .thenThrow(new UnknownDoseException(List.of("RV-1")));

        assertUnavailable();
        verify(schedules, times(2)).fetchSchedule(any(), any(), anyList());
    }

    @Test
    void emptyUnknownDoseIdsStopsWithoutRetry() {
        when(schedules.fetchSchedule(any(), any(), anyList())).thenThrow(new UnknownDoseException(List.of()));

        assertUnavailable();
        verify(schedules, times(1)).fetchSchedule(any(), any(), anyList());
    }

    @Test
    void unknownDoseIdsNotStoredStopsWithoutRetry() {
        when(schedules.fetchSchedule(any(), any(), anyList()))
                .thenThrow(new UnknownDoseException(List.of("ZZZ-1")));

        assertUnavailable();
        verify(schedules, times(1)).fetchSchedule(any(), any(), anyList());
    }

    private void assertUnavailable() {
        assertThatThrownBy(() -> service.scheduleOf(PATIENT))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(VaccinationErrorCode.AI_UNAVAILABLE));
        verify(checks, never()).deleteByPatientIdAndDoseId(any(), anyString());
    }
}

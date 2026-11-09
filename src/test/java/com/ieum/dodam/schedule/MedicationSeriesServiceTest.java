package com.ieum.dodam.schedule;

import com.ieum.dodam.schedule.command.application.MedicationSeriesService;
import com.ieum.dodam.schedule.domain.MedicationSeries;
import com.ieum.dodam.schedule.domain.repository.*;
import com.ieum.dodam.visit.domain.VisitAccessPolicy;
import org.junit.jupiter.api.Test;
import java.time.*;
import java.util.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

class MedicationSeriesServiceTest {
    @Test
    void stoppedRulesAreSkippedBeforeTakingALock() {
        var series = mock(MedicationSeriesRepository.class);
        var schedules = mock(ScheduleRepository.class);
        var patient = UUID.randomUUID();
        var rule = MedicationSeries.create(UUID.randomUUID(), patient, "medicine", null,
                Instant.now(), ZoneId.of("Asia/Seoul"), null);
        rule.stop();
        when(series.findByPatientIdOrderByStartDateDesc(patient)).thenReturn(List.of(rule));
        new MedicationSeriesService(series, schedules, mock(VisitAccessPolicy.class))
                .ensureRange(patient, Instant.now(), Instant.now().plusSeconds(86400));
        verify(series, never()).lockById(any());
        verifyNoInteractions(schedules);
    }
}

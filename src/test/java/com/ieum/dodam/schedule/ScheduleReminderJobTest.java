package com.ieum.dodam.schedule;

import com.ieum.dodam.schedule.command.application.*;
import com.ieum.dodam.schedule.domain.repository.MedicationSeriesRepository;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

class ScheduleReminderJobTest {
    @Test
    void brokenRuleDoesNotPreventOtherRulesOrDelivery() {
        var reminders = mock(ScheduleReminderService.class);
        var recurring = mock(MedicationSeriesService.class);
        var series = mock(MedicationSeriesRepository.class);
        var broken = UUID.randomUUID();
        var healthy = UUID.randomUUID();
        when(series.dueIds(any())).thenReturn(List.of(broken, healthy));
        doThrow(new java.time.DateTimeException("invalid zone")).when(recurring).replenish(eq(broken), any());
        new ScheduleReminderJob(reminders, recurring, series).run();
        verify(recurring).replenish(eq(healthy), any());
        verify(reminders).deliverDue(any());
    }

    @Test
    void queryFailureStillAttemptsDelivery() {
        var reminders = mock(ScheduleReminderService.class);
        var recurring = mock(MedicationSeriesService.class);
        var series = mock(MedicationSeriesRepository.class);
        when(series.dueIds(any())).thenThrow(new IllegalStateException("query failed"));
        new ScheduleReminderJob(reminders, recurring, series).run();
        verify(reminders).deliverDue(any());
    }
}

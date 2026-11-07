package com.ieum.dodam.schedule.command.application;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import java.time.Instant;

@Configuration
@EnableScheduling
@ConditionalOnProperty(
        name = "dodam.schedule.reminders-enabled",
        havingValue = "true",
        matchIfMissing = true)
public class ScheduleReminderJob {
    private final ScheduleReminderService reminders;

    private final MedicationSeriesService recurring;
    private final com.ieum.dodam.schedule.domain.repository.MedicationSeriesRepository series;
    public ScheduleReminderJob(ScheduleReminderService reminders, MedicationSeriesService recurring, com.ieum.dodam.schedule.domain.repository.MedicationSeriesRepository series) {
        this.reminders = reminders; this.recurring=recurring; this.series=series;
    }

    @Scheduled(fixedDelayString = "${dodam.schedule.reminder-delay-ms:15000}", initialDelay = 15000)
    public void run() {
        var now=Instant.now();
        for(var id:series.activeIds()) recurring.replenish(id,now);
        reminders.deliverDue(now);
    }
}

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
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(ScheduleReminderJob.class);
    private final ScheduleReminderService reminders;

    private final MedicationSeriesService recurring;
    private final com.ieum.dodam.schedule.domain.repository.MedicationSeriesRepository series;
    public ScheduleReminderJob(ScheduleReminderService reminders, MedicationSeriesService recurring, com.ieum.dodam.schedule.domain.repository.MedicationSeriesRepository series) {
        this.reminders = reminders; this.recurring=recurring; this.series=series;
    }

    @Scheduled(fixedDelayString = "${dodam.schedule.reminder-delay-ms:15000}", initialDelay = 15000)
    public void run() {
        var now=Instant.now();
        try {
            // UTC+14 is the earliest next local day; replenish checks each rule's own zone.
            var through = now.atOffset(java.time.ZoneOffset.ofHours(14)).toLocalDate().plusDays(31);
            for (var id : series.dueIds(through)) {
                try {
                    recurring.replenish(id, now);
                } catch (RuntimeException exception) {
                    log.error("Medication replenishment failed for series {}", id, exception);
                }
            }
        } catch (RuntimeException exception) {
            log.error("Could not query medication series for replenishment", exception);
        } finally {
            reminders.deliverDue(now);
        }
    }
}

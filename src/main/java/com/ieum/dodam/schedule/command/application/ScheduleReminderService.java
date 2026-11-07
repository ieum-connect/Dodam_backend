package com.ieum.dodam.schedule.command.application;

import com.ieum.dodam.care_relation.domain.CareRelationStatus;
import com.ieum.dodam.care_relation.domain.repository.CareRelationRepository;
import com.ieum.dodam.notification.domain.SystemActivityType;
import com.ieum.dodam.notification.domain.entity.SystemNotification;
import com.ieum.dodam.notification.domain.repository.SystemNotificationRepository;
import com.ieum.dodam.patient.domain.repository.PatientRepository;
import com.ieum.dodam.schedule.domain.ScheduleType;
import com.ieum.dodam.schedule.domain.repository.ScheduleRepository;
import com.ieum.dodam.user.domain.UserStatus;
import com.ieum.dodam.user.domain.repository.UserRepository;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashSet;

@Service
public class ScheduleReminderService {
    private final ScheduleRepository schedules;
    private final PatientRepository patients;
    private final CareRelationRepository relations;
    private final UserRepository users;
    private final SystemNotificationRepository notifications;

    public ScheduleReminderService(
            ScheduleRepository schedules,
            PatientRepository patients,
            CareRelationRepository relations,
            UserRepository users,
            SystemNotificationRepository notifications) {
        this.schedules = schedules;
        this.patients = patients;
        this.relations = relations;
        this.users = users;
        this.notifications = notifications;
    }

    private static String shortText(String value, int max) {
        return value.length() > max ? value.substring(0, max - 1) + "…" : value;
    }

    /** Row locks and delivery marker commit together with all recipient notifications. */
    @Transactional
    public void deliverDue(Instant now) {
        for (var schedule : schedules.lockDue(now, PageRequest.of(0, 100))) {
            var patient = patients.findById(schedule.getPatientId()).orElseThrow();
            var recipients = new HashSet<java.util.UUID>();
            recipients.add(patient.getUserId());
            relations
                    .findByPatientIdAndStatus(patient.getId(), CareRelationStatus.ACTIVE)
                    .forEach(relation -> recipients.add(relation.getCaregiverId()));
            String childName = patient.getChildName();
            if (childName == null || childName.isBlank()) childName = "아이";
            String time = java.time.format.DateTimeFormatter.ofPattern("M월 d일 HH:mm")
                    .withZone(java.time.ZoneId.of("Asia/Seoul")).format(schedule.getStartsAt());
            String message = shortText(childName, 40) + " · " + time + "\n"
                    + shortText(schedule.getTitle(), 120)
                    + (schedule.getScheduleType() == ScheduleType.MEDICATION
                            ? " — 복용 여부를 확인해주세요." : " — 일정을 확인해주세요.");
            String path =
                    "/calendar?patientId=" + patient.getId() + "&date=" + schedule.getStartsAt();
            for (var recipient : recipients) {
                if (users.findById(recipient)
                        .filter(user -> user.getStatus() == UserStatus.ACTIVE)
                        .isPresent()) {
                    notifications.save(
                            new SystemNotification(
                                    recipient, SystemActivityType.SCHEDULE_DUE, message, path));
                }
            }
            schedule.markReminded(now);
        }
    }
}

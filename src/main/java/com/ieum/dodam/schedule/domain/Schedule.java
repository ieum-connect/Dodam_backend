package com.ieum.dodam.schedule.domain;

import jakarta.persistence.*;

import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "schedules", uniqueConstraints = @UniqueConstraint(columnNames = "action_item_id"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Schedule {
    @Id private UUID id;

    @Column(name = "patient_id", nullable = false)
    private UUID patientId;

    @Column(name = "action_item_id")
    private UUID actionItemId;

    @Enumerated(EnumType.STRING)
    @Column(name = "schedule_type", nullable = false, length = 20)
    private ScheduleType scheduleType;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(name = "starts_at", nullable = false)
    private Instant startsAt;

    @Column(name = "ends_at")
    private Instant endsAt;

    @Column(nullable = false, length = 20)
    private String status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(length = 500)
    private String notes;

    private UUID seriesId;
    private java.time.LocalDate occurrenceDate;
    public void assignSeries(UUID id, java.time.LocalDate day) { seriesId=id; occurrenceDate=day; }

    @Column(name = "batch_id")
    private UUID batchId;

    public void assignBatch(UUID batchId) {
        this.batchId = batchId;
    }

    @Column(name = "reminded_at")
    private Instant remindedAt;

    @Version private long version;

    public void update(String title, ScheduleType type, Instant startsAt, String notes) {
        this.title = title;
        this.scheduleType = type;
        if (!this.startsAt.equals(startsAt)) remindedAt = null;
        this.startsAt = startsAt;
        this.notes = notes;
        updatedAt = Instant.now();
    }

    public void finish(boolean completed) {
        status = completed ? "COMPLETED" : "CANCELLED";
        updatedAt = Instant.now();
    }

    public void markReminded(Instant now) {
        remindedAt = now;
    }

    public static Schedule create(
            UUID patientId, UUID actionItemId, ScheduleType type, String title, Instant startsAt) {
        var row = new Schedule();
        row.id = UUID.randomUUID();
        row.patientId = patientId;
        row.actionItemId = actionItemId;
        row.scheduleType = type;
        row.title = title;
        row.startsAt = startsAt;
        row.status = "SCHEDULED";
        row.createdAt = row.updatedAt = Instant.now();
        return row;
    }
}

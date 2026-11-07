package com.ieum.dodam.growth.domain;

import jakarta.persistence.*;

import lombok.*;

import java.math.BigDecimal;
import java.time.*;
import java.util.UUID;

@Entity
@Table(name = "growth_records")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class GrowthRecord {
    @Id private UUID id;

    @Column(nullable = false)
    private UUID patientId;

    @Column(nullable = false)
    private UUID authorUserId;

    @Column(nullable = false)
    private LocalDate measuredOn;

    @Column(precision = 4, scale = 1)
    private BigDecimal heightCm;

    @Column(precision = 4, scale = 1)
    private BigDecimal weightKg;

    @Column(length = 500)
    private String note;

    @Column(nullable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    private Instant deletedAt;
    @Version private long version;

    public static GrowthRecord create(
            UUID patientId,
            UUID author,
            LocalDate date,
            BigDecimal height,
            BigDecimal weight,
            String note) {
        var record = new GrowthRecord();
        record.id = UUID.randomUUID();
        record.patientId = patientId;
        record.authorUserId = author;
        record.createdAt = Instant.now();
        record.update(date, height, weight, note);
        return record;
    }

    public void update(LocalDate date, BigDecimal height, BigDecimal weight, String note) {
        measuredOn = date;
        heightCm = height;
        weightKg = weight;
        this.note = note == null ? null : note.trim();
        updatedAt = Instant.now();
    }

    public void delete() {
        deletedAt = Instant.now();
        updatedAt = deletedAt;
    }
}

package com.ieum.dodam.schedule.domain;

import jakarta.persistence.*;

import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "schedule_batches")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ScheduleBatch {
    @Id private UUID id;

    @Column(nullable = false)
    private UUID patientId;

    @Column(nullable = false, length = 64)
    private String fingerprint;

    public ScheduleBatch(UUID id, UUID patientId, String fingerprint) {
        this.id = id;
        this.patientId = patientId;
        this.fingerprint = fingerprint;
    }
}

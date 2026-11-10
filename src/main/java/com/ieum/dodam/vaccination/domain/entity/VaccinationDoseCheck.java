package com.ieum.dodam.vaccination.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/** 보호자가 "이미 맞았다"고 체크한 접종 차수 한 건입니다. (patient_id, dose_id)는 한 행만 존재합니다. */
@Entity
@Table(
        name = "vaccination_dose_checks",
        uniqueConstraints = @UniqueConstraint(
                name = "vaccination_dose_checks_patient_dose_key",
                columnNames = {"patient_id", "dose_id"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class VaccinationDoseCheck {

    /** doseId 최대 길이. DB 컬럼 VARCHAR(50)과 같은 값입니다. */
    public static final int DOSE_ID_MAX_LENGTH = 50;

    @Id
    private UUID id;

    @Column(name = "patient_id", nullable = false)
    private UUID patientId;

    @Column(name = "dose_id", nullable = false, length = DOSE_ID_MAX_LENGTH)
    private String doseId;

    @Column(name = "checked_by")
    private UUID checkedBy;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public static VaccinationDoseCheck create(UUID patientId, String doseId, UUID checkedBy) {
        VaccinationDoseCheck check = new VaccinationDoseCheck();
        check.id = UUID.randomUUID();
        check.patientId = patientId;
        check.doseId = doseId;
        check.checkedBy = checkedBy;
        check.createdAt = Instant.now();
        return check;
    }
}

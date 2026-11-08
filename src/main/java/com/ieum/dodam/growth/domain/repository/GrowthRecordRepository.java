package com.ieum.dodam.growth.domain.repository;

import com.ieum.dodam.growth.domain.GrowthRecord;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.*;

public interface GrowthRecordRepository extends JpaRepository<GrowthRecord, UUID> {
    List<GrowthRecord> findByPatientIdAndDeletedAtIsNullOrderByMeasuredOnDesc(UUID patientId);

    boolean existsByPatientIdAndMeasuredOnAndDeletedAtIsNull(UUID patientId, java.time.LocalDate measuredOn);

    boolean existsByPatientIdAndMeasuredOnAndDeletedAtIsNullAndIdNot(UUID patientId, java.time.LocalDate measuredOn, UUID id);

    Optional<GrowthRecord> findByIdAndDeletedAtIsNull(UUID id);
}

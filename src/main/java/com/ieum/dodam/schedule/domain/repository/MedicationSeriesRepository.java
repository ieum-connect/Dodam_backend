package com.ieum.dodam.schedule.domain.repository;
import com.ieum.dodam.schedule.domain.MedicationSeries;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import java.util.*;
public interface MedicationSeriesRepository extends JpaRepository<MedicationSeries,UUID> {
    List<MedicationSeries> findByPatientIdOrderByStartDateDesc(UUID patientId);
    @Query("select s.id from MedicationSeries s where s.active=true") List<UUID> activeIds();
    @Lock(LockModeType.PESSIMISTIC_WRITE) @Query("select s from MedicationSeries s where s.id=:id")
    Optional<MedicationSeries> lockById(@Param("id") UUID id);
}

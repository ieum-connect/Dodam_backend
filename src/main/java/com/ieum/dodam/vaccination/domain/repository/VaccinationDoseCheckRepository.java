package com.ieum.dodam.vaccination.domain.repository;

import com.ieum.dodam.vaccination.domain.entity.VaccinationDoseCheck;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

public interface VaccinationDoseCheckRepository extends JpaRepository<VaccinationDoseCheck, UUID> {

    @Query("select c.doseId from VaccinationDoseCheck c where c.patientId = :patientId order by c.doseId")
    List<String> findDoseIds(UUID patientId);

    boolean existsByPatientIdAndDoseId(UUID patientId, String doseId);

    /** 해당 체크를 지우고 지운 행 수를 반환합니다. 없으면 0입니다. */
    @Transactional
    @Modifying
    @Query("delete from VaccinationDoseCheck c where c.patientId = :patientId and c.doseId = :doseId")
    int deleteByPatientIdAndDoseId(UUID patientId, String doseId);
}

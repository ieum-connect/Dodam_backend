package com.ieum.dodam.patient.domain.repository;

import com.ieum.dodam.patient.domain.Patient;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.List;
import java.util.UUID;

public interface PatientRepository extends JpaRepository<Patient, UUID> {

    Optional<Patient> findByUserId(UUID userId);

    Optional<Patient> findByInviteCode(String inviteCode);

    @Query("select p.userId from Patient p where p.inviteCode = :code and p.childName is not null")
    Optional<UUID> findChildOwnerByInviteCode(@Param("code") String inviteCode);

    @Query("select p.userId from Patient p where p.id = :id and p.childName is not null")
    Optional<UUID> findChildOwnerById(@Param("id") UUID id);

    List<Patient> findAllByUserId(UUID userId);
}

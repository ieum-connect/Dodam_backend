package com.ieum.dodam.patient.domain.repository;

import com.ieum.dodam.patient.domain.Patient;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.List;
import java.util.UUID;

public interface PatientRepository extends JpaRepository<Patient, UUID> {

    Optional<Patient> findByUserId(UUID userId);

    Optional<Patient> findByInviteCode(String inviteCode);

    List<Patient> findAllByUserId(UUID userId);
}

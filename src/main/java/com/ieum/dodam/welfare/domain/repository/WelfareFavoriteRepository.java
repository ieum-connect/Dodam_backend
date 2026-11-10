package com.ieum.dodam.welfare.domain.repository;

import com.ieum.dodam.welfare.domain.entity.WelfareFavorite;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WelfareFavoriteRepository extends JpaRepository<WelfareFavorite, UUID> {
    Optional<WelfareFavorite> findByPatientIdAndProgramId(UUID patientId, UUID programId);
    List<WelfareFavorite> findByPatientIdOrderByCreatedAtDesc(UUID patientId);
}

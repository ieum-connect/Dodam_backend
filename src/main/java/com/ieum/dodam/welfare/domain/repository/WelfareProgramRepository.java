package com.ieum.dodam.welfare.domain.repository;

import com.ieum.dodam.welfare.domain.entity.WelfareProgram;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;
import java.util.List;
import org.springframework.data.domain.Pageable;

public interface WelfareProgramRepository extends JpaRepository<WelfareProgram, UUID> {
    Optional<WelfareProgram> findByExternalServiceId(String externalServiceId);
    List<WelfareProgram> findByActiveTrueAndNameContainingIgnoreCaseOrderByModifiedAtSourceDescIdDesc(String query, Pageable pageable);
    List<WelfareProgram> findByActiveTrueOrderByModifiedAtSourceDescIdDesc(Pageable pageable);
    List<WelfareProgram> findByActiveTrueOrderByModifiedAtSourceDescIdDesc();
}

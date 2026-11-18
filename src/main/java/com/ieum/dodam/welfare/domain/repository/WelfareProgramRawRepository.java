package com.ieum.dodam.welfare.domain.repository;

import com.ieum.dodam.welfare.domain.entity.WelfareProgramRaw;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface WelfareProgramRawRepository extends JpaRepository<WelfareProgramRaw, UUID> {
}

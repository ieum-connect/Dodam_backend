package com.ieum.dodam.welfare.domain.repository;

import com.ieum.dodam.welfare.domain.entity.WelfareSyncLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface WelfareSyncLogRepository extends JpaRepository<WelfareSyncLog, UUID> {
}

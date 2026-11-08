package com.ieum.dodam.schedule.domain.repository;

import com.ieum.dodam.schedule.domain.ScheduleBatch;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ScheduleBatchRepository extends JpaRepository<ScheduleBatch, UUID> {}

package com.ieum.dodam.schedule.domain.repository;

import com.ieum.dodam.schedule.domain.Schedule;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.*;

public interface ScheduleRepository extends JpaRepository<Schedule, UUID> {
    List<Schedule> findByPatientIdAndStartsAtGreaterThanEqualAndStartsAtLessThanOrderByStartsAtAsc(
            UUID patientId, Instant from, Instant to);

    Optional<Schedule> findByActionItemId(UUID actionItemId);
}

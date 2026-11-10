package com.ieum.dodam.schedule.domain.repository;

import com.ieum.dodam.patient.domain.Patient;
import com.ieum.dodam.schedule.domain.Schedule;

import jakarta.persistence.LockModeType;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.*;

public interface ScheduleRepository extends JpaRepository<Schedule, UUID> {
    @Query("select s.occurrenceDate from Schedule s where s.seriesId=:id and s.occurrenceDate between :from and :to")
    List<java.time.LocalDate> occurrenceDates(@Param("id") UUID id,@Param("from") java.time.LocalDate from,@Param("to") java.time.LocalDate to);
    @Lock(LockModeType.PESSIMISTIC_WRITE) @Query("select s from Schedule s where s.seriesId=:id and s.startsAt>=:now order by s.startsAt")
    List<Schedule> lockFutureSeries(@Param("id") UUID id,@Param("now") Instant now);

    List<Schedule> findByBatchIdOrderByStartsAtAsc(UUID batchId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from Schedule s where s.id = :id")
    Optional<Schedule> lockById(@Param("id") UUID id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Patient p where p.id = :id")
    Optional<Patient> lockPatient(@Param("id") UUID id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query(
            "select s from Schedule s where s.status = 'SCHEDULED' and s.remindedAt is null and"
                + " s.startsAt <= :now order by s.startsAt")
    List<Schedule> lockDue(@Param("now") Instant now, Pageable page);

    List<Schedule> findByPatientIdAndStartsAtGreaterThanEqualAndStartsAtLessThanOrderByStartsAtAsc(
            UUID patientId, Instant from, Instant to);

    Optional<Schedule> findByActionItemId(UUID actionItemId);
}

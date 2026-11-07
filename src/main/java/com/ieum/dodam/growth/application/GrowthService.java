package com.ieum.dodam.growth.application;

import com.ieum.dodam.global.exception.*;
import com.ieum.dodam.growth.api.dto.*;
import com.ieum.dodam.growth.domain.GrowthRecord;
import com.ieum.dodam.growth.domain.repository.GrowthRecordRepository;
import com.ieum.dodam.health_record.domain.HealthRecordAccessPolicy;
import com.ieum.dodam.patient.domain.repository.PatientRepository;
import com.ieum.dodam.user.domain.User;

import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.*;
import java.util.*;

@Service
@Transactional
public class GrowthService {
    private final GrowthRecordRepository records;
    private final HealthRecordAccessPolicy access;
    private final PatientRepository patients;
    private final JdbcTemplate jdbc;

    public GrowthService(
            GrowthRecordRepository records,
            HealthRecordAccessPolicy access,
            PatientRepository patients,
            JdbcTemplate jdbc) {
        this.records = records;
        this.access = access;
        this.patients = patients;
        this.jdbc = jdbc;
    }

    @Transactional(readOnly = true)
    public List<GrowthResponse> list(User actor, UUID patientId) {
        access.requirePatient(actor, patientId);
        return records.findByPatientIdAndDeletedAtIsNullOrderByMeasuredOnDesc(patientId).stream()
                .map(GrowthResponse::of)
                .toList();
    }

    public GrowthResponse create(User actor, GrowthWriteRequest request) {
        access.requirePatient(actor, request.patientId());
        lock(request.patientId());
        validate(request, null);
        return GrowthResponse.of(
                records.saveAndFlush(
                        GrowthRecord.create(
                                request.patientId(),
                                actor.getId(),
                                request.measuredOn(),
                                request.heightCm(),
                                request.weightKg(),
                                request.note())));
    }

    public GrowthResponse update(User actor, UUID id, GrowthWriteRequest request) {
        access.requirePatient(actor, request.patientId());
        lock(request.patientId());
        GrowthRecord record = require(id);
        if (!record.getPatientId().equals(request.patientId()))
            throw new BusinessException(GlobalErrorCode.INVALID_REQUEST);
        if (request.version() == null || request.version() != record.getVersion())
            throw new BusinessException(GlobalErrorCode.CONFLICT);
        validate(request, id);
        record.update(request.measuredOn(), request.heightCm(), request.weightKg(), request.note());
        return GrowthResponse.of(records.saveAndFlush(record));
    }

    public void delete(User actor, UUID id, long version) {
        GrowthRecord record = require(id);
        access.requirePatient(actor, record.getPatientId());
        if (version != record.getVersion()) throw new BusinessException(GlobalErrorCode.CONFLICT);
        record.delete();
        records.flush();
    }

    private GrowthRecord require(UUID id) {
        return records.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(
                        () ->
                                new ResponseStatusException(
                                        HttpStatus.NOT_FOUND, "성장 기록을 찾을 수 없어요."));
    }

    private void lock(UUID patientId) {
        jdbc.queryForObject("select id from patients where id=? for update", UUID.class, patientId);
    }

    private void validate(GrowthWriteRequest request, UUID editingId) {
        LocalDate birth = patients.findById(request.patientId()).orElseThrow().getBirthDate();
        if ((request.heightCm() == null && request.weightKg() == null)
                || request.measuredOn().isAfter(LocalDate.now(ZoneId.of("Asia/Seoul")))
                || (birth != null && request.measuredOn().isBefore(birth))) {
            throw new BusinessException(GlobalErrorCode.INVALID_REQUEST);
        }
        boolean duplicate = editingId == null
                ? records.existsByPatientIdAndMeasuredOnAndDeletedAtIsNull(request.patientId(), request.measuredOn())
                : records.existsByPatientIdAndMeasuredOnAndDeletedAtIsNullAndIdNot(request.patientId(), request.measuredOn(), editingId);
        if (duplicate) throw new BusinessException(GlobalErrorCode.CONFLICT);
    }
}

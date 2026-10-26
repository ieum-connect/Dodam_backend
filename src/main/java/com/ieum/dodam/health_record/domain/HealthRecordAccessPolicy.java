package com.ieum.dodam.health_record.domain;

import com.ieum.dodam.care_relation.domain.CareRelationStatus;
import com.ieum.dodam.care_relation.domain.repository.CareRelationRepository;
import com.ieum.dodam.global.exception.BusinessException;
import com.ieum.dodam.global.exception.GlobalErrorCode;
import com.ieum.dodam.health_record.error.HealthRecordErrorCode;
import com.ieum.dodam.patient.domain.Patient;
import com.ieum.dodam.patient.domain.repository.PatientRepository;
import com.ieum.dodam.user.domain.User;
import com.ieum.dodam.user.domain.UserRole;
import com.ieum.dodam.user.domain.UserStatus;

import org.springframework.stereotype.Component;

import java.util.UUID;

/** 본인 프로필 또는 ACTIVE 보호자 관계의 기록 접근을 검증한다. */
@Component
public class HealthRecordAccessPolicy {
    private final PatientRepository patients;
    private final CareRelationRepository relations;

    public HealthRecordAccessPolicy(PatientRepository patients, CareRelationRepository relations) {
        this.patients = patients;
        this.relations = relations;
    }

    /** 현재 개인 계정 본인 또는 ACTIVE로 연결된 대상 개인만 허용한다. */
    public UUID requirePatient(User actor, UUID requested) {
        if (actor.getStatus() != UserStatus.ACTIVE) {
            throw new BusinessException(HealthRecordErrorCode.ACCESS_DENIED);
        }
        if (actor.getRole() == UserRole.PATIENT) {
            UUID own =
                    patients.findByUserId(actor.getId())
                            .map(Patient::getId)
                            .orElseThrow(
                                    () ->
                                            new BusinessException(
                                                    HealthRecordErrorCode.ACCESS_DENIED));
            if (requested == null || own.equals(requested)) {
                return own;
            }
        } else if (actor.getRole() == UserRole.CAREGIVER
                && requested != null
                && relations.existsByPatientIdAndCaregiverIdAndStatus(
                        requested, actor.getId(), CareRelationStatus.ACTIVE)) {
            return requested;
        }
        throw new BusinessException(
                requested == null
                        ? GlobalErrorCode.INVALID_REQUEST
                        : HealthRecordErrorCode.ACCESS_DENIED);
    }
}

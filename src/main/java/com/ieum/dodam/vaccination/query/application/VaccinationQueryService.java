package com.ieum.dodam.vaccination.query.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.ieum.dodam.global.exception.BusinessException;
import com.ieum.dodam.medical_document.error.AiDocumentErrorCode;
import com.ieum.dodam.medical_document.query.application.CareRelationPermissionService;
import com.ieum.dodam.patient.domain.Patient;
import com.ieum.dodam.patient.domain.repository.PatientRepository;
import com.ieum.dodam.user.domain.User;
import com.ieum.dodam.vaccination.domain.VaccinationSchedulePort;
import com.ieum.dodam.vaccination.domain.repository.VaccinationDoseCheckRepository;
import com.ieum.dodam.vaccination.error.VaccinationErrorCode;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * 예방접종 일정 조회입니다. 클래스에 트랜잭션을 걸지 않아 AI 호출이 DB 트랜잭션 밖에서 일어납니다.
 */
@Service
public class VaccinationQueryService {

    private static final Logger log = LoggerFactory.getLogger(VaccinationQueryService.class);
    private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");

    private final CareRelationPermissionService permissions;
    private final PatientRepository patients;
    private final VaccinationDoseCheckRepository checks;
    private final VaccinationSchedulePort schedules;

    public VaccinationQueryService(
            CareRelationPermissionService permissions,
            PatientRepository patients,
            VaccinationDoseCheckRepository checks,
            VaccinationSchedulePort schedules) {
        this.permissions = permissions;
        this.patients = patients;
        this.checks = checks;
        this.schedules = schedules;
    }

    /** 접근 권한을 확인한 뒤 AI 일정 응답을 그대로 반환합니다. */
    public JsonNode schedule(User actor, UUID patientId) {
        return scheduleOf(permissions.resolveTargetPatient(actor, patientId));
    }

    /**
     * 권한 확인이 끝난 환자의 일정을 조회합니다. 저장된 체크 중 현재 일정에 없는 차수가 있으면
     * (일정 버전 변경, 수동 데이터 등) 그 차수만 빼고 다시 조회합니다. 저장 데이터는 지우지 않습니다.
     */
    public JsonNode scheduleOf(UUID patientId) {
        LocalDate birthDate = requireBirthDate(patientId);
        LocalDate today = LocalDate.now(SEOUL);
        List<String> completed = checks.findDoseIds(patientId);
        try {
            return schedules.fetchSchedule(birthDate, today, completed);
        } catch (BusinessException e) {
            if (e.getErrorCode() != VaccinationErrorCode.UNKNOWN_DOSE) {
                throw e;
            }
        }
        JsonNode base = fetchWithoutUnknownDose(birthDate, today, List.of());
        Set<String> valid = doseIds(base);
        List<String> known = completed.stream().filter(valid::contains).toList();
        log.warn("현재 일정에 없는 저장 차수를 빼고 다시 조회합니다: patientId={}, ignored={}",
                patientId, completed.size() - known.size());
        return known.isEmpty() ? base : fetchWithoutUnknownDose(birthDate, today, known);
    }

    /** 일정 응답의 vaccines[].doses[].doseId 목록입니다. 구조가 없으면 빈 집합입니다. */
    public static Set<String> doseIds(JsonNode schedule) {
        Set<String> ids = new HashSet<>();
        for (JsonNode vaccine : schedule.path("vaccines")) {
            for (JsonNode dose : vaccine.path("doses")) {
                String id = dose.path("doseId").asText(null);
                if (id != null) {
                    ids.add(id);
                }
            }
        }
        return ids;
    }

    private LocalDate requireBirthDate(UUID patientId) {
        // 권한 확인 직후 환자가 지워진 경우는 권한 거절과 같게 처리합니다.
        Patient patient = patients.findById(patientId)
                .orElseThrow(() -> new BusinessException(AiDocumentErrorCode.DOCUMENT_ACCESS_DENIED));
        LocalDate birthDate = patient.getBirthDate();
        if (birthDate == null) {
            throw new BusinessException(VaccinationErrorCode.BIRTH_DATE_REQUIRED);
        }
        // AI는 생년월일 > 기준일을 400으로 거절하므로 미리 막아 503으로 바뀌지 않게 합니다.
        if (birthDate.isAfter(LocalDate.now(SEOUL))) {
            throw new BusinessException(VaccinationErrorCode.BIRTH_DATE_IN_FUTURE);
        }
        return birthDate;
    }

    /** 재조회에서도 모르는 차수라고 하면 AI 응답이 서로 어긋난 것이므로 503으로 바꿉니다. */
    private JsonNode fetchWithoutUnknownDose(LocalDate birthDate, LocalDate today, List<String> doses) {
        try {
            return schedules.fetchSchedule(birthDate, today, doses);
        } catch (BusinessException e) {
            if (e.getErrorCode() != VaccinationErrorCode.UNKNOWN_DOSE) {
                throw e;
            }
            log.warn("일정에서 얻은 차수로 다시 조회했지만 AI가 모르는 차수라고 응답했습니다.");
            throw new BusinessException(VaccinationErrorCode.AI_UNAVAILABLE);
        }
    }
}

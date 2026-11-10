package com.ieum.dodam.vaccination.command.application;

import com.ieum.dodam.global.exception.BusinessException;
import com.ieum.dodam.medical_document.query.application.CareRelationPermissionService;
import com.ieum.dodam.user.domain.User;
import com.ieum.dodam.vaccination.domain.entity.VaccinationDoseCheck;
import com.ieum.dodam.vaccination.domain.repository.VaccinationDoseCheckRepository;
import com.ieum.dodam.vaccination.error.VaccinationErrorCode;
import com.ieum.dodam.vaccination.query.application.VaccinationQueryService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * 지난 접종 체크·해제입니다. 클래스에 트랜잭션을 걸지 않습니다. 저장은 저장소 자체 트랜잭션에서 끝나므로
 * 중복 저장 예외를 잡아도 바깥 트랜잭션이 rollback-only가 되지 않고, AI 호출도 DB 트랜잭션 밖에서 일어납니다.
 */
@Service
public class VaccinationCheckCommandService {

    private static final Logger log = LoggerFactory.getLogger(VaccinationCheckCommandService.class);
    private static final Pattern DOSE_ID = Pattern.compile("^[A-Z]+-[0-9]+$");

    private final CareRelationPermissionService permissions;
    private final VaccinationDoseCheckRepository checks;
    private final VaccinationQueryService queries;

    public VaccinationCheckCommandService(
            CareRelationPermissionService permissions,
            VaccinationDoseCheckRepository checks,
            VaccinationQueryService queries) {
        this.permissions = permissions;
        this.checks = checks;
        this.queries = queries;
    }

    /** 현재 일정에 있는 차수만 체크합니다. 이미 있으면 아무것도 하지 않습니다. */
    public void check(User actor, UUID patientId, String doseId) {
        requireFormat(doseId);
        UUID target = permissions.resolveTargetPatient(actor, patientId);
        if (checks.existsByPatientIdAndDoseId(target, doseId)) {
            return;
        }
        Set<String> valid = VaccinationQueryService.doseIds(queries.scheduleOf(target));
        if (valid.isEmpty()) {
            log.warn("예방접종 일정 응답에 차수 목록이 없습니다: patientId={}", target);
            throw new BusinessException(VaccinationErrorCode.AI_UNAVAILABLE);
        }
        if (!valid.contains(doseId)) {
            throw new BusinessException(VaccinationErrorCode.UNKNOWN_DOSE);
        }
        try {
            checks.saveAndFlush(VaccinationDoseCheck.create(target, doseId, actor.getId()));
        } catch (DataIntegrityViolationException e) {
            // 같은 체크가 동시에 먼저 저장된 경우만 성공입니다. FK 등 다른 위반은 그대로 올립니다.
            if (!checks.existsByPatientIdAndDoseId(target, doseId)) {
                throw e;
            }
        }
    }

    /** 체크를 해제합니다. 없어도 성공이며, 옛 차수도 지울 수 있게 일정 존재 여부는 보지 않습니다. */
    public void uncheck(User actor, UUID patientId, String doseId) {
        requireFormat(doseId);
        UUID target = permissions.resolveTargetPatient(actor, patientId);
        checks.deleteByPatientIdAndDoseId(target, doseId);
    }

    private static void requireFormat(String doseId) {
        if (doseId == null
                || doseId.length() > VaccinationDoseCheck.DOSE_ID_MAX_LENGTH
                || !DOSE_ID.matcher(doseId).matches()) {
            throw new BusinessException(VaccinationErrorCode.INVALID_DOSE_ID);
        }
    }
}

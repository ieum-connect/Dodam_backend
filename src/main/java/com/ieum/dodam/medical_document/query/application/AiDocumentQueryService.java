package com.ieum.dodam.medical_document.query.application;

import com.ieum.dodam.global.exception.BusinessException;
import com.ieum.dodam.global.status.AiJobStatus;
import com.ieum.dodam.global.status.ResultStatus;
import com.ieum.dodam.medical_document.domain.ExplanationSectionType;
import com.ieum.dodam.medical_document.domain.repository.AiDocumentExplanationRepository;
import com.ieum.dodam.medical_document.domain.repository.AiDocumentRepository;
import com.ieum.dodam.medical_document.error.AiDocumentErrorCode;
import com.ieum.dodam.medical_document.query.model.AiDocumentExplanationStatusView;
import com.ieum.dodam.medical_document.query.model.AiDocumentSectionItemView;
import com.ieum.dodam.medical_document.query.model.AiDocumentSectionRow;
import com.ieum.dodam.medical_document.query.model.AiDocumentSectionView;
import com.ieum.dodam.medical_document.query.model.AiDocumentSectionsView;
import com.ieum.dodam.medical_document.query.model.AiDocumentStatusRow;
import com.ieum.dodam.user.domain.User;
import com.ieum.dodam.user.query.application.UserQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class AiDocumentQueryService {

    private final AiDocumentRepository documents;
    private final AiDocumentExplanationRepository explanations;
    private final CareRelationPermissionService permissions;
    private final UserQueryService userQueries;

    /** 내부 사용자 식별자로 인증된 사용자를 조회한다. */
    public User requireUser(UUID userId) {
        return userQueries.requireById(userId);
    }

    /** 문서의 AI 설명 섹션과 섹션별 항목을 조회한다. */
    public AiDocumentSectionsView getSections(
            User actor,
            UUID documentId,
            String rawSectionType
    ) {
        requireDocumentPermission(actor, documentId);
        requireExplanationSucceeded(statusRow(documentId).getJobStatus());
        String sectionType = parseSectionType(rawSectionType);
        List<AiDocumentSectionRow> rows = explanations.findSectionRows(documentId, sectionType);

        Map<UUID, List<AiDocumentSectionRow>> grouped = new LinkedHashMap<>();
        for (AiDocumentSectionRow row : rows) {
            grouped.computeIfAbsent(parseUuid(row.getSectionId()), ignored -> new ArrayList<>()).add(row);
        }

        List<AiDocumentSectionView> sections = grouped.values().stream()
                .map(this::toSectionView)
                .toList();
        return new AiDocumentSectionsView(documentId, sections);
    }

    /** 문서의 AI 설명 작업 상태와 재시도 가능 여부를 조회한다. */
    public AiDocumentExplanationStatusView getExplanationStatus(User actor, UUID documentId) {
        requireDocumentPermission(actor, documentId);
        AiDocumentStatusRow row = statusRow(documentId);
        AiJobStatus jobStatus = enumValue(
                AiJobStatus.class,
                row.getJobStatus(),
                AiDocumentErrorCode.INVALID_FILTER
        );
        ResultStatus resultStatus = nullableEnum(ResultStatus.class, row.getResultStatus());
        return new AiDocumentExplanationStatusView(
                parseUuid(row.getDocumentId()),
                jobStatus,
                resultStatus,
                row.getCurrentStep(),
                row.getProgress(),
                parseDbInstant(row.getCompletedAt()),
                row.getFailedStep(),
                row.getErrorCode(),
                Boolean.TRUE.equals(row.getRetryable()),
                "/api/documents/" + documentId
        );
    }

    /** 동일 섹션의 조회 결과 행들을 하나의 섹션 모델로 묶는다. */
    private AiDocumentSectionView toSectionView(List<AiDocumentSectionRow> rows) {
        AiDocumentSectionRow first = rows.get(0);
        List<AiDocumentSectionItemView> items = rows.stream()
                .filter(row -> row.getSentenceId() != null)
                .map(row -> new AiDocumentSectionItemView(
                        parseUuid(row.getSentenceId()),
                        parseNullableUuid(row.getSourceItemId()),
                        row.getLabel(),
                        row.getDisplayValue(),
                        row.getUnit(),
                        Boolean.TRUE.equals(row.getHasSource()),
                        parseNullableUuid(row.getCitationId())
                ))
                .toList();
        return new AiDocumentSectionView(
                parseUuid(first.getSectionId()),
                enumValue(
                        ExplanationSectionType.class,
                        first.getSectionType(),
                        AiDocumentErrorCode.INVALID_SECTION_TYPE
                ),
                first.getSectionTitle(),
                items
        );
    }

    /** 문서가 존재하고 현재 사용자가 해당 환자의 문서를 볼 수 있는지 검증한다. */
    private void requireDocumentPermission(User actor, UUID documentId) {
        String patientId = documents.findPatientByDocumentId(documentId)
                .orElseThrow(() -> new BusinessException(AiDocumentErrorCode.DOCUMENT_NOT_FOUND))
                .getPatientId();
        permissions.requireDocumentAccess(actor, parseUuid(patientId));
    }

    /** 문서의 최신 분석 및 AI 설명 상태 행을 조회한다. */
    private AiDocumentStatusRow statusRow(UUID documentId) {
        return explanations.findStatusByDocumentId(documentId)
                .orElseThrow(() -> new BusinessException(AiDocumentErrorCode.DOCUMENT_NOT_FOUND));
    }

    /** AI 설명 작업이 성공적으로 완료된 상태인지 검증한다. */
    private static void requireExplanationSucceeded(String rawStatus) {
        if (!"SUCCEEDED".equals(rawStatus)) {
            throw new BusinessException(AiDocumentErrorCode.EXPLANATION_NOT_READY);
        }
    }

    /** 섹션 유형 필터를 검증하고 DB 조회용 값으로 변환한다. */
    private static String parseSectionType(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return enumValue(ExplanationSectionType.class, value, AiDocumentErrorCode.INVALID_SECTION_TYPE).name();
    }

    /** 문자열을 지정된 enum 값으로 변환하고 잘못된 값은 계약 오류로 처리한다. */
    private static <E extends Enum<E>> E enumValue(
            Class<E> type,
            String rawValue,
            AiDocumentErrorCode errorCode
    ) {
        if (rawValue == null || rawValue.isBlank()) {
            throw new BusinessException(errorCode);
        }
        try {
            return Enum.valueOf(type, rawValue.toUpperCase());
        } catch (IllegalArgumentException exception) {
            throw new BusinessException(errorCode);
        }
    }

    /** 비어 있는 문자열은 null로, 값이 있으면 enum으로 변환한다. */
    private static <E extends Enum<E>> E nullableEnum(Class<E> type, String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
            return null;
        }
        return Enum.valueOf(type, rawValue.toUpperCase());
    }

    /** PostgreSQL·H2 native 조회 시간 문자열을 공통 Instant로 변환한다. */
    private static Instant parseDbInstant(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = value.trim().replace(' ', 'T');
        if (normalized.matches(".*[+-]\\d{2}$")) {
            normalized = normalized + ":00";
        }
        try {
            return Instant.parse(normalized);
        } catch (DateTimeParseException ignored) {
            return OffsetDateTime.parse(normalized, DateTimeFormatter.ISO_OFFSET_DATE_TIME).toInstant();
        }
    }

    /** 필수 UUID 문자열을 변환하고 변환 실패 시 문서 없음 오류를 발생시킨다. */
    private static UUID parseUuid(String value) {
        if (value == null || value.isBlank()) {
            throw new BusinessException(AiDocumentErrorCode.DOCUMENT_NOT_FOUND);
        }
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException exception) {
            throw new BusinessException(AiDocumentErrorCode.DOCUMENT_NOT_FOUND);
        }
    }

    /** nullable UUID 문자열을 null을 보존한 채 변환한다. */
    private static UUID parseNullableUuid(String value) {
        return value == null || value.isBlank() ? null : parseUuid(value);
    }
}

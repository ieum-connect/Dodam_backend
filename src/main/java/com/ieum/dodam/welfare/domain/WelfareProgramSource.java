package com.ieum.dodam.welfare.domain;

import com.fasterxml.jackson.databind.JsonNode;

import java.time.Instant;

/** 정부24 원본을 카탈로그 도메인에 전달하는 불변 입력. */
public record WelfareProgramSource(
        String externalServiceId,
        String name,
        String supportType,
        String purposeSummary,
        String purposeDetail,
        String targetText,
        String selectionCriteriaText,
        String benefitText,
        String applicationMethod,
        String applicationDeadline,
        String applicationDeadlineType,
        String requiredDocuments,
        String officialCheckDocuments,
        String identityCheckDocuments,
        String onlineApplicationUrl,
        String detailUrl,
        String agencyCode,
        String agencyName,
        String agencyType,
        String departmentName,
        String receptionAgency,
        String contact,
        String serviceCategory,
        String userCategory,
        Long viewCount,
        Instant registeredAtSource,
        Instant modifiedAtSource,
        boolean childCandidate,
        JsonNode rawListJson,
        JsonNode rawDetailJson,
        JsonNode rawConditionJson
) {
    public WelfareProgramSource {
        if (externalServiceId == null || externalServiceId.isBlank()) {
            throw new IllegalArgumentException("외부 서비스ID는 필수입니다.");
        }
        if (name == null || name.isBlank()) throw new IllegalArgumentException("서비스명은 필수입니다.");
    }
}

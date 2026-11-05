package com.ieum.dodam.welfare.infrastructure.gov24.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/** 공공서비스 목록 API의 한 항목. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record Gov24ServiceListItem(
        @JsonProperty("서비스ID") String serviceId,
        @JsonProperty("지원유형") String supportType,
        @JsonProperty("서비스명") String serviceName,
        @JsonProperty("서비스목적요약") String purposeSummary,
        @JsonProperty("지원대상") String target,
        @JsonProperty("선정기준") String selectionCriteria,
        @JsonProperty("지원내용") String benefit,
        @JsonProperty("신청방법") String applicationMethod,
        @JsonProperty("신청기한") String applicationDeadline,
        @JsonProperty("상세조회URL") String detailUrl,
        @JsonProperty("소관기관코드") String agencyCode,
        @JsonProperty("소관기관명") String agencyName,
        @JsonProperty("부서명") String departmentName,
        @JsonProperty("조회수") Long viewCount,
        @JsonProperty("소관기관유형") String agencyType,
        @JsonProperty("사용자구분") String userCategory,
        @JsonProperty("서비스분야") String serviceCategory,
        @JsonProperty("접수기관") String receptionAgency,
        @JsonProperty("전화문의") String contact,
        @JsonProperty("등록일시") String registeredAt,
        @JsonProperty("수정일시") String modifiedAt,
        @JsonProperty("신청기한유형") String deadlineType
) {
}

package com.ieum.dodam.welfare.infrastructure.gov24.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/** 공공서비스 상세 API의 한 항목. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record Gov24ServiceDetailItem(
        @JsonProperty("서비스ID") String serviceId,
        @JsonProperty("지원유형") String supportType,
        @JsonProperty("서비스명") String serviceName,
        @JsonProperty("서비스목적") String purpose,
        @JsonProperty("신청기한") String applicationDeadline,
        @JsonProperty("지원대상") String target,
        @JsonProperty("선정기준") String selectionCriteria,
        @JsonProperty("지원내용") String benefit,
        @JsonProperty("신청방법") String applicationMethod,
        @JsonProperty("구비서류") String requiredDocuments,
        @JsonProperty("접수기관명") String receptionAgency,
        @JsonProperty("문의처") String contact,
        @JsonProperty("온라인신청사이트URL") String onlineApplicationUrl,
        @JsonProperty("수정일시") String modifiedAt,
        @JsonProperty("소관기관명") String agencyName,
        @JsonProperty("행정규칙") String administrativeRule,
        @JsonProperty("자치법규") String localOrdinance,
        @JsonProperty("법령") String statute,
        @JsonProperty("공무원확인구비서류") String officialCheckDocuments,
        @JsonProperty("본인확인필요구비서류") String identityCheckDocuments,
        @JsonProperty("신청기한유형") String deadlineType
) {
}

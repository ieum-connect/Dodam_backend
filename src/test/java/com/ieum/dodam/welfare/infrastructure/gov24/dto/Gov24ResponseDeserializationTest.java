package com.ieum.dodam.welfare.infrastructure.gov24.dto;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class Gov24ResponseDeserializationTest {
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void readsKoreanServiceListFields() throws Exception {
        String json = """
                {"page":1,"perPage":10,"totalCount":1,"currentCount":1,"matchCount":1,"data":[{
                  "서비스ID":"SVC-1","지원유형":"현금","서비스명":"영유아 지원",
                  "서비스목적요약":"양육 부담 경감","지원대상":"만 7세 이하","선정기준":"지역 거주",
                  "지원내용":"월별 지원","신청방법":"온라인","신청기한":"상시",
                  "상세조회URL":"https://gov.example/SVC-1","소관기관명":"행정안전부",
                  "수정일시":"2026-11-05 12:00:00","신청기한유형":"상시"
                }]}
                """;

        Gov24PageResponse<Gov24ServiceListItem> response = objectMapper.readValue(
                json, new TypeReference<>() {});

        assertThat(response.totalCount()).isOne();
        assertThat(response.data()).singleElement().satisfies(item -> {
            assertThat(item.serviceId()).isEqualTo("SVC-1");
            assertThat(item.serviceName()).isEqualTo("영유아 지원");
            assertThat(item.target()).isEqualTo("만 7세 이하");
            assertThat(item.deadlineType()).isEqualTo("상시");
        });
    }

    @Test
    void readsDetailDocumentsAndApplicationUrl() throws Exception {
        String json = """
                {"page":1,"perPage":1,"totalCount":1,"currentCount":1,"matchCount":1,"data":[{
                  "서비스ID":"SVC-1","서비스명":"영유아 지원","서비스목적":"돌봄 지원",
                  "구비서류":"신청서","공무원확인구비서류":"주민등록정보",
                  "본인확인필요구비서류":"신분증","온라인신청사이트URL":"https://apply.example"
                }]}
                """;

        Gov24PageResponse<Gov24ServiceDetailItem> response = objectMapper.readValue(
                json, new TypeReference<>() {});

        assertThat(response.data()).singleElement().satisfies(item -> {
            assertThat(item.requiredDocuments()).isEqualTo("신청서");
            assertThat(item.officialCheckDocuments()).isEqualTo("주민등록정보");
            assertThat(item.onlineApplicationUrl()).isEqualTo("https://apply.example");
        });
    }

    @Test
    void preservesUnknownJaConditionCodes() throws Exception {
        String json = """
                {"page":1,"perPage":1,"totalCount":1,"currentCount":1,"matchCount":1,"data":[{
                  "서비스ID":"SVC-1","서비스명":"영유아 지원","JA0101":"Y","JA0110":7,
                  "추가필드":"임의 값"
                }]}
                """;

        Gov24PageResponse<Gov24SupportConditionsItem> response = objectMapper.readValue(
                json, new TypeReference<>() {});

        assertThat(response.data()).singleElement().satisfies(item -> {
            assertThat(item.getServiceId()).isEqualTo("SVC-1");
            assertThat(item.getConditionCodes()).containsOnlyKeys("JA0101", "JA0110");
            assertThat(item.getConditionCodes().get("JA0110").asInt()).isEqualTo(7);
        });
    }
}

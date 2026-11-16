package com.ieum.dodam.welfare.command.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ieum.dodam.welfare.domain.WelfareProgramSource;
import com.ieum.dodam.welfare.domain.repository.WelfareSyncLogRepository;
import com.ieum.dodam.welfare.infrastructure.gov24.Gov24WelfareClient;
import com.ieum.dodam.welfare.infrastructure.gov24.dto.Gov24PageResponse;
import com.ieum.dodam.welfare.infrastructure.gov24.dto.Gov24ServiceDetailItem;
import com.ieum.dodam.welfare.infrastructure.gov24.dto.Gov24ServiceListItem;
import com.ieum.dodam.welfare.infrastructure.gov24.dto.Gov24SupportConditionsItem;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class WelfareSyncServiceTest {
    private final Gov24WelfareClient client = mock(Gov24WelfareClient.class);
    private final WelfareCatalogService catalog = mock(WelfareCatalogService.class);
    private final WelfareSyncLogRepository logs = mock(WelfareSyncLogRepository.class);
    private final ObjectMapper mapper = new ObjectMapper();
    private final WelfareSyncService service = new WelfareSyncService(client, catalog, logs, mapper,
            Clock.fixed(Instant.parse("2026-11-17T00:00:00Z"), ZoneOffset.UTC));

    @Test
    void excludesProfessionalTrainingEvenWhenEducationKeywordsScoreHighly() {
        Gov24ServiceListItem item = item("FTA 전문인력 교육", "재직자 교육 대상", "교육");
        when(client.serviceList(1, 100, null)).thenReturn(page(item));

        assertThat(service.synchronizeAll(100, 10)).isZero();
        verify(catalog, never()).upsert(any());
    }

    @Test
    void enrichesChangedCandidateWithAllConditionsWithinBatchLimit() throws Exception {
        Gov24ServiceListItem item = item("아이돌봄 지원", "아동 양육 가정", "보육");
        when(client.serviceList(1, 100, null)).thenReturn(page(item));
        when(catalog.needsEnrichment(any())).thenReturn(true);
        when(client.serviceDetail("SVC-1")).thenReturn(new Gov24PageResponse<>(1, 1, 1, 1, 1,
                List.of(detail())));
        Gov24SupportConditionsItem first = mapper.readValue("{\"서비스ID\":\"SVC-1\",\"JA0101\":\"A\"}", Gov24SupportConditionsItem.class);
        Gov24SupportConditionsItem second = mapper.readValue("{\"서비스ID\":\"SVC-1\",\"JA0102\":\"B\"}", Gov24SupportConditionsItem.class);
        when(client.supportConditions(1, 1000, "SVC-1"))
                .thenReturn(new Gov24PageResponse<>(1, 1000, 2, 2, 2, List.of(first, second)));

        assertThat(service.synchronizeAll(100, 1)).isEqualTo(1);

        ArgumentCaptor<WelfareProgramSource> source = ArgumentCaptor.forClass(WelfareProgramSource.class);
        verify(catalog).upsert(source.capture());
        assertThat(source.getValue().rawDetailJson().get("서비스ID").asText()).isEqualTo("SVC-1");
        assertThat(source.getValue().rawConditionJson().isArray()).isTrue();
        assertThat(source.getValue().rawConditionJson().size()).isEqualTo(2);
    }

    private Gov24PageResponse<Gov24ServiceListItem> page(Gov24ServiceListItem item) {
        return new Gov24PageResponse<>(1, 100, 1, 1, 1, List.of(item));
    }

    private Gov24ServiceListItem item(String name, String target, String category) {
        return new Gov24ServiceListItem("SVC-1", "서비스", name, "목적", target, "기준", "내용", "온라인",
                "상시", "https://detail", "A1", "보건복지부", "부서", 1L, "중앙행정기관", "가정", category,
                "주민센터", "02-0000", "2026-11-01T00:00:00Z", "2026-11-17T00:00:00Z", "상시");
    }

    private Gov24ServiceDetailItem detail() {
        return new Gov24ServiceDetailItem("SVC-1", "서비스", "아이돌봄 지원", "상세 목적", "상시", "아동 양육 가정",
                "기준", "내용", "온라인", "신청서", "주민센터", "02-0000", "https://apply",
                "2026-11-17T00:00:00Z", "보건복지부", null, null, null, "공무원 확인", "본인 확인", "상시");
    }
}

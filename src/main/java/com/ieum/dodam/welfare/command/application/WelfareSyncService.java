package com.ieum.dodam.welfare.command.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ieum.dodam.welfare.domain.WelfareProgramSource;
import com.ieum.dodam.welfare.domain.entity.WelfareSyncLog;
import com.ieum.dodam.welfare.infrastructure.gov24.Gov24WelfareClient;
import com.ieum.dodam.welfare.infrastructure.gov24.Gov24WelfareClientException;
import com.ieum.dodam.welfare.infrastructure.gov24.dto.Gov24PageResponse;
import com.ieum.dodam.welfare.infrastructure.gov24.dto.Gov24ServiceListItem;
import com.ieum.dodam.welfare.domain.repository.WelfareSyncLogRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Locale;

@Service
public class WelfareSyncService {
    private static final List<String> STRONG_CHILD_KEYWORDS = List.of(
            "영아", "유아", "영유아", "아동수당", "부모급여", "보육료", "어린이집", "유치원",
            "가정양육", "아이돌봄", "출산", "임신", "아동복지", "아동양육");
    private static final List<String> SUPPORT_KEYWORDS = List.of("양육", "보육", "돌봄", "교육", "자녀", "아동");
    private static final List<String> EXCLUSION_KEYWORDS = List.of(
            "성폭력", "범죄피해", "피해자", "국선변호사", "진술조력", "해양사고", "어선", "옵서버");
    private final Gov24WelfareClient client;
    private final WelfareCatalogService catalog;
    private final WelfareSyncLogRepository logs;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    public WelfareSyncService(Gov24WelfareClient client, WelfareCatalogService catalog,
                              WelfareSyncLogRepository logs, ObjectMapper objectMapper) {
        this(client, catalog, logs, objectMapper, Clock.systemUTC());
    }

    WelfareSyncService(Gov24WelfareClient client, WelfareCatalogService catalog,
                       WelfareSyncLogRepository logs, ObjectMapper objectMapper, Clock clock) {
        this.client = client; this.catalog = catalog; this.logs = logs; this.objectMapper = objectMapper; this.clock = clock;
    }

    @Transactional
    public int synchronizeAll(int perPage) {
        int page = 1;
        int saved = 0;
        while (true) {
            Instant started = clock.instant();
            Gov24PageResponse<Gov24ServiceListItem> response = client.serviceList(page, perPage, null);
            for (Gov24ServiceListItem item : response.data()) {
                if (!isChildCandidate(item)) continue;
                catalog.upsert(toSource(item));
                saved++;
            }
            logs.save(WelfareSyncLog.completed(WelfareSyncLog.Endpoint.SERVICE_LIST,
                    WelfareSyncLog.SyncType.FULL, page, response.currentCount(), started, clock.instant()));
            if (response.data().isEmpty() || page * (long) perPage >= response.totalCount()) break;
            page++;
        }
        return saved;
    }

    private boolean isChildCandidate(Gov24ServiceListItem item) {
        String name = safe(item.serviceName());
        String purpose = safe(item.purposeSummary());
        String target = safe(item.target());
        String category = safe(item.serviceCategory());
        String all = String.join(" ", name, purpose, target, category).toLowerCase(Locale.ROOT);
        if (EXCLUSION_KEYWORDS.stream().anyMatch(all::contains)) return false;

        int score = 0;
        score += STRONG_CHILD_KEYWORDS.stream().filter(name::contains).mapToInt(value -> 3).sum();
        score += STRONG_CHILD_KEYWORDS.stream().filter(target::contains).mapToInt(value -> 3).sum();
        score += STRONG_CHILD_KEYWORDS.stream().filter(purpose::contains).mapToInt(value -> 2).sum();
        score += SUPPORT_KEYWORDS.stream().filter(category::contains).mapToInt(value -> 1).sum();
        score += SUPPORT_KEYWORDS.stream().filter(name::contains).mapToInt(value -> 1).sum();
        return score >= 3;
    }

    private String safe(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT);
    }

    private WelfareProgramSource toSource(Gov24ServiceListItem item) {
        JsonNode raw = objectMapper.valueToTree(item);
        return new WelfareProgramSource(item.serviceId(), item.serviceName(), item.supportType(), item.purposeSummary(),
                null, item.target(), item.selectionCriteria(), item.benefit(), item.applicationMethod(),
                item.applicationDeadline(), item.deadlineType(), null, null, null, null, item.detailUrl(),
                item.agencyCode(), item.agencyName(), item.agencyType(), item.departmentName(), item.receptionAgency(),
                item.contact(), item.serviceCategory(), item.userCategory(), item.viewCount(), null, parseInstant(item.modifiedAt()),
                isChildCandidate(item), raw, objectMapper.createObjectNode(), objectMapper.createObjectNode());
    }

    private Instant parseInstant(String value) {
        if (value == null || value.isBlank()) return null;
        try { return Instant.parse(value); } catch (Exception ignored) { return null; }
    }
}

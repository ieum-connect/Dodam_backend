package com.ieum.dodam.welfare.api.controller;

import com.ieum.dodam.welfare.domain.entity.WelfareProgram;
import com.ieum.dodam.welfare.query.application.WelfareProgramQueryService;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/welfare-programs")
public class WelfareProgramApiController {
    private final WelfareProgramQueryService queries;
    public WelfareProgramApiController(WelfareProgramQueryService queries) { this.queries = queries; }

    @GetMapping
    public List<ProgramResponse> list(@RequestParam(required = false) String query,
                                      @RequestParam(defaultValue = "20") int size) {
        return queries.list(query, size).stream().map(ProgramResponse::from).toList();
    }

    @GetMapping("/{programId}")
    public ProgramResponse detail(@PathVariable UUID programId) {
        return ProgramResponse.from(queries.get(programId));
    }

    public record ProgramResponse(UUID id, String externalServiceId, String name, String supportType,
                                  String purposeSummary, String purposeDetail, String target,
                                  String selectionCriteria, String benefit, String applicationMethod,
                                  String applicationDeadline, String requiredDocuments, String onlineApplicationUrl,
                                  String detailUrl, String agencyName, String contact, boolean active,
                                  java.time.Instant modifiedAtSource, java.time.Instant lastSyncedAt) {
        static ProgramResponse from(WelfareProgram p) {
            String applyUrl = p.getOnlineApplicationUrl() == null || p.getOnlineApplicationUrl().isBlank()
                    ? p.getDetailUrl() : p.getOnlineApplicationUrl();
            return new ProgramResponse(p.getId(), p.getExternalServiceId(), p.getName(), p.getSupportType(),
                    p.getPurposeSummary(), p.getPurposeDetail(), p.getTargetText(), p.getSelectionCriteriaText(),
                    p.getBenefitText(), p.getApplicationMethod(), p.getApplicationDeadline(), p.getRequiredDocuments(),
                    applyUrl, p.getDetailUrl(), p.getAgencyName(), p.getContact(), p.isActive(),
                    p.getModifiedAtSource(), p.getLastSyncedAt());
        }
    }
}

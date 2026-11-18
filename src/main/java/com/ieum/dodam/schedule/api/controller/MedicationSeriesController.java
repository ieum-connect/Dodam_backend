package com.ieum.dodam.schedule.api.controller;
import com.ieum.dodam.schedule.command.application.MedicationSeriesService;
import com.ieum.dodam.schedule.domain.MedicationSeries;
import com.ieum.dodam.user.query.application.UserQueryService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import java.time.*;
import java.util.*;
@RestController @RequestMapping("/api/medication-series")
public class MedicationSeriesController {
    private final MedicationSeriesService service; private final UserQueryService users;
    public MedicationSeriesController(MedicationSeriesService service,UserQueryService users){this.service=service;this.users=users;}
    public record Write(@NotNull UUID patientId,@NotNull UUID requestId,@NotBlank @Size(max=200) String title,@Size(max=500) String notes,@NotNull Instant startsAt,@NotBlank @Size(max=80) String timeZone,LocalDate endDate,@Min(1) @Max(127) Integer weekdayMask){}
    public record RuleUpdate(@NotNull Long version, @NotNull @Min(1) @Max(127) Integer weekdayMask, @NotNull LocalTime timeOfDay, LocalDate endDate) {}

    @PostMapping public MedicationSeries create(@AuthenticationPrincipal OAuth2User p,@Valid @RequestBody Write r){return service.create(users.requireByKakaoId(String.valueOf((Object)p.getAttribute("id"))),r.patientId(),r.requestId(),r.title(),r.notes(),r.startsAt(),r.timeZone(),r.endDate(),r.weekdayMask()==null?127:r.weekdayMask());}
    @PutMapping("/{id}")
    public MedicationSeries update(@AuthenticationPrincipal OAuth2User p, @PathVariable UUID id, @Valid @RequestBody RuleUpdate r) {
        return service.update(users.requireByKakaoId(String.valueOf((Object) p.getAttribute("id"))), id, r.version(), r.weekdayMask(), r.timeOfDay(), r.endDate());
    }
    @GetMapping public List<MedicationSeries> list(@AuthenticationPrincipal OAuth2User p,@RequestParam UUID patientId){return service.list(users.requireByKakaoId(String.valueOf((Object)p.getAttribute("id"))),patientId);}
    @PostMapping("/{id}/stop") public MedicationSeries stop(@AuthenticationPrincipal OAuth2User p,@PathVariable UUID id){return service.stop(users.requireByKakaoId(String.valueOf((Object)p.getAttribute("id"))),id);}
}

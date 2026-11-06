package com.ieum.dodam.schedule.api.controller;

import com.ieum.dodam.action_item.domain.ActionItemApprovalStatus;
import com.ieum.dodam.action_item.domain.ActionItemType;
import com.ieum.dodam.action_item.domain.entity.ActionItem;
import com.ieum.dodam.schedule.command.application.CalendarService;
import com.ieum.dodam.schedule.domain.Schedule;
import com.ieum.dodam.schedule.domain.ScheduleType;
import com.ieum.dodam.user.domain.User;
import com.ieum.dodam.user.query.application.UserQueryService;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.*;

@RestController
@RequestMapping("/api")
public class CalendarApiController {
    private final CalendarService calendar;
    private final UserQueryService users;

    public CalendarApiController(CalendarService calendar, UserQueryService users) {
        this.calendar = calendar;
        this.users = users;
    }

    public record Write(
            @NotNull UUID patientId,
            @NotBlank @Size(max = 200) String title,
            @NotNull ScheduleType scheduleType,
            @NotNull Instant startsAt) {}

    public record Decision(Instant startsAt) {}

    public record Candidate(
            UUID id,
            UUID documentId,
            ActionItemType type,
            String title,
            String description,
            Instant dueAt,
            ActionItemApprovalStatus approvalStatus) {
        private static Candidate from(ActionItem item) {
            return new Candidate(
                    item.getId(),
                    item.getDocumentId(),
                    item.getType(),
                    item.getTitle(),
                    item.getDescription(),
                    item.getDueAt(),
                    item.getApprovalStatus());
        }
    }

    @GetMapping("/action-items")
    public List<Candidate> candidates(
            @AuthenticationPrincipal OAuth2User principal, @RequestParam UUID patientId) {
        return calendar.candidates(actor(principal), patientId).stream()
                .map(Candidate::from)
                .toList();
    }

    @PostMapping("/action-items/{id}/approve")
    public Schedule approve(
            @AuthenticationPrincipal OAuth2User principal,
            @PathVariable UUID id,
            @RequestBody(required = false) Decision body) {
        return calendar.decide(actor(principal), id, true, body == null ? null : body.startsAt());
    }

    @PostMapping("/action-items/{id}/reject")
    @ResponseStatus(org.springframework.http.HttpStatus.NO_CONTENT)
    public void reject(@AuthenticationPrincipal OAuth2User principal, @PathVariable UUID id) {
        calendar.decide(actor(principal), id, false, null);
    }

    @GetMapping("/schedules")
    public List<Schedule> list(
            @AuthenticationPrincipal OAuth2User principal,
            @RequestParam UUID patientId,
            @RequestParam Instant from,
            @RequestParam Instant to) {
        return calendar.list(actor(principal), patientId, from, to);
    }

    @PostMapping("/schedules")
    @ResponseStatus(org.springframework.http.HttpStatus.CREATED)
    public Schedule create(
            @AuthenticationPrincipal OAuth2User principal, @Valid @RequestBody Write body) {
        return calendar.create(
                actor(principal),
                body.patientId(),
                body.title(),
                body.scheduleType(),
                body.startsAt());
    }

    private User actor(OAuth2User principal) {
        return users.requireByKakaoId(String.valueOf((Object) principal.getAttribute("id")));
    }
}

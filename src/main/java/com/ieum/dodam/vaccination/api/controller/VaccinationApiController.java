package com.ieum.dodam.vaccination.api.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.ieum.dodam.user.domain.User;
import com.ieum.dodam.user.query.application.UserQueryService;
import com.ieum.dodam.vaccination.command.application.VaccinationCheckCommandService;
import com.ieum.dodam.vaccination.query.application.VaccinationQueryService;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/vaccinations")
public class VaccinationApiController {

    private final VaccinationQueryService queries;
    private final VaccinationCheckCommandService commands;
    private final UserQueryService users;

    public VaccinationApiController(
            VaccinationQueryService queries,
            VaccinationCheckCommandService commands,
            UserQueryService users) {
        this.queries = queries;
        this.commands = commands;
        this.users = users;
    }

    /** AI 일정 응답을 해석하지 않고 그대로 전달합니다. */
    @GetMapping
    public JsonNode schedule(@AuthenticationPrincipal OAuth2User principal, @RequestParam UUID patientId) {
        return queries.schedule(actor(principal), patientId);
    }

    @PutMapping("/checks/{doseId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void check(@AuthenticationPrincipal OAuth2User principal,
                      @PathVariable String doseId,
                      @RequestParam UUID patientId) {
        commands.check(actor(principal), patientId, doseId);
    }

    @DeleteMapping("/checks/{doseId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void uncheck(@AuthenticationPrincipal OAuth2User principal,
                        @PathVariable String doseId,
                        @RequestParam UUID patientId) {
        commands.uncheck(actor(principal), patientId, doseId);
    }

    private User actor(OAuth2User principal) {
        return users.requireByKakaoId(String.valueOf(principal.getAttributes().get("id")));
    }
}

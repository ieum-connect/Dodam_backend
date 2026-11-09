package com.ieum.dodam.growth.api;

import com.ieum.dodam.growth.api.dto.*;
import com.ieum.dodam.growth.application.GrowthService;
import com.ieum.dodam.user.domain.User;
import com.ieum.dodam.user.query.application.UserQueryService;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/growth-records")
public class GrowthApiController {
    private final GrowthService service;
    private final UserQueryService users;

    public GrowthApiController(GrowthService service, UserQueryService users) {
        this.service = service;
        this.users = users;
    }

    @GetMapping
    public List<GrowthResponse> list(
            @AuthenticationPrincipal OAuth2User principal, @RequestParam UUID patientId) {
        return service.list(actor(principal), patientId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public GrowthResponse create(
            @AuthenticationPrincipal OAuth2User principal,
            @Valid @RequestBody GrowthWriteRequest request) {
        return service.create(actor(principal), request);
    }

    @PutMapping("/{id}")
    public GrowthResponse update(
            @AuthenticationPrincipal OAuth2User principal,
            @PathVariable UUID id,
            @Valid @RequestBody GrowthWriteRequest request) {
        return service.update(actor(principal), id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
            @AuthenticationPrincipal OAuth2User principal,
            @PathVariable UUID id,
            @RequestParam long version) {
        service.delete(actor(principal), id, version);
    }

    private User actor(OAuth2User principal) {
        return users.requireByKakaoId(String.valueOf((Object) principal.getAttribute("id")));
    }
}

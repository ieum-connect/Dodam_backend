package com.ieum.dodam.welfare.api.controller;

import com.ieum.dodam.welfare.domain.entity.WelfareFavorite;
import com.ieum.dodam.welfare.domain.repository.WelfareFavoriteRepository;
import com.ieum.dodam.medical_document.query.application.CareRelationPermissionService;
import com.ieum.dodam.user.domain.User;
import com.ieum.dodam.user.query.application.UserQueryService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/welfare-favorites")
public class WelfareFavoriteApiController {
    private final WelfareFavoriteRepository favorites;
    private final CareRelationPermissionService permissions;
    private final UserQueryService users;

    public WelfareFavoriteApiController(WelfareFavoriteRepository favorites,
                                        CareRelationPermissionService permissions,
                                        UserQueryService users) {
        this.favorites = favorites;
        this.permissions = permissions;
        this.users = users;
    }

    @GetMapping
    public List<UUID> list(@AuthenticationPrincipal OAuth2User principal, @RequestParam UUID patientId) {
        patientId = target(principal, patientId);
        return favorites.findByPatientIdOrderByCreatedAtDesc(patientId).stream()
                .map(WelfareFavorite::getProgramId)
                .toList();
    }

    @PostMapping
    public WelfareFavorite add(@AuthenticationPrincipal OAuth2User principal, @RequestParam UUID patientId, @RequestParam UUID programId) {
        UUID targetPatientId = target(principal, patientId);
        return favorites.findByPatientIdAndProgramId(targetPatientId, programId)
                .orElseGet(() -> favorites.save(WelfareFavorite.create(targetPatientId, programId)));
    }

    @DeleteMapping("/{programId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void remove(@AuthenticationPrincipal OAuth2User principal, @PathVariable UUID programId, @RequestParam UUID patientId) {
        patientId = target(principal, patientId);
        favorites.findByPatientIdAndProgramId(patientId, programId).ifPresent(favorites::delete);
    }

    private UUID target(OAuth2User principal, UUID patientId) {
        User actor = users.requireByKakaoId(String.valueOf(principal.getAttribute("id")));
        return permissions.resolveTargetPatient(actor, patientId);
    }
}

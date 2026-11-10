package com.ieum.dodam.welfare.api.controller;

import com.ieum.dodam.welfare.command.application.WelfareRecommendationService;
import com.ieum.dodam.medical_document.query.application.CareRelationPermissionService;
import com.ieum.dodam.user.domain.User;
import com.ieum.dodam.user.query.application.UserQueryService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;

@RestController
@RequestMapping("/api/welfare-recommendations")
public class WelfareRecommendationApiController {
    private final WelfareRecommendationService recommendations;
    private final CareRelationPermissionService permissions;
    private final UserQueryService users;
    public WelfareRecommendationApiController(WelfareRecommendationService recommendations,
                                               CareRelationPermissionService permissions,
                                               UserQueryService users) {
        this.recommendations = recommendations;
        this.permissions = permissions;
        this.users = users;
    }
    @GetMapping
    public WelfareRecommendationService.RecommendationResult list(@AuthenticationPrincipal OAuth2User principal,
                                                                    @RequestParam UUID patientId,
                                                                    @RequestParam(defaultValue = "SUGGESTIVE") String type) {
        return recommendations.recommend(permissions.resolveTargetPatient(actor(principal), patientId),
                "PROACTIVE".equalsIgnoreCase(type));
    }

    private User actor(OAuth2User principal) {
        return users.requireByKakaoId(String.valueOf(principal.getAttributes().get("id")));
    }
}

package com.ieum.dodam.welfare.api.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.ieum.dodam.medical_document.query.application.CareRelationPermissionService;
import com.ieum.dodam.user.domain.User;
import com.ieum.dodam.user.query.application.UserQueryService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClient;

import java.util.UUID;

@RestController
@RequestMapping("/api/welfare-explanations")
public class WelfareExplanationApiController {
    private final RestClient aiClient;
    private final CareRelationPermissionService permissions;
    private final UserQueryService users;

    public WelfareExplanationApiController(
            RestClient.Builder builder,
            @Value("${dodam.ai.base-url}") String aiBaseUrl,
            @Value("${dodam.ai.internal-token}") String internalToken,
            CareRelationPermissionService permissions,
            UserQueryService users) {
        this.aiClient = builder.baseUrl(aiBaseUrl.replaceAll("/+$", ""))
                .defaultHeader("X-Internal-Token", internalToken)
                .build();
        this.permissions = permissions;
        this.users = users;
    }

    @PostMapping
    public JsonNode explain(@AuthenticationPrincipal OAuth2User principal,
                            @RequestParam UUID patientId,
                            @RequestBody JsonNode request) {
        User actor = users.requireByKakaoId(String.valueOf(principal.getAttributes().get("id")));
        permissions.resolveTargetPatient(actor, patientId);
        return aiClient.post()
                .uri("/api/v1/welfare/explanations")
                .body(request)
                .retrieve()
                .body(JsonNode.class);
    }
}

package com.ieum.dodam.care_relation.api.controller;

import com.ieum.dodam.care_relation.api.dto.response.CareRelationResponse;
import com.ieum.dodam.care_relation.api.mapper.CareRelationResponseMapper;
import com.ieum.dodam.care_relation.command.application.CareRelationCommandService;
import com.ieum.dodam.care_relation.query.application.CareRelationQueryService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
public class CareRelationApiController {

    private final CareRelationCommandService commands;
    private final CareRelationQueryService queries;
    private final CareRelationResponseMapper responseMapper;

    public CareRelationApiController(
            CareRelationCommandService commands,
            CareRelationQueryService queries,
            CareRelationResponseMapper responseMapper
    ) {
        this.commands = commands;
        this.queries = queries;
        this.responseMapper = responseMapper;
    }

    /** 현재 사용자와 관련된 개인·보호자 연결 목록을 반환한다. */
    @GetMapping("/api/care-relations")
    public List<CareRelationResponse> list(@AuthenticationPrincipal OAuth2User user) {
        return responseMapper.toResponses(queries.listFor(queries.requireUser(kakaoId(user))));
    }

    /** 개인이 대기 중인 연결 요청을 수락한다. */
    @PostMapping("/api/care-relations/{id}/accept")
    public ResponseEntity<?> accept(@AuthenticationPrincipal OAuth2User user, @PathVariable UUID id) {
        return mutate(user, () -> commands.accept(queries.requireUser(kakaoId(user)), id));
    }

    /** 개인이 대기 중인 연결 요청을 거절한다. */
    @PostMapping("/api/care-relations/{id}/reject")
    public ResponseEntity<?> reject(@AuthenticationPrincipal OAuth2User user, @PathVariable UUID id) {
        return mutate(user, () -> commands.reject(queries.requireUser(kakaoId(user)), id));
    }

    private ResponseEntity<List<CareRelationResponse>> mutate(OAuth2User user, Runnable action) {
        action.run();
        return ResponseEntity.ok(responseMapper.toResponses(queries.listFor(queries.requireUser(kakaoId(user)))));
    }

    private static String kakaoId(OAuth2User user) {
        Object id = user.getAttributes().get("id");
        return id == null ? "" : String.valueOf(id);
    }
}

package com.ieum.dodam.welfare.api.controller;

import com.ieum.dodam.welfare.query.application.WelfareProgramQueryService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

/** AI Backend 전용 복지 카탈로그 조회 API. */
@RestController
@RequestMapping("/internal/welfare-programs")
public class InternalWelfareProgramApiController {
    private final WelfareProgramQueryService queries;
    private final String internalToken;

    public InternalWelfareProgramApiController(
            WelfareProgramQueryService queries,
            @Value("${dodam.ai.internal-token:}") String internalToken) {
        this.queries = queries;
        this.internalToken = internalToken;
    }

    @GetMapping
    public List<WelfareProgramApiController.ProgramResponse> list(
            @RequestHeader(value = "X-Internal-Token", required = false) String token,
            @RequestParam(required = false) String query,
            @RequestParam(defaultValue = "20") int size) {
        requireToken(token);
        return queries.list(query, size).stream()
                .map(WelfareProgramApiController.ProgramResponse::from)
                .toList();
    }

    @GetMapping("/{programId}")
    public WelfareProgramApiController.ProgramResponse detail(
            @RequestHeader(value = "X-Internal-Token", required = false) String token,
            @PathVariable UUID programId) {
        requireToken(token);
        return WelfareProgramApiController.ProgramResponse.from(queries.get(programId));
    }

    private void requireToken(String token) {
        if (internalToken.isBlank() || token == null || !internalToken.equals(token)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Internal token is invalid.");
        }
    }
}

package com.ieum.dodam.medical_document.api.controller;

import com.ieum.dodam.medical_document.query.application.OriginalDocumentQueryService;
import com.ieum.dodam.user.query.application.UserQueryService;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.*;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.UUID;

@RestController
public class OriginalDocumentController {
    private final OriginalDocumentQueryService originals;
    private final UserQueryService users;
    public OriginalDocumentController(OriginalDocumentQueryService originals, UserQueryService users) {
        this.originals = originals;
        this.users = users;
    }
    @GetMapping("/api/documents/{documentId}/original")
    public ResponseEntity<byte[]> get(@AuthenticationPrincipal OAuth2User principal, @PathVariable UUID documentId) {
        var user = users.requireByKakaoId(String.valueOf(principal.getAttributes().get("id")));
        var original = originals.get(user, documentId);
        boolean inline = Set.of("application/pdf", "image/png", "image/jpeg").contains(original.mimeType());
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .header("X-Content-Type-Options", "nosniff")
                .header(HttpHeaders.CONTENT_DISPOSITION, (inline ? ContentDisposition.inline() : ContentDisposition.attachment())
                        .filename(original.fileName(), StandardCharsets.UTF_8).build().toString())
                .contentType(inline ? MediaType.parseMediaType(original.mimeType()) : MediaType.APPLICATION_OCTET_STREAM)
                .body(original.content());
    }
}

package com.ieum.dodam.user.api.controller;

import com.ieum.dodam.care_relation.domain.CareRelationCode;
import com.ieum.dodam.global.auth.application.AuthCookieService;
import com.ieum.dodam.user.api.dto.request.WithdrawalRequest;
import com.ieum.dodam.user.api.dto.response.WithdrawalResponse;
import com.ieum.dodam.user.command.application.UserWithdrawalService;
import com.ieum.dodam.user.query.application.UserQueryService;
import com.ieum.dodam.user.api.dto.response.MeResponse;
import com.ieum.dodam.user.domain.User;
import com.ieum.dodam.user.domain.UserRole;
import com.ieum.dodam.patient.domain.Patient;
import com.ieum.dodam.patient.domain.repository.PatientRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class MeApiController {

    private final UserQueryService users;
    private final PatientRepository patients;
    private final UserWithdrawalService withdrawals;
    private final AuthCookieService authCookies;

    public MeApiController(
            UserQueryService users,
            PatientRepository patients,
            UserWithdrawalService withdrawals,
            AuthCookieService authCookies
    ) {
        this.users = users;
        this.patients = patients;
        this.withdrawals = withdrawals;
        this.authCookies = authCookies;
    }

    @GetMapping("/api/me")
    public MeResponse me(@AuthenticationPrincipal OAuth2User principal) {
        User user = users.requireByKakaoId(kakaoId(principal));
        return MeResponse.of(user, inviteCodeFor(user));
    }

    @DeleteMapping("/api/me")
    public ResponseEntity<WithdrawalResponse> withdraw(
            @AuthenticationPrincipal OAuth2User principal,
            @Valid @RequestBody WithdrawalRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse response
    ) {
        WithdrawalResponse result = withdrawals.withdraw(kakaoId(principal), request.getConfirmed());
        authCookies.clearAll(response);
        HttpSession session = httpRequest.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        SecurityContextHolder.clearContext();
        return ResponseEntity.ok(result);
    }

    private static String kakaoId(OAuth2User user) {
        Object id = user.getAttributes().get("id");
        return id == null ? "" : String.valueOf(id);
    }

    private String inviteCodeFor(User user) {
        if (user.getRole() != UserRole.PATIENT) {
            return null;
        }
        return patients.findByUserId(user.getId())
                .map(Patient::getInviteCode)
                .map(CareRelationCode::display)
                .orElse(null);
    }
}

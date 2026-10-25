package com.ieum.dodam.user.api.controller;

import com.ieum.dodam.care_relation.domain.CareRelationCode;
import com.ieum.dodam.global.auth.application.AuthCookieService;
import com.ieum.dodam.global.auth.security.JwtAuthFilter;
import com.ieum.dodam.user.api.dto.request.RoleRequest;
import com.ieum.dodam.user.api.dto.response.LinkedAccountsResponse;
import com.ieum.dodam.user.api.dto.response.MeResponse;
import com.ieum.dodam.user.command.application.AccountSwitchService;
import com.ieum.dodam.user.command.application.UserAccountService;
import com.ieum.dodam.user.domain.User;
import com.ieum.dodam.user.query.application.UserQueryService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/** 개인 계정과 보호자 계정 사이의 전환 API. */
@RestController
public class AccountSwitchApiController {

    private final AccountSwitchService switches;
    private final UserAccountService accounts;
    private final UserQueryService users;
    private final AuthCookieService authCookies;

    public AccountSwitchApiController(
            AccountSwitchService switches,
            UserAccountService accounts,
            UserQueryService users,
            AuthCookieService authCookies
    ) {
        this.switches = switches;
        this.accounts = accounts;
        this.users = users;
        this.authCookies = authCookies;
    }

    @GetMapping("/api/accounts")
    public LinkedAccountsResponse linkedAccounts(@AuthenticationPrincipal OAuth2User principal) {
        User me = users.requireByKakaoId(kakaoId(principal));
        return LinkedAccountsResponse.of(me, switches.linkedAccounts(me));
    }

    /** 대상 역할의 계정으로 로그인 상태를 바꾼다. 계정이 없으면 새로 만든 뒤 전환한다. */
    @PostMapping("/api/accounts/switch")
    public MeResponse switchAccount(
            @AuthenticationPrincipal OAuth2User principal,
            @RequestBody RoleRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse response
    ) {
        User target = switches.switchTo(kakaoId(principal), request.toUserRole());
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(JwtAuthFilter.principal(target));
        SecurityContextHolder.setContext(context);
        new HttpSessionSecurityContextRepository().saveContext(context, httpRequest, response);
        authCookies.write(response, target);
        String inviteCode = accounts.patientInviteCode(target);
        return MeResponse.of(target, inviteCode == null ? null : CareRelationCode.display(inviteCode));
    }

    private static String kakaoId(OAuth2User user) {
        Object id = user.getAttributes().get("id");
        return id == null ? "" : String.valueOf(id);
    }
}

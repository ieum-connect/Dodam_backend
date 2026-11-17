package com.ieum.dodam.user.api.controller;

import com.ieum.dodam.care_relation.domain.CareRelationCode;
import com.ieum.dodam.global.auth.application.AuthCookieService;
import com.ieum.dodam.global.auth.security.JwtAuthFilter;
import com.ieum.dodam.global.exception.BusinessException;
import com.ieum.dodam.global.exception.GlobalErrorCode;
import com.ieum.dodam.user.api.dto.request.DemoLoginRequest;
import com.ieum.dodam.user.api.dto.response.AuthConfigResponse;
import com.ieum.dodam.user.api.dto.response.MeResponse;
import com.ieum.dodam.user.command.application.UserAccountService;
import com.ieum.dodam.user.domain.User;
import com.ieum.dodam.user.domain.UserRole;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
public class AuthApiController {

    private final UserAccountService accounts;
    private final AuthCookieService authCookies;
    private final String kakaoClientId;
    private final boolean demoLogin;
    private final boolean demoShowcaseOnly;

    public AuthApiController(
            UserAccountService accounts,
            AuthCookieService authCookies,
            @Value("${spring.security.oauth2.client.registration.kakao.client-id:unset}") String kakaoClientId,
            @Value("${dodam.demo-login:false}") boolean demoLogin,
            @Value("${dodam.demo-showcase-only:false}") boolean demoShowcaseOnly
    ) {
        this.accounts = accounts;
        this.authCookies = authCookies;
        this.kakaoClientId = kakaoClientId;
        this.demoLogin = demoLogin;
        this.demoShowcaseOnly = demoShowcaseOnly;
    }

    @GetMapping("/api/auth/config")
    public AuthConfigResponse config() {
        return new AuthConfigResponse(kakaoReady(), demoLogin);
    }

    public record ProfileRequest(String name) {}

    @PutMapping("/api/me/profile")
    public MeResponse updateProfile(@AuthenticationPrincipal OAuth2User principal,
                                    @RequestBody ProfileRequest request) {
        User updated = accounts.updateName(kakaoId(principal), request.name());
        refreshSession(principal, updated);
        return toMe(updated);
    }

    @PostMapping("/api/demo/login")
    public ResponseEntity<MeResponse> demoLogin(
            @RequestBody DemoLoginRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse response
    ) {
        if (!demoLogin) {
            throw new BusinessException(GlobalErrorCode.DEMO_LOGIN_DISABLED);
        }
        if (demoShowcaseOnly && !"MINJI".equals(request.getAccount()) && !"JIHUN".equals(request.getAccount())) {
            throw new BusinessException(GlobalErrorCode.INVALID_REQUEST);
        }
        User user = request.getAccount() == null
                ? accounts.ensureDemoUser(request.toUserRole())
                : accounts.ensureNamedDemoUser(request.getAccount());
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(JwtAuthFilter.principal(user));
        SecurityContextHolder.setContext(context);
        new HttpSessionSecurityContextRepository().saveContext(context, httpRequest, response);
        authCookies.write(response, user);
        return ResponseEntity.ok(toMe(user));
    }

    private MeResponse toMe(User user) {
        String inviteCode = accounts.patientInviteCode(user);
        return MeResponse.of(user, inviteCode == null ? null : CareRelationCode.display(inviteCode));
    }

    private void refreshSession(OAuth2User current, User updated) {
        Map<String, Object> attributes = new HashMap<>(current.getAttributes());
        attributes.put("role", updated.getRole().name());
        attributes.put("displayName", updated.getName());
        DefaultOAuth2User principal = new DefaultOAuth2User(
                List.of(new SimpleGrantedAuthority("ROLE_" + updated.getRole().name())),
                attributes,
                "id"
        );
        var auth = SecurityContextHolder.getContext().getAuthentication();
        String registrationId = auth instanceof OAuth2AuthenticationToken oauth
                ? oauth.getAuthorizedClientRegistrationId()
                : "kakao";
        SecurityContextHolder.getContext().setAuthentication(
                new OAuth2AuthenticationToken(principal, principal.getAuthorities(), registrationId)
        );
    }

    private static String kakaoId(OAuth2User user) {
        Object id = user.getAttributes().get("id");
        return id == null ? "" : String.valueOf(id);
    }

    private boolean kakaoReady() {
        return kakaoClientId != null
                && !kakaoClientId.isBlank()
                && !"unset".equalsIgnoreCase(kakaoClientId);
    }
}

package com.ieum.dodam.global.auth.oauth;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;

public final class KakaoAuthorizationRequests implements OAuth2AuthorizationRequestResolver {

    private final DefaultOAuth2AuthorizationRequestResolver defaults;

    public KakaoAuthorizationRequests(ClientRegistrationRepository registrations) {
        defaults = new DefaultOAuth2AuthorizationRequestResolver(registrations, "/oauth2/authorization");
    }

    @Override
    public OAuth2AuthorizationRequest resolve(HttpServletRequest request) {
        return withAccountChoice(request, defaults.resolve(request));
    }

    @Override
    public OAuth2AuthorizationRequest resolve(HttpServletRequest request, String registrationId) {
        return withAccountChoice(request, defaults.resolve(request, registrationId));
    }

    private OAuth2AuthorizationRequest withAccountChoice(
            HttpServletRequest request, OAuth2AuthorizationRequest authorization
    ) {
        if (authorization == null || !"kakao".equals(authorization.getAttribute(OAuth2ParameterNames.REGISTRATION_ID))
                || !"1".equals(request.getParameter("chooseAccount"))) {
            return authorization;
        }

        // select_account on its own can reuse the current Kakao session.
        return OAuth2AuthorizationRequest.from(authorization)
                .additionalParameters(parameters -> parameters.put("prompt", "login,select_account"))
                .authorizationRequestUri((String) null)
                .build();
    }
}

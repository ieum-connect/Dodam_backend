package com.ieum.dodam.global.config;

import com.ieum.dodam.global.auth.oauth.KakaoAuthorizationRequests;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.web.util.UriUtils;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class SecurityConfigTest {

    @Test
    void kakaoAccountSelectionRequiresExplicitOptIn() {
        OAuth2AuthorizationRequest standard = resolveKakao(null);
        OAuth2AuthorizationRequest choice = resolveKakao("1");

        assertThat(standard.getAuthorizationRequestUri()).doesNotContain("prompt=");
        assertThat(resolveKakao("0").getAuthorizationRequestUri()).doesNotContain("prompt=");
        assertThat(choice.getAdditionalParameters()).containsEntry("prompt", "login,select_account");
        var query = UriComponentsBuilder.fromUriString(choice.getAuthorizationRequestUri()).build().getQueryParams();
        assertThat(UriUtils.decode(query.getFirst("prompt"), StandardCharsets.UTF_8)).isEqualTo("login,select_account");
        assertThat(UriUtils.decode(query.getFirst("state"), StandardCharsets.UTF_8)).isEqualTo(choice.getState());
        assertThat(choice.getState()).isNotBlank().isNotEqualTo(standard.getState());
        assertThat(choice.getRedirectUri()).isEqualTo(standard.getRedirectUri());
    }

    private OAuth2AuthorizationRequest resolveKakao(String chooseAccount) {
        ClientRegistration kakao = ClientRegistration.withRegistrationId("kakao")
                .clientId("test-client")
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .redirectUri("{baseUrl}/login/oauth2/code/{registrationId}")
                .authorizationUri("https://kauth.kakao.com/oauth/authorize")
                .tokenUri("https://kauth.kakao.com/oauth/token")
                .build();
        String path = "/oauth2/authorization/kakao";
        var request = new MockHttpServletRequest("GET", path);
        request.setServletPath(path);
        if (chooseAccount != null) request.setParameter("chooseAccount", chooseAccount);
        return new KakaoAuthorizationRequests(new InMemoryClientRegistrationRepository(kakao)).resolve(request);
    }

    @Test
    void cancelMapsToCanceled() {
        OAuth2AuthenticationException denied = new OAuth2AuthenticationException(
                new OAuth2Error("access_denied")
        );
        assertThat(SecurityConfig.loginError(denied)).isEqualTo("canceled");
    }

    @Test
    void otherFailureMapsToFail() {
        OAuth2AuthenticationException other = new OAuth2AuthenticationException(
                new OAuth2Error("server_error")
        );
        assertThat(SecurityConfig.loginError(other)).isEqualTo("fail");
        assertThat(SecurityConfig.loginError(new IllegalStateException("broken"))).isEqualTo("fail");
    }
}

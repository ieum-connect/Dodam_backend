package com.ieum.dodam.global.auth.application;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;

class AuthCookieServiceTest {

    @Test
    void productionCookieDeletionPreservesSecureAndHttpOnlyFlags() {
        AuthCookieService cookies = new AuthCookieService(null,"DODAM_TOKEN",30,"DODAM_SESSION");
        org.springframework.test.util.ReflectionTestUtils.setField(cookies,"secure",true);
        MockHttpServletResponse response=new MockHttpServletResponse();cookies.clearAll(response);
        assertThat(response.getHeaders(HttpHeaders.SET_COOKIE)).hasSize(2)
            .allSatisfy(header -> assertThat(header).contains("Secure","HttpOnly","SameSite=Lax","Max-Age=0"));
    }

    @Test
    void clearAllExpiresJwtAndSessionCookies() {
        AuthCookieService cookies = new AuthCookieService(null, "DODAM_TOKEN", 30, "DODAM_SESSION");
        MockHttpServletResponse response = new MockHttpServletResponse();

        cookies.clearAll(response);

        assertThat(response.getHeaders(HttpHeaders.SET_COOKIE))
                .anySatisfy(header -> assertThat(header).startsWith("DODAM_TOKEN=;").contains("Max-Age=0", "Path=/"))
                .anySatisfy(header -> assertThat(header).startsWith("DODAM_SESSION=;").contains("Max-Age=0", "Path=/"));
    }
}

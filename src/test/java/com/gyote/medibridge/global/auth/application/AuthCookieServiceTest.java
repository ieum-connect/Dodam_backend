package com.gyote.medibridge.global.auth.application;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;

class AuthCookieServiceTest {

    @Test
    void clearAllExpiresJwtAndSessionCookies() {
        AuthCookieService cookies = new AuthCookieService(null, "MEDIBRIDGE_TOKEN", 30, "MEDIBRIDGE_SESSION");
        MockHttpServletResponse response = new MockHttpServletResponse();

        cookies.clearAll(response);

        assertThat(response.getHeaders(HttpHeaders.SET_COOKIE))
                .anySatisfy(header -> assertThat(header).startsWith("MEDIBRIDGE_TOKEN=;").contains("Max-Age=0", "Path=/"))
                .anySatisfy(header -> assertThat(header).startsWith("MEDIBRIDGE_SESSION=;").contains("Max-Age=0", "Path=/"));
    }
}

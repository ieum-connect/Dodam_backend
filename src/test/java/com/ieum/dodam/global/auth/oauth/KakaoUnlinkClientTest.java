package com.ieum.dodam.global.auth.oauth;

import com.ieum.dodam.global.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class KakaoUnlinkClientTest {
    private final RestClient.Builder builder = RestClient.builder().baseUrl("https://kapi.kakao.com");
    private final MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
    private final KakaoUnlinkClient client = new KakaoUnlinkClient(builder.build(), "test-key");

    @Test
    void unlinksTheRequestedAccount() {
        server.expect(requestTo("https://kapi.kakao.com/v1/user/unlink"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "KakaoAK test-key"))
                .andExpect(content().string("target_id_type=user_id&target_id=123456789"))
                .andRespond(withSuccess("{\"id\":123456789}", MediaType.APPLICATION_JSON));

        client.unlink("123456789");
        server.verify();
    }

    @Test
    void alreadyDisconnectedIsSafeToRetry() {
        server.expect(requestTo("https://kapi.kakao.com/v1/user/unlink"))
                .andRespond(withStatus(HttpStatus.BAD_REQUEST).contentType(MediaType.APPLICATION_JSON)
                        .body("{\"code\":-101}"));

        client.unlink("123456789");
        server.verify();
    }

    @Test
    void unexpectedAccountDoesNotCountAsSuccess() {
        server.expect(requestTo("https://kapi.kakao.com/v1/user/unlink"))
                .andRespond(withSuccess("{\"id\":987654321}", MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> client.unlink("123456789")).isInstanceOf(BusinessException.class);
        server.verify();
    }
}

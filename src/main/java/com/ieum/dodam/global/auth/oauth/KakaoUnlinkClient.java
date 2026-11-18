package com.ieum.dodam.global.auth.oauth;

import com.ieum.dodam.global.exception.BusinessException;
import com.ieum.dodam.user.error.UserErrorCode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.time.Duration;

@Component
public class KakaoUnlinkClient {
    private final RestClient client;
    private final String adminKey;

    @Autowired
    public KakaoUnlinkClient(RestClient.Builder builder, @Value("${KAKAO_ADMIN_KEY:}") String adminKey) {
        this(kakaoClient(builder), adminKey);
    }

    KakaoUnlinkClient(RestClient client, String adminKey) {
        this.client = client;
        this.adminKey = adminKey.trim();
    }

    private static RestClient kakaoClient(RestClient.Builder builder) {
        var requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(3));
        requestFactory.setReadTimeout(Duration.ofSeconds(5));
        return builder.clone()
                .baseUrl("https://kapi.kakao.com")
                .requestFactory(requestFactory)
                .build();
    }

    public void unlink(String kakaoId) {
        if (adminKey.isBlank() || kakaoId == null || !kakaoId.matches("[1-9][0-9]{0,18}")) {
            throw new BusinessException(UserErrorCode.KAKAO_UNLINK_FAILED);
        }
        var form = new LinkedMultiValueMap<String, String>();
        form.add("target_id_type", "user_id");
        form.add("target_id", kakaoId);
        try {
            UnlinkResponse response = client.post().uri("/v1/user/unlink")
                    .header(HttpHeaders.AUTHORIZATION, "KakaoAK " + adminKey)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .body(UnlinkResponse.class);
            if (response != null && kakaoId.equals(response.id())) {
                return;
            }
        } catch (RestClientResponseException exception) {
            if (alreadyUnlinked(exception)) {
                return;
            }
        } catch (RestClientException ignored) {
            // The caller rolls back the withdrawal and can retry.
        }
        throw new BusinessException(UserErrorCode.KAKAO_UNLINK_FAILED);
    }

    private boolean alreadyUnlinked(RestClientResponseException exception) {
        if (exception.getStatusCode().value() != 400) {
            return false;
        }
        try {
            KakaoError error = exception.getResponseBodyAs(KakaoError.class);
            return error != null && error.code() == -101;
        } catch (IllegalStateException | RestClientException ignored) {
            return false;
        }
    }

    private record UnlinkResponse(String id) {}
    private record KakaoError(int code) {}
}

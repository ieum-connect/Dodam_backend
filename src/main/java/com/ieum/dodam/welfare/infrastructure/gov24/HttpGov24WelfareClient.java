package com.ieum.dodam.welfare.infrastructure.gov24;

import com.ieum.dodam.welfare.infrastructure.gov24.dto.Gov24PageResponse;
import com.ieum.dodam.welfare.infrastructure.gov24.dto.Gov24ServiceDetailItem;
import com.ieum.dodam.welfare.infrastructure.gov24.dto.Gov24ServiceListItem;
import com.ieum.dodam.welfare.infrastructure.gov24.dto.Gov24SupportConditionsItem;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.util.Objects;

public class HttpGov24WelfareClient implements Gov24WelfareClient {
    private static final ParameterizedTypeReference<Gov24PageResponse<Gov24ServiceListItem>> LIST_TYPE = new ParameterizedTypeReference<>() {};
    private static final ParameterizedTypeReference<Gov24PageResponse<Gov24ServiceDetailItem>> DETAIL_TYPE = new ParameterizedTypeReference<>() {};
    private static final ParameterizedTypeReference<Gov24PageResponse<Gov24SupportConditionsItem>> CONDITIONS_TYPE = new ParameterizedTypeReference<>() {};

    private final RestClient restClient;
    private final String serviceKey;

    public HttpGov24WelfareClient(RestClient.Builder builder, Gov24WelfareProperties properties) {
        this(builder, properties.getBaseUrl().toString(), properties.getServiceKey());
    }

    HttpGov24WelfareClient(RestClient.Builder builder, String baseUrl, String serviceKey) {
        this.restClient = builder.baseUrl(baseUrl.replaceAll("/+$", "")).build();
        this.serviceKey = Objects.requireNonNull(serviceKey);
    }

    @Override
    public Gov24PageResponse<Gov24ServiceListItem> serviceList(int page, int perPage, String modifiedFrom) {
        return get("/gov24/v3/serviceList", LIST_TYPE, page, perPage, modifiedFrom, null);
    }

    @Override
    public Gov24PageResponse<Gov24ServiceDetailItem> serviceDetail(String serviceId) {
        return get("/gov24/v3/serviceDetail", DETAIL_TYPE, null, null, null, serviceId);
    }

    @Override
    public Gov24PageResponse<Gov24SupportConditionsItem> supportConditions(int page, int perPage, String serviceId) {
        return get("/gov24/v3/supportConditions", CONDITIONS_TYPE, page, perPage, null, serviceId);
    }

    private <T> T get(String path, ParameterizedTypeReference<T> type, Integer page, Integer perPage,
                      String modifiedFrom, String serviceId) {
        try {
            return restClient.get().uri(uri -> {
                var builder = uri.path(path).queryParam("serviceKey", serviceKey).queryParam("returnType", "JSON");
                if (page != null) builder.queryParam("page", page);
                if (perPage != null) builder.queryParam("perPage", perPage);
                if (modifiedFrom != null && !modifiedFrom.isBlank()) builder.queryParam("수정일시::GTE", modifiedFrom);
                if (serviceId != null && !serviceId.isBlank()) builder.queryParam("서비스ID", serviceId);
                return builder.build();
            }).retrieve().body(type);
        } catch (RestClientResponseException e) {
            throw new Gov24WelfareClientException(classify(e.getStatusCode()), e.getStatusCode().value(),
                    "정부24 복지 API 요청이 실패했습니다.", e);
        } catch (RestClientException e) {
            throw new Gov24WelfareClientException(Gov24WelfareClientException.Reason.UNAVAILABLE, null,
                    "정부24 복지 API에 연결할 수 없습니다.", e);
        } catch (RuntimeException e) {
            throw new Gov24WelfareClientException(Gov24WelfareClientException.Reason.INVALID_RESPONSE, null,
                    "정부24 복지 API 응답을 해석할 수 없습니다.", e);
        }
    }

    private static Gov24WelfareClientException.Reason classify(HttpStatusCode status) {
        if (status.value() == 401 || status.value() == 403) return Gov24WelfareClientException.Reason.AUTHENTICATION;
        if (status.value() == 429) return Gov24WelfareClientException.Reason.RATE_LIMITED;
        if (status.is5xxServerError() || status.value() == 408) return Gov24WelfareClientException.Reason.UNAVAILABLE;
        return Gov24WelfareClientException.Reason.REQUEST_REJECTED;
    }
}

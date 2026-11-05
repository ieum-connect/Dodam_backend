package com.ieum.dodam.welfare.infrastructure.gov24;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.queryParam;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withBadRequest;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.http.HttpMethod.GET;

class HttpGov24WelfareClientTest {
    @Test
    void requestsServiceListWithPagingAndKey() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        HttpGov24WelfareClient client = new HttpGov24WelfareClient(builder, "https://example.test/api", "secret");
        server.expect(requestTo(org.hamcrest.Matchers.startsWith("https://example.test/api/gov24/v3/serviceList")))
                .andExpect(queryParam("serviceKey", "secret"))
                .andExpect(queryParam("page", "1"))
                .andExpect(queryParam("perPage", "20"))
                .andRespond(withSuccess("{\"page\":1,\"perPage\":20,\"totalCount\":1,\"currentCount\":1,\"data\":[{\"서비스ID\":\"SVC-1\",\"서비스명\":\"영유아 지원\"}]}", MediaType.APPLICATION_JSON));

        var response = client.serviceList(1, 20, "2026-11-01");

        assertThat(response.data()).singleElement().satisfies(item -> assertThat(item.serviceId()).isEqualTo("SVC-1"));
        server.verify();
    }

    @Test
    void classifiesBadRequestAsRequestRejected() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        HttpGov24WelfareClient client = new HttpGov24WelfareClient(builder, "https://example.test/api", "secret");
        server.expect(requestTo(org.hamcrest.Matchers.startsWith("https://example.test/api/gov24/v3/serviceDetail")))
                .andRespond(withBadRequest());

        assertThatThrownBy(() -> client.serviceDetail("SVC-1"))
                .isInstanceOf(Gov24WelfareClientException.class)
                .extracting("reason").isEqualTo(Gov24WelfareClientException.Reason.REQUEST_REJECTED);
        server.verify();
    }
}

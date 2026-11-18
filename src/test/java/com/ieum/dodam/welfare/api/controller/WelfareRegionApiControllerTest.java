package com.ieum.dodam.welfare.api.controller;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.queryParam;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class WelfareRegionApiControllerTest {

    @Test
    void includesSejongAndUsesCurrentProvinceNames() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        WelfareRegionApiController controller = new WelfareRegionApiController(builder);
        server.expect(queryParam("regcode_pattern", "*00000000"))
                .andRespond(withSuccess("""
                        {"regcodes":[
                          {"code":"1100000000","name":"서울특별시"},
                          {"code":"4200000000","name":"강원도"},
                          {"code":"4500000000","name":"전라북도"}
                        ]}
                        """, MediaType.APPLICATION_JSON));

        WelfareRegionApiController.RegionResponse response =
                controller.regions(WelfareRegionApiController.Level.PROVINCE, null);

        assertThat(response.items())
                .extracting(WelfareRegionApiController.RegionOption::name)
                .containsExactly("서울특별시", "세종특별자치시", "강원특별자치도", "전북특별자치도");
        server.verify();
    }

    @Test
    void normalizesProvincePrefixInChildRegionNames() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        WelfareRegionApiController controller = new WelfareRegionApiController(builder);
        server.expect(queryParam("regcode_pattern", "42*00000"))
                .andRespond(withSuccess("""
                        {"regcodes":[{"code":"4211000000","name":"강원도 춘천시"}]}
                        """, MediaType.APPLICATION_JSON));

        WelfareRegionApiController.RegionResponse response = controller.regions(
                WelfareRegionApiController.Level.DISTRICT, "4200000000");

        assertThat(response.items()).singleElement()
                .extracting(WelfareRegionApiController.RegionOption::name)
                .isEqualTo("강원특별자치도 춘천시");
        server.verify();
    }
}

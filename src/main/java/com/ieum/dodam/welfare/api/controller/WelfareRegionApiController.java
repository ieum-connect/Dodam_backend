package com.ieum.dodam.welfare.api.controller;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/welfare-regions")
public class WelfareRegionApiController {
    private final RestClient client;

    public WelfareRegionApiController(RestClient.Builder builder) {
        this.client = builder.baseUrl("https://api.juso.dev/v1").build();
    }

    @GetMapping
    public RegionResponse regions(@RequestParam(defaultValue = "PROVINCE") Level level,
                                  @RequestParam(required = false) String parentCode) {
        String pattern = switch (level) {
            case PROVINCE -> "*00000000";
            case DISTRICT -> prefix(parentCode, 2) + "*00000";
            case NEIGHBORHOOD -> prefix(parentCode, 5) + "*00";
        };
        JsonNode body = client.get().uri(uri -> uri.path("/regcodes")
                .queryParam("regcode_pattern", pattern)
                .queryParam("is_ignore_zero", level != Level.PROVINCE)
                .build()).retrieve().body(JsonNode.class);
        List<RegionOption> items = new ArrayList<>();
        if (body != null && body.path("regcodes").isArray()) {
            body.path("regcodes").forEach(item -> items.add(new RegionOption(
                    item.path("code").asText(), item.path("name").asText())));
        }
        return new RegionResponse(level, items);
    }

    private String prefix(String code, int length) {
        if (code == null || !code.matches("\\d{10}")) throw new IllegalArgumentException("올바른 상위 행정구역 코드가 필요합니다.");
        return code.substring(0, length);
    }

    public enum Level { PROVINCE, DISTRICT, NEIGHBORHOOD }
    public record RegionOption(String code, String name) {}
    public record RegionResponse(Level level, List<RegionOption> items) {}
}

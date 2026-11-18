package com.ieum.dodam.welfare.api.controller;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@RestController
@RequestMapping("/api/welfare-regions")
public class WelfareRegionApiController {
    private static final RegionOption SEJONG = new RegionOption("3611000000", "세종특별자치시");
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
                    item.path("code").asText(), currentName(item.path("name").asText()))));
        }
        // 세종은 법정동 코드에 별도의 시·도 대표 코드(3600000000)가 없어 일반 패턴에 잡히지 않는다.
        if (level == Level.PROVINCE && items.stream().noneMatch(item -> item.code().startsWith("36"))) {
            items.add(SEJONG);
        }
        items.sort(Comparator.comparing(RegionOption::code));
        return new RegionResponse(level, items);
    }

    private static String currentName(String name) {
        if (name.startsWith("강원도")) return "강원특별자치도" + name.substring("강원도".length());
        if (name.startsWith("전라북도")) return "전북특별자치도" + name.substring("전라북도".length());
        return name;
    }

    private String prefix(String code, int length) {
        if (code == null || !code.matches("\\d{10}")) throw new IllegalArgumentException("올바른 상위 행정구역 코드가 필요합니다.");
        return code.substring(0, length);
    }

    public enum Level { PROVINCE, DISTRICT, NEIGHBORHOOD }
    public record RegionOption(String code, String name) {}
    public record RegionResponse(Level level, List<RegionOption> items) {}
}

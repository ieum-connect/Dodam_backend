package com.ieum.dodam.welfare.infrastructure.gov24.dto;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.LinkedHashMap;
import java.util.Map;

/** 의미가 검증되지 않은 JA 코드를 손실 없이 보관하는 지원조건 응답 항목. */
@JsonIgnoreProperties(ignoreUnknown = false)
public class Gov24SupportConditionsItem {
    @JsonProperty("서비스ID")
    private String serviceId;
    @JsonProperty("서비스명")
    private String serviceName;
    private final Map<String, JsonNode> conditionCodes = new LinkedHashMap<>();

    public String getServiceId() { return serviceId; }
    public String getServiceName() { return serviceName; }
    public Map<String, JsonNode> getConditionCodes() { return Map.copyOf(conditionCodes); }

    @JsonAnySetter
    void captureCondition(String code, JsonNode value) {
        if (code != null && code.startsWith("JA")) conditionCodes.put(code, value);
    }
}

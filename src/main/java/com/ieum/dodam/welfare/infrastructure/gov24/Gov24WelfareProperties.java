package com.ieum.dodam.welfare.infrastructure.gov24;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.net.URI;
import java.time.Duration;

/** 정부24 공공서비스 API 연결 설정. 인증키는 문자열 표현과 로그에서 항상 숨긴다. */
@ConfigurationProperties(prefix = "dodam.welfare.gov24")
public class Gov24WelfareProperties {
    private URI baseUrl = URI.create("https://api.odcloud.kr/api");
    private String serviceKey = "";
    private int pageSize = 100;
    private Duration connectTimeout = Duration.ofSeconds(2);
    private Duration readTimeout = Duration.ofSeconds(10);
    private boolean syncEnabled;

    public URI getBaseUrl() { return baseUrl; }
    public void setBaseUrl(URI baseUrl) { this.baseUrl = baseUrl; }
    public String getServiceKey() { return serviceKey; }
    public void setServiceKey(String serviceKey) { this.serviceKey = serviceKey; }
    public int getPageSize() { return pageSize; }
    public void setPageSize(int pageSize) {
        if (pageSize < 1 || pageSize > 1000) throw new IllegalArgumentException("pageSize는 1~1000이어야 합니다.");
        this.pageSize = pageSize;
    }
    public Duration getConnectTimeout() { return connectTimeout; }
    public void setConnectTimeout(Duration connectTimeout) { this.connectTimeout = connectTimeout; }
    public Duration getReadTimeout() { return readTimeout; }
    public void setReadTimeout(Duration readTimeout) { this.readTimeout = readTimeout; }
    public boolean isSyncEnabled() { return syncEnabled; }
    public void setSyncEnabled(boolean syncEnabled) { this.syncEnabled = syncEnabled; }

    @Override
    public String toString() {
        return "Gov24WelfareProperties[baseUrl=" + baseUrl + ", serviceKey=***, pageSize=" + pageSize
                + ", connectTimeout=" + connectTimeout + ", readTimeout=" + readTimeout
                + ", syncEnabled=" + syncEnabled + "]";
    }
}

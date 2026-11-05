package com.ieum.dodam.welfare.infrastructure.gov24.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/** ODcloud API의 공통 페이지 응답. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record Gov24PageResponse<T>(
        int page,
        int perPage,
        long totalCount,
        int currentCount,
        long matchCount,
        List<T> data
) {
    public Gov24PageResponse {
        data = data == null ? List.of() : List.copyOf(data);
    }
}

package com.ieum.dodam.vaccination.error;

import com.ieum.dodam.global.exception.ErrorCode;
import org.springframework.http.HttpStatus;

/**
 * 예방접종 API 오류 코드입니다. AI 서버의 오류 코드(VACCINATION_001/002)와 문자열이 같아도 뜻이 다릅니다.
 * AI 서버 코드는 HTTP 어댑터 안에서만 해석하고 프론트에는 이 코드만 내려갑니다.
 */
public enum VaccinationErrorCode implements ErrorCode {
    BIRTH_DATE_REQUIRED(HttpStatus.BAD_REQUEST, "VACCINATION_001", "아이의 생년월일을 먼저 등록해 주세요."),
    BIRTH_DATE_IN_FUTURE(HttpStatus.BAD_REQUEST, "VACCINATION_002", "생년월일이 오늘보다 늦습니다."),
    INVALID_DOSE_ID(HttpStatus.BAD_REQUEST, "VACCINATION_003", "접종 차수 형식이 올바르지 않습니다."),
    UNKNOWN_DOSE(HttpStatus.BAD_REQUEST, "VACCINATION_004", "접종 일정에 없는 차수입니다."),
    AI_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "VACCINATION_005", "예방접종 일정을 지금 불러올 수 없습니다. 잠시 후 다시 시도해 주세요.");

    private final HttpStatus status;
    private final String code;
    private final String message;

    VaccinationErrorCode(HttpStatus status, String code, String message) {
        this.status = status;
        this.code = code;
        this.message = message;
    }

    @Override
    public HttpStatus status() {
        return status;
    }

    @Override
    public String code() {
        return code;
    }

    @Override
    public String message() {
        return message;
    }
}

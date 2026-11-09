package com.ieum.dodam.growth.error;

import com.ieum.dodam.global.exception.ErrorCode;
import org.springframework.http.HttpStatus;

public enum GrowthErrorCode implements ErrorCode {
    RECORD_NOT_FOUND;
    public HttpStatus status() { return HttpStatus.NOT_FOUND; }
    public String code() { return "GROWTH_001"; }
    public String message() { return "성장 기록을 찾을 수 없어요."; }
}

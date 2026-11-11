package com.ieum.dodam.notification.push;

import com.ieum.dodam.global.exception.ErrorCode;
import org.springframework.http.HttpStatus;

public enum PushErrorCode implements ErrorCode {
    PUSH_DISABLED;

    @Override public HttpStatus status() { return HttpStatus.SERVICE_UNAVAILABLE; }
    @Override public String code() { return "PUSH_DISABLED"; }
    @Override public String message() { return "푸시 알림을 준비하고 있어요. 잠시 후 다시 시도해주세요."; }
}

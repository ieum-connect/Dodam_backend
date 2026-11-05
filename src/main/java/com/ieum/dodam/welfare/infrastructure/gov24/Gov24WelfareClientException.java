package com.ieum.dodam.welfare.infrastructure.gov24;

public class Gov24WelfareClientException extends RuntimeException {
    public enum Reason { AUTHENTICATION, RATE_LIMITED, UNAVAILABLE, INVALID_RESPONSE, REQUEST_REJECTED }

    private final Reason reason;
    private final Integer statusCode;

    public Gov24WelfareClientException(Reason reason, Integer statusCode, String message, Throwable cause) {
        super(message, cause);
        this.reason = reason;
        this.statusCode = statusCode;
    }

    public Reason getReason() { return reason; }
    public Integer getStatusCode() { return statusCode; }
}

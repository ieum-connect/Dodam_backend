package com.ieum.dodam.schedule.domain;

import com.ieum.dodam.global.exception.ErrorCode;
import org.springframework.http.HttpStatus;

public enum ScheduleErrorCode implements ErrorCode {
    START_REQUIRED("복용 시작 날짜와 시간을 입력해 주세요."),
    DATE_RANGE("날짜는 1~9998년 범위로 입력해 주세요."),
    END_REQUIRED("반복 복약을 승인하려면 처방받은 마지막 날짜를 입력해 주세요."),
    END_BEFORE_START("반복 종료일은 복용 시작일과 같거나 이후여야 해요."),
    WEEKDAY_REQUIRED("복용 요일을 한 개 이상 선택해 주세요."),
    EMPTY_PERIOD("선택한 기간에 복용 요일이 없어요. 요일이나 종료일을 바꿔 주세요."),
    TIME_ZONE("기기의 시간대 설정을 확인한 뒤 다시 시도해 주세요."),
    NOTES_TOO_LONG("복용 방법 메모는 500자까지 입력할 수 있어요.");

    private final String message;
    ScheduleErrorCode(String message) { this.message = message; }
    public HttpStatus status() { return HttpStatus.BAD_REQUEST; }
    public String code() { return "SCHEDULE_" + name(); }
    public String message() { return message; }
}

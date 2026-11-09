package com.ieum.dodam.vaccination.domain;

import com.fasterxml.jackson.databind.JsonNode;

import java.time.LocalDate;
import java.util.List;

/** AI 서버에 예방접종 일정 계산을 요청하는 포트입니다. */
public interface VaccinationSchedulePort {

    /**
     * 일정 계산 결과를 해석하지 않은 JSON 객체로 반환합니다.
     * 모르는 차수가 섞이면 {@code BusinessException(UNKNOWN_DOSE)}, 그 밖의 실패는 {@code BusinessException(AI_UNAVAILABLE)}을 던집니다.
     */
    JsonNode fetchSchedule(LocalDate birthDate, LocalDate asOf, List<String> completedDoses);
}

package com.ieum.dodam.health_record.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.UUID;
import java.time.Instant;

/** 대상 개인과 작성자는 서버에서 권한을 검증한다. */
public record HealthRecordWriteRequestDto(
        UUID patientId,
        @NotBlank @Size(max = 2000) String body,
        UUID visitId,
        Instant recordedAt) {}

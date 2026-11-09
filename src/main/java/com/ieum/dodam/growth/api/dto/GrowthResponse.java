package com.ieum.dodam.growth.api.dto;

import com.ieum.dodam.growth.domain.GrowthRecord;

import java.math.BigDecimal;
import java.time.*;
import java.util.UUID;

public record GrowthResponse(
        UUID id,
        UUID patientId,
        LocalDate measuredOn,
        BigDecimal heightCm,
        BigDecimal weightKg,
        String note,
        long version) {
    public static GrowthResponse of(GrowthRecord record) {
        return new GrowthResponse(
                record.getId(),
                record.getPatientId(),
                record.getMeasuredOn(),
                record.getHeightCm(),
                record.getWeightKg(),
                record.getNote(),
                record.getVersion());
    }
}

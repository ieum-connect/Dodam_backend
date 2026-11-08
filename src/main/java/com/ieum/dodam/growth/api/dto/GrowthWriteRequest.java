package com.ieum.dodam.growth.api.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record GrowthWriteRequest(
        @NotNull UUID patientId,
        @NotNull LocalDate measuredOn,
        @DecimalMin("0.1") @DecimalMax("300.0") @Digits(integer = 3, fraction = 1)
                BigDecimal heightCm,
        @DecimalMin("0.1") @DecimalMax("500.0") @Digits(integer = 3, fraction = 1)
                BigDecimal weightKg,
        @Size(max = 500) String note,
        @PositiveOrZero Long version) {}

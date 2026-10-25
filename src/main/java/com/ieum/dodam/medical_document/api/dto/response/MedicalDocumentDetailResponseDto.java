package com.ieum.dodam.medical_document.api.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.ieum.dodam.global.status.AiJobStatus;
import com.ieum.dodam.global.status.DocumentStatus;
import com.ieum.dodam.global.status.ResultStatus;
import com.ieum.dodam.global.type.DocumentType;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

public record MedicalDocumentDetailResponseDto(
        UUID documentId,
        UUID visitId,
        UUID patientId,
        String documentName,
        DocumentType documentType,
        LocalDate visitedOn,
        Author author,
        DocumentStatus documentStatus,
        AiJobStatus latestAiJobStatus,
        ResultStatus resultStatus,
        OffsetDateTime statusChangedAt,
        boolean retryable,
        @JsonInclude(JsonInclude.Include.NON_NULL) String signedUrl,
        OffsetDateTime createdAt
) {

    public record Author(String name, String role) {
    }
}

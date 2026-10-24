package com.gyote.medibridge.medical_document.query.model;

import com.gyote.medibridge.global.status.AiJobStatus;
import com.gyote.medibridge.global.status.DocumentStatus;
import com.gyote.medibridge.global.status.ResultStatus;
import com.gyote.medibridge.global.type.DocumentType;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/** Query application layer에서 사용하는 의료 문서 읽기 전용 모델입니다. */
public record MedicalDocumentView(
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
        Instant statusChangedAt,
        boolean retryable,
        String signedUrl,
        Instant createdAt
) {

    /** 문서를 올린 사용자입니다. */
    public record Author(String name, String role) {
    }
}

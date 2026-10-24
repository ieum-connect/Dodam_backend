package com.gyote.medibridge.medical_document.query.model;

import com.gyote.medibridge.global.status.AiJobStatus;
import com.gyote.medibridge.global.status.DocumentStatus;
import com.gyote.medibridge.global.status.ResultStatus;
import com.gyote.medibridge.global.type.DocumentType;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record AiDocumentListView(
        UUID documentId,
        String documentName,
        DocumentType documentType,
        UUID visitId,
        LocalDate visitedOn,
        Instant createdAt,
        String authorName,
        DocumentStatus documentStatus,
        AiJobStatus jobStatus,
        ResultStatus resultStatus
) {
}

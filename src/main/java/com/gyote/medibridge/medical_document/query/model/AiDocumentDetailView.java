package com.gyote.medibridge.medical_document.query.model;

import com.gyote.medibridge.global.status.AiJobStatus;
import com.gyote.medibridge.global.status.DocumentStatus;
import com.gyote.medibridge.global.status.ResultStatus;
import com.gyote.medibridge.global.type.DocumentType;

import java.time.Instant;
import java.util.UUID;

public record AiDocumentDetailView(
        UUID documentId,
        String documentName,
        DocumentType documentType,
        UUID visitId,
        DocumentStatus documentStatus,
        AiJobStatus jobStatus,
        ResultStatus resultStatus,
        String title,
        String content,
        Instant createdAt,
        long sectionCount,
        long citationCount
) {
}

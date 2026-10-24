package com.ieum.medibridge.medical_document.query.model;

import com.ieum.medibridge.global.status.AiJobStatus;
import com.ieum.medibridge.global.status.DocumentStatus;
import com.ieum.medibridge.global.status.ResultStatus;
import com.ieum.medibridge.global.type.DocumentType;

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

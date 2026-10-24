package com.ieum.dodam.medical_document.query.model;

import com.ieum.dodam.global.status.AiJobStatus;
import com.ieum.dodam.global.status.DocumentStatus;
import com.ieum.dodam.global.status.ResultStatus;
import com.ieum.dodam.global.type.DocumentType;

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

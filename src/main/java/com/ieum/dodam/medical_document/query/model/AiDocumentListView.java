package com.ieum.dodam.medical_document.query.model;

import com.ieum.dodam.global.status.AiJobStatus;
import com.ieum.dodam.global.status.DocumentStatus;
import com.ieum.dodam.global.status.ResultStatus;
import com.ieum.dodam.global.type.DocumentType;

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

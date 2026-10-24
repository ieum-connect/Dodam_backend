package com.ieum.medibridge.medical_document.query.model;

import com.ieum.medibridge.global.status.AiJobStatus;
import com.ieum.medibridge.global.status.DocumentStatus;
import com.ieum.medibridge.global.status.ResultStatus;
import com.ieum.medibridge.global.type.DocumentType;

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

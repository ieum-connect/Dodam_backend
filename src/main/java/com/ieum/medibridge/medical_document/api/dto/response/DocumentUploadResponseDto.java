package com.ieum.medibridge.medical_document.api.dto.response;

import com.ieum.medibridge.global.status.AiJobStatus;
import com.ieum.medibridge.global.status.DocumentStatus;

import java.util.UUID;

/** 문서 업로드 API 응답입니다. 저장소 경로(objectKey)는 공개하지 않습니다. */
public record DocumentUploadResponseDto(
        UUID documentId,
        UUID visitId,
        String mimeType,
        DocumentStatus documentStatus,
        AiJobStatus latestAiJobStatus,
        String requestId
) {
}

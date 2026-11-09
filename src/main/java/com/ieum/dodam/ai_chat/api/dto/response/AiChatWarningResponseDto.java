package com.ieum.dodam.ai_chat.api.dto.response;

import java.util.UUID;

public record AiChatWarningResponseDto(String code, UUID documentId, String message) {}

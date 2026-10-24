package com.ieum.dodam.ai_chat.api.dto.request;

import java.util.UUID;

public record CreateChatSessionRequestDto(UUID patientId, String title) {
}

package com.ieum.dodam.ai_chat.api.mapper;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ieum.dodam.ai_chat.api.dto.response.AiChatMessageResponseDto;
import com.ieum.dodam.ai_chat.api.dto.response.AiChatSessionResponseDto;
import com.ieum.dodam.ai_chat.api.dto.response.AiChatWarningResponseDto;
import com.ieum.dodam.ai_chat.query.model.AiChatMessageView;
import com.ieum.dodam.ai_chat.query.model.AiChatSessionView;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

/** AI 채팅 조회 모델을 HTTP 응답 DTO로 변환한다. */
@Component
public class AiChatResponseMapper {

    private final ObjectMapper objectMapper;

    public AiChatResponseMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /** 단일 채팅방 조회 결과를 HTTP 응답으로 변환한다. */
    public AiChatSessionResponseDto toSessionResponse(AiChatSessionView view) {
        return new AiChatSessionResponseDto(view.sessionId(), view.patientId(), view.title(), view.createdAt());
    }

    /** 채팅방 조회 결과 목록을 HTTP 응답 목록으로 변환한다. */
    public List<AiChatSessionResponseDto> toSessionResponses(List<AiChatSessionView> views) {
        return views.stream().map(this::toSessionResponse).toList();
    }

    /** 채팅 메시지 조회 결과 목록을 HTTP 응답 목록으로 변환한다. */
    public List<AiChatMessageResponseDto> toMessageResponses(List<AiChatMessageView> views) {
        return views.stream().map(view -> new AiChatMessageResponseDto(
                view.messageId(), view.role(), view.content(), view.createdAt(), view.resultDocumentId()
                , warnings(view.ragMetadataJson())
        )).toList();
    }

    private List<AiChatWarningResponseDto> warnings(String metadataJson) {
        if (metadataJson == null || metadataJson.isBlank()) {
            return List.of();
        }
        try {
            JsonNode warnings = objectMapper.readTree(metadataJson).path("warnings");
            if (!warnings.isArray()) return List.of();
            java.util.LinkedHashMap<String, AiChatWarningResponseDto> unique = new java.util.LinkedHashMap<>();
            warnings.forEach(node -> {
                String code = node.path("code").asText();
                if (code.isBlank()) return;
                UUID documentId = node.path("documentId").isTextual()
                        ? UUID.fromString(node.path("documentId").asText()) : null;
                unique.putIfAbsent(code, new AiChatWarningResponseDto(
                        code, documentId, warningMessage(code)
                ));
            });
            return List.copyOf(unique.values());
        } catch (Exception ignored) {
            return List.of();
        }
    }

    private String warningMessage(String code) {
        return switch (code) {
            case "PARTIAL_DOCUMENT_ANALYSIS" ->
                    "일부 페이지가 분석되지 않은 문서를 참고했습니다. 답변에 포함되지 않은 내용이 있을 수 있어요.";
            case "DOCUMENT_INDEX_PENDING" -> "일부 문서는 아직 검색 준비 중이라 준비된 문서만 참고했어요.";
            case "DOCUMENT_INDEX_FAILED" -> "일부 문서를 AI 검색용으로 준비하지 못했어요.";
            default -> "일부 문서 근거를 확인해 주세요.";
        };
    }
}

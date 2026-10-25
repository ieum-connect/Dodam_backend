package com.ieum.dodam.ai_result.infrastructure;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.ieum.dodam.ai_result.domain.QuestionType;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.UUID;
import java.nio.charset.StandardCharsets;

public class AiResultGenerationClient {

    private static final String TOKEN_HEADER = "X-Internal-Token";
    private final RestClient restClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /** 내부 인증 토큰을 포함한 AI Backend 전용 HTTP 클라이언트를 구성한다. */
    public AiResultGenerationClient(RestClient.Builder builder, String baseUrl, String token) {
        this.restClient = builder.baseUrl(baseUrl.replaceAll("/+$", ""))
                .defaultHeader(TOKEN_HEADER, token)
                .build();
    }

    /** AI Backend에 문서 근거와 질문을 전달해 답변 생성을 요청한다. */
    public GenerationResponse generate(GenerationRequest request) {
        final String payload;
        try {
            payload = objectMapper.writeValueAsString(request);
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("AI 요청을 JSON으로 변환하지 못했습니다.", exception);
        }
        return restClient.post()
                .uri("/internal/ai-result-documents/generate")
                .contentType(MediaType.APPLICATION_JSON)
                .contentLength(payload.getBytes(StandardCharsets.UTF_8).length)
                .body(payload.getBytes(StandardCharsets.UTF_8))
                .retrieve()
                .body(GenerationResponse.class);
    }

    public record GenerationRequest(
            @JsonProperty("request_id") UUID requestId,
            @JsonProperty("patient_id") UUID patientId,
            @JsonProperty("actor_user_id") UUID actorUserId,
            String question,
            @JsonProperty("question_type_hint") QuestionType questionTypeHint,
            List<Evidence> evidence
    ) {
    }

    public record Evidence(
            @JsonProperty("citation_id") UUID citationId,
            @JsonProperty("source_type") String sourceType,
            @JsonProperty("document_id") UUID documentId,
            @JsonProperty("page_no") Integer pageNo,
            String label,
            @JsonProperty("source_text") String sourceText,
            String value,
            String unit,
            @JsonProperty("reference_range") String referenceRange
    ) {
        /** Backward-compatible constructor for callers that retain an internal item id. */
        public Evidence(UUID citationId, String sourceType, UUID ignoredSourceId, UUID documentId,
                        Integer pageNo, String label, String sourceText, String value,
                        String unit, String referenceRange) {
            this(citationId, sourceType, documentId, pageNo, label, sourceText, value, unit, referenceRange);
        }
    }

    public record GenerationResponse(
            @JsonProperty("result_status") String resultStatus,
            String title,
            String answer,
            List<Sentence> sentences,
            boolean blocked,
            boolean failed,
            @JsonProperty("error_code") String errorCode,
            @JsonProperty("user_message") String userMessage
    ) {
    }

    public record Sentence(String text, @JsonProperty("citation_ids") List<UUID> citationIds) {
    }
}

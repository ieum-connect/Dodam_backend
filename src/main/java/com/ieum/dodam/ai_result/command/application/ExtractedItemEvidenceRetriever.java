package com.ieum.dodam.ai_result.command.application;

import com.ieum.dodam.ai_result.domain.repository.ResultDocumentEvidenceRepository;
import com.ieum.dodam.ai_result.infrastructure.AiResultGenerationClient;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

/** 현재 분석 결과의 추출 항목을 근거로 사용하는 기본 검색 구현이다. */
@Component
@RequiredArgsConstructor
public class ExtractedItemEvidenceRetriever implements AiEvidenceRetriever {

    private final ResultDocumentEvidenceRepository evidenceRepository;
    private final ObjectProvider<AiResultGenerationClient> generationClients;

    /** 분석 완료된 추출 항목을 AI Backend 근거 모델로 변환한다. */
    @Override
    public EvidenceResult retrieve(
            UUID patientId, UUID actorUserId, String question, List<UUID> documentIds
    ) {
        List<AiResultGenerationClient.Evidence> structured = evidenceRepository
                .findEvidence(patientId, documentIds).stream()
                .map(this::toEvidence)
                .toList();
        AiResultGenerationClient client = generationClients.getIfAvailable();
        if (client == null) {
            return new EvidenceResult(structured.stream().limit(20).toList(), List.of());
        }
        AiResultGenerationClient.EvidenceSearchResponse vector = client.searchEvidence(
                new AiResultGenerationClient.EvidenceSearchRequest(
                        UUID.randomUUID(), patientId, actorUserId, question, documentIds, 12
                )
        );
        List<AiResultGenerationClient.Evidence> merged = new java.util.ArrayList<>();
        if (vector != null && vector.items() != null) {
            vector.items().stream().map(AiResultGenerationClient.EvidenceSearchItem::toEvidence)
                    .forEach(merged::add);
        }
        java.util.Set<UUID> searchableDocuments = vector == null
                || vector.searchableDocumentIds() == null
                ? java.util.Set.of() : java.util.Set.copyOf(vector.searchableDocumentIds());
        structured.stream()
                .filter(item -> searchableDocuments.contains(item.documentId()))
                .filter(item -> merged.stream().noneMatch(existing ->
                        java.util.Objects.equals(existing.documentId(), item.documentId())
                                && java.util.Objects.equals(existing.sourceText(), item.sourceText())))
                .forEach(merged::add);
        return new EvidenceResult(
                merged.stream().limit(20).toList(),
                vector == null || vector.warnings() == null ? List.of() : vector.warnings()
        );
    }

    /** 저장소 조회 행을 citation 가능한 근거로 변환한다. */
    private AiResultGenerationClient.Evidence toEvidence(ResultDocumentEvidenceRepository.EvidenceRow row) {
        return new AiResultGenerationClient.Evidence(
                UUID.randomUUID(), "EXTRACTED_ITEM",
                UUID.fromString(row.getDocumentId()), row.getPageNo(), row.getLabel(), row.getSourceText(),
                row.getRawValue(), row.getUnit(), row.getReferenceRange()
        );
    }
}

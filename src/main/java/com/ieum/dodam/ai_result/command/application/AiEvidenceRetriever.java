package com.ieum.dodam.ai_result.command.application;

import com.ieum.dodam.ai_result.infrastructure.AiResultGenerationClient;

import java.util.List;
import java.util.UUID;

/** 질문에 사용할 권한 검증 근거를 조회해 AI Backend 요청 모델로 변환한다. */
public interface AiEvidenceRetriever {

    /** 대상 환자와 선택 문서 범위에서 답변 근거를 조회한다. */
    EvidenceResult retrieve(
            UUID patientId, UUID actorUserId, String question, List<UUID> documentIds
    );

    record EvidenceResult(
            List<AiResultGenerationClient.Evidence> evidence,
            List<AiResultGenerationClient.EvidenceWarning> warnings
    ) {
        public EvidenceResult {
            evidence = evidence == null ? List.of() : List.copyOf(evidence);
            warnings = warnings == null ? List.of() : List.copyOf(warnings);
        }
    }
}

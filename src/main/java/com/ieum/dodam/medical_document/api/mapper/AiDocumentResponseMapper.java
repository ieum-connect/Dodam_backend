package com.ieum.dodam.medical_document.api.mapper;

import com.ieum.dodam.medical_document.api.dto.response.AiDocumentExplanationStatusResponseDto;
import com.ieum.dodam.medical_document.api.dto.response.AiDocumentSectionItemResponseDto;
import com.ieum.dodam.medical_document.api.dto.response.AiDocumentSectionResponseDto;
import com.ieum.dodam.medical_document.api.dto.response.AiDocumentSectionsResponseDto;
import com.ieum.dodam.medical_document.query.model.AiDocumentExplanationStatusView;
import com.ieum.dodam.medical_document.query.model.AiDocumentSectionView;
import com.ieum.dodam.medical_document.query.model.AiDocumentSectionsView;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
public class AiDocumentResponseMapper {

    /** 섹션 조회 모델을 공통 응답 계약의 섹션 DTO로 변환한다. */
    public AiDocumentSectionsResponseDto toSectionsResponse(AiDocumentSectionsView view) {
        List<AiDocumentSectionResponseDto> sections = view.sections().stream()
                .map(this::toSection)
                .toList();
        return new AiDocumentSectionsResponseDto(uuid(view.documentId()), sections);
    }

    /** 상태 조회 모델을 공통 응답 계약의 상태 DTO로 변환한다. */
    public AiDocumentExplanationStatusResponseDto toStatusResponse(AiDocumentExplanationStatusView view) {
        return new AiDocumentExplanationStatusResponseDto(
                uuid(view.documentId()),
                view.jobStatus().name(),
                view.resultStatus() == null ? null : view.resultStatus().name(),
                view.currentStep(),
                view.progress(),
                view.completedAt(),
                view.detailUrl(),
                view.failedStep(),
                view.errorCode(),
                view.retryable(),
                view.originalDocumentUrl()
        );
    }

    /** 섹션 조회 모델을 섹션 응답 DTO로 변환한다. */
    private AiDocumentSectionResponseDto toSection(AiDocumentSectionView view) {
        List<AiDocumentSectionItemResponseDto> items = view.items().stream()
                .map(item -> new AiDocumentSectionItemResponseDto(
                        uuid(item.sentenceId()),
                        uuid(item.sourceItemId()),
                        item.label(),
                        item.value(),
                        item.unit(),
                        item.hasSource(),
                        uuid(item.citationId())
                ))
                .toList();
        return new AiDocumentSectionResponseDto(
                uuid(view.sectionId()),
                view.sectionType().name(),
                view.title(),
                items
        );
    }

    /** UUID를 API 응답용 문자열로 변환한다. */
    private static String uuid(UUID value) {
        return value == null ? null : value.toString();
    }
}

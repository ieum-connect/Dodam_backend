package com.ieum.medibridge.medical_document.query.model;

import java.util.UUID;

public record AiDocumentSectionItemView(
        UUID sentenceId,
        UUID sourceItemId,
        String label,
        String value,
        String unit,
        boolean hasSource,
        UUID citationId
) {
}

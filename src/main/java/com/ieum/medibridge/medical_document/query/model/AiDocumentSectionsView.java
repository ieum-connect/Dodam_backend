package com.ieum.medibridge.medical_document.query.model;

import java.util.List;
import java.util.UUID;

public record AiDocumentSectionsView(
        UUID documentId,
        List<AiDocumentSectionView> sections
) {
}

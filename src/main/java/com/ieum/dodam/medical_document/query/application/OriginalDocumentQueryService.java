package com.ieum.dodam.medical_document.query.application;

import com.ieum.dodam.global.exception.BusinessException;
import com.ieum.dodam.global.status.DocumentStatus;
import com.ieum.dodam.medical_document.domain.*;
import com.ieum.dodam.medical_document.domain.repository.MedicalDocumentRepository;
import com.ieum.dodam.medical_document.error.MedicalDocumentErrorCode;
import com.ieum.dodam.user.domain.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class OriginalDocumentQueryService {
    private final MedicalDocumentRepository documents;
    private final MedicalDocumentAccessPolicy access;
    private final OriginalDocumentStoragePort storage;

    public OriginalDocumentQueryService(MedicalDocumentRepository documents, MedicalDocumentAccessPolicy access, OriginalDocumentStoragePort storage) {
        this.documents = documents;
        this.access = access;
        this.storage = storage;
    }
    public record Original(byte[] content, String mimeType, String fileName) {}
    public Original get(User user, UUID id) {
        var document = documents.findByIdAndStatusNot(id, DocumentStatus.DELETED)
                .orElseThrow(() -> new BusinessException(MedicalDocumentErrorCode.DOCUMENT_NOT_FOUND));
        if (!access.canRead(user, document.getPatientId())) {
            throw new BusinessException(MedicalDocumentErrorCode.DOCUMENT_NOT_FOUND);
        }
        try {
            return new Original(storage.readOriginal(document.getStorageKey()), document.getMimeType(), document.getFileName());
        } catch (DocumentStorageException e) {
            throw new BusinessException(MedicalDocumentErrorCode.STORAGE_UNAVAILABLE);
        }
    }
}

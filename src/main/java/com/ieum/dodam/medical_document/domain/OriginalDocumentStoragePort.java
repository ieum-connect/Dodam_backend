package com.ieum.dodam.medical_document.domain;

public interface OriginalDocumentStoragePort {
    byte[] readOriginal(String storageKey);
}

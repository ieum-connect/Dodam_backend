package com.ieum.dodam.medical_document.query.model;

import java.time.LocalDate;

public interface MedicalDocumentAiStatusRow {

    String getDocumentId();

    LocalDate getVisitedOn();

    String getJobStatus();

    String getResultStatus();

    Boolean getRetryable();
}

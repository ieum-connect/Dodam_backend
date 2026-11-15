package com.ieum.dodam.medical_document.domain;

/** 업로드 시 보호자가 선택한 문서 유형입니다. AI의 분류 결과와 별도로 저장합니다. */
public enum DeclaredDocumentType {
    MEDICAL,
    PRESCRIPTION,
    LAB_RESULT,
    DIAGNOSIS,
    DISCHARGE_GUIDE,
    UNKNOWN
}

package com.ieum.dodam.medical_document.domain.repository;

import com.ieum.dodam.medical_document.domain.entity.MedicalDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface AiDocumentRepository extends JpaRepository<MedicalDocument, UUID> {

    /** 문서 식별자로 문서가 속한 환자를 조회한다. */
    @Query(value = """
            SELECT CAST(v.patient_id AS VARCHAR) AS "patientId"
            FROM documents d
            JOIN visits v ON v.id = d.visit_id
            WHERE d.id = :documentId
              AND d.deleted_at IS NULL
            """, nativeQuery = true)
    Optional<AiDocumentPatientRow> findPatientByDocumentId(@Param("documentId") UUID documentId);

    /** 문서-환자 조회 결과를 담는 프로젝션이다. */
    interface AiDocumentPatientRow {
        /** 환자 식별자를 반환한다. */
        String getPatientId();
    }
}

package com.ieum.dodam.ai_result.domain.repository;

import com.ieum.dodam.ai_result.domain.entity.AiResultDocument;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface AiResultDocumentRepository extends JpaRepository<AiResultDocument, UUID> {
}

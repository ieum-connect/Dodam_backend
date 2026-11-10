package com.ieum.dodam.welfare.domain.repository;

import com.ieum.dodam.welfare.domain.WelfareReviewStatus;
import com.ieum.dodam.welfare.domain.entity.WelfareConditionCodeMap;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WelfareConditionCodeMapRepository extends JpaRepository<WelfareConditionCodeMap, UUID> {
    Optional<WelfareConditionCodeMap> findByConditionCode(String conditionCode);
    List<WelfareConditionCodeMap> findByReviewStatus(WelfareReviewStatus reviewStatus);
}

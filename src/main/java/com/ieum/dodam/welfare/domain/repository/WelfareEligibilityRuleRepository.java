package com.ieum.dodam.welfare.domain.repository;

import com.ieum.dodam.welfare.domain.WelfareReviewStatus;
import com.ieum.dodam.welfare.domain.entity.WelfareEligibilityRule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface WelfareEligibilityRuleRepository extends JpaRepository<WelfareEligibilityRule, UUID> {
    List<WelfareEligibilityRule> findByProgramIdAndReviewStatus(UUID programId, WelfareReviewStatus reviewStatus);
}

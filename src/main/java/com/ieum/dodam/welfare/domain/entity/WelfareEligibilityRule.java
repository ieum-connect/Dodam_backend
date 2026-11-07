package com.ieum.dodam.welfare.domain.entity;

import com.fasterxml.jackson.databind.JsonNode;
import com.ieum.dodam.welfare.domain.WelfareReviewStatus;
import com.ieum.dodam.welfare.domain.WelfareRuleOperator;
import com.ieum.dodam.welfare.domain.WelfareRuleType;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** 사람이 검증한 공공서비스 자격 규칙. */
@Entity @Table(name = "welfare_eligibility_rules")
@Getter @NoArgsConstructor(access = AccessLevel.PROTECTED)
public class WelfareEligibilityRule {
    @Id private UUID id;
    @Column(name="program_id",nullable=false) private UUID programId;
    @Enumerated(EnumType.STRING) @Column(name="rule_type",nullable=false) private WelfareRuleType ruleType;
    @Enumerated(EnumType.STRING) @Column(nullable=false) private WelfareRuleOperator operator;
    @JdbcTypeCode(SqlTypes.JSON) @Column(name="value_json",nullable=false,columnDefinition="jsonb") private JsonNode valueJson;
    @Column(nullable=false) private boolean required;
    @Column(name="source_field",nullable=false,length=100) private String sourceField;
    @Column(name="source_code",length=20) private String sourceCode;
    @Column(nullable=false,precision=4,scale=3) private BigDecimal confidence;
    @Enumerated(EnumType.STRING) @Column(name="review_status",nullable=false) private WelfareReviewStatus reviewStatus;
    @Column(name="created_at",nullable=false) private Instant createdAt;
    @Column(name="updated_at",nullable=false) private Instant updatedAt;
}

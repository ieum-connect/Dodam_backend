package com.ieum.dodam.welfare.domain.entity;

import com.fasterxml.jackson.databind.JsonNode;
import com.ieum.dodam.welfare.domain.WelfareReviewStatus;
import com.ieum.dodam.welfare.domain.WelfareRuleType;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

/** JA 지원조건 코드의 검증된 의미와 값 변환표. */
@Entity @Table(name="welfare_condition_code_map")
@Getter @NoArgsConstructor(access=AccessLevel.PROTECTED)
public class WelfareConditionCodeMap {
    @Id private UUID id;
    @Column(name="condition_code",nullable=false,unique=true,length=20) private String conditionCode;
    @Column(name="display_name",nullable=false,length=200) private String displayName;
    @Column(columnDefinition="TEXT") private String description;
    @Enumerated(EnumType.STRING) @Column(name="normalized_rule_type") private WelfareRuleType normalizedRuleType;
    @JdbcTypeCode(SqlTypes.JSON) @Column(name="value_mapping",nullable=false,columnDefinition="jsonb") private JsonNode valueMapping;
    @Enumerated(EnumType.STRING) @Column(name="review_status",nullable=false) private WelfareReviewStatus reviewStatus;
    @Column(name="reviewed_by_user_id") private UUID reviewedByUserId;
    @Column(name="reviewed_at") private Instant reviewedAt;
    @Column(name="created_at",nullable=false) private Instant createdAt;
    @Column(name="updated_at",nullable=false) private Instant updatedAt;
}

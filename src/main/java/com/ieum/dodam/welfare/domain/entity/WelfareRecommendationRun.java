package com.ieum.dodam.welfare.domain.entity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import com.fasterxml.jackson.databind.JsonNode;
import java.time.Instant;
import java.util.UUID;
@Entity @Table(name="welfare_recommendation_runs") @Getter @NoArgsConstructor(access=AccessLevel.PROTECTED)
public class WelfareRecommendationRun {
 @Id private UUID id; @Column(name="patient_id",nullable=false) private UUID patientId;
 @Column(name="profile_version") private Long profileVersion; @Column(name="catalog_synced_at") private Instant catalogSyncedAt;
 @JdbcTypeCode(SqlTypes.JSON) @Column(name="profile_snapshot",nullable=false,columnDefinition="jsonb") private JsonNode profileSnapshot;
 @Column(name="created_at",nullable=false) private Instant createdAt;
 public static WelfareRecommendationRun create(UUID patientId, Long profileVersion, Instant catalogSyncedAt, JsonNode snapshot, Instant now){ WelfareRecommendationRun r=new WelfareRecommendationRun(); r.id=UUID.randomUUID(); r.patientId=patientId; r.profileVersion=profileVersion; r.catalogSyncedAt=catalogSyncedAt; r.profileSnapshot=snapshot; r.createdAt=now; return r; }
}

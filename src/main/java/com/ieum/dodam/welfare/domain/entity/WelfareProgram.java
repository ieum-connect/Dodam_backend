package com.ieum.dodam.welfare.domain.entity;

import com.fasterxml.jackson.databind.JsonNode;
import com.ieum.dodam.welfare.domain.WelfareProgramSource;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

/** 정부24 공공서비스의 정규화 필드와 원본 JSON을 함께 보존한다. */
@Entity
@Table(name = "welfare_programs")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class WelfareProgram {
    @Id private UUID id;
    @Column(name = "external_service_id", nullable = false, unique = true, length = 100) private String externalServiceId;
    @Column(nullable = false, length = 500) private String name;
    @Column(name = "support_type", length = 100) private String supportType;
    @Column(name = "purpose_summary", columnDefinition = "TEXT") private String purposeSummary;
    @Column(name = "purpose_detail", columnDefinition = "TEXT") private String purposeDetail;
    @Column(name = "target_text", columnDefinition = "TEXT") private String targetText;
    @Column(name = "selection_criteria_text", columnDefinition = "TEXT") private String selectionCriteriaText;
    @Column(name = "benefit_text", columnDefinition = "TEXT") private String benefitText;
    @Column(name = "application_method", columnDefinition = "TEXT") private String applicationMethod;
    @Column(name = "application_deadline", columnDefinition = "TEXT") private String applicationDeadline;
    @Column(name = "application_deadline_type", length = 100) private String applicationDeadlineType;
    @Column(name = "required_documents", columnDefinition = "TEXT") private String requiredDocuments;
    @Column(name = "official_check_documents", columnDefinition = "TEXT") private String officialCheckDocuments;
    @Column(name = "identity_check_documents", columnDefinition = "TEXT") private String identityCheckDocuments;
    @Column(name = "online_application_url", columnDefinition = "TEXT") private String onlineApplicationUrl;
    @Column(name = "detail_url", columnDefinition = "TEXT") private String detailUrl;
    @Column(name = "agency_code", length = 100) private String agencyCode;
    @Column(name = "agency_name", length = 500) private String agencyName;
    @Column(name = "agency_type", length = 100) private String agencyType;
    @Column(name = "department_name", length = 500) private String departmentName;
    @Column(name = "reception_agency", length = 500) private String receptionAgency;
    @Column(columnDefinition = "TEXT") private String contact;
    @Column(name = "service_category", length = 200) private String serviceCategory;
    @Column(name = "user_category", length = 200) private String userCategory;
    @Column(name = "view_count") private Long viewCount;
    @Column(name = "registered_at_source") private Instant registeredAtSource;
    @Column(name = "modified_at_source") private Instant modifiedAtSource;
    @Column(name = "last_synced_at", nullable = false) private Instant lastSyncedAt;
    @Column(nullable = false) private boolean active;
    @Column(name = "child_candidate", nullable = false) private boolean childCandidate;
    @JdbcTypeCode(SqlTypes.JSON) @Column(name = "raw_list_json", nullable = false, columnDefinition = "jsonb") private JsonNode rawListJson;
    @JdbcTypeCode(SqlTypes.JSON) @Column(name = "raw_detail_json", nullable = false, columnDefinition = "jsonb") private JsonNode rawDetailJson;
    @JdbcTypeCode(SqlTypes.JSON) @Column(name = "raw_condition_json", nullable = false, columnDefinition = "jsonb") private JsonNode rawConditionJson;
    @Column(name = "created_at", nullable = false) private Instant createdAt;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;

    public static WelfareProgram create(WelfareProgramSource source, Instant syncedAt) {
        WelfareProgram program = new WelfareProgram();
        program.id = UUID.randomUUID();
        program.externalServiceId = source.externalServiceId();
        program.createdAt = syncedAt;
        program.active = true;
        program.applyFields(source, syncedAt);
        return program;
    }

    /** 더 오래된 외부 응답은 현재 데이터를 되돌리지 않고 동기화 시각만 갱신한다. */
    public boolean updateFrom(WelfareProgramSource source, Instant syncedAt) {
        lastSyncedAt = syncedAt;
        if (modifiedAtSource != null && source.modifiedAtSource() != null
                && source.modifiedAtSource().isBefore(modifiedAtSource)) return false;
        applyFields(source, syncedAt);
        active = true;
        return true;
    }

    public void deactivate(Instant now) {
        if (active) {
            active = false;
            updatedAt = now;
        }
        lastSyncedAt = now;
    }

    private void applyFields(WelfareProgramSource s, Instant now) {
        name=s.name(); supportType=s.supportType(); purposeSummary=s.purposeSummary(); purposeDetail=s.purposeDetail();
        targetText=s.targetText(); selectionCriteriaText=s.selectionCriteriaText(); benefitText=s.benefitText();
        applicationMethod=s.applicationMethod(); applicationDeadline=s.applicationDeadline(); applicationDeadlineType=s.applicationDeadlineType();
        requiredDocuments=s.requiredDocuments(); officialCheckDocuments=s.officialCheckDocuments(); identityCheckDocuments=s.identityCheckDocuments();
        onlineApplicationUrl=s.onlineApplicationUrl(); detailUrl=s.detailUrl(); agencyCode=s.agencyCode(); agencyName=s.agencyName();
        agencyType=s.agencyType(); departmentName=s.departmentName(); receptionAgency=s.receptionAgency(); contact=s.contact();
        serviceCategory=s.serviceCategory(); userCategory=s.userCategory(); viewCount=s.viewCount();
        registeredAtSource=s.registeredAtSource(); modifiedAtSource=s.modifiedAtSource(); childCandidate=s.childCandidate();
        rawListJson=s.rawListJson(); rawDetailJson=s.rawDetailJson(); rawConditionJson=s.rawConditionJson();
        lastSyncedAt=now; updatedAt=now;
    }
}

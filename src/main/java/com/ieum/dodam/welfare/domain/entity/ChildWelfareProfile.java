package com.ieum.dodam.welfare.domain.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "child_welfare_profiles")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChildWelfareProfile {
    @Id private UUID id;
    @Column(name = "patient_id", nullable = false, unique = true) private UUID patientId;
    @Column(name = "storage_consent", nullable = false) private boolean storageConsent;
    @Column(name = "consented_at") private Instant consentedAt;
    @Column(name = "profile_version", nullable = false) private long profileVersion;
    @Column(name = "region_code", length = 20) private String regionCode;
    @Column(name = "household_size") private Integer householdSize;
    @Column(name = "income_band", length = 50) private String incomeBand;
    @Column(name = "child_count") private Integer childCount;
    @Column(name = "is_single_parent") private Boolean singleParent;
    @Column(name = "is_multicultural") private Boolean multicultural;
    @Column(name = "has_disability") private Boolean disability;
    @Column(name = "is_dual_income") private Boolean dualIncome;
    @Column(name = "childcare_status", length = 50) private String childcareStatus;
    @Column(name = "health_data_consent", nullable = false) private boolean healthDataConsent;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;

    public static ChildWelfareProfile create(UUID patientId, WelfareProfileCommand command, Instant now) {
        ChildWelfareProfile p = new ChildWelfareProfile();
        p.id = UUID.randomUUID(); p.patientId = patientId; p.profileVersion = 0;
        p.update(command, now); return p;
    }
    public void update(WelfareProfileCommand c, Instant now) {
        if (!c.storageConsent()) throw new IllegalArgumentException("프로필 저장 동의가 필요합니다.");
        storageConsent = true; consentedAt = consentedAt == null ? now : consentedAt;
        profileVersion++; regionCode=c.regionCode(); householdSize=c.householdSize(); incomeBand=c.incomeBand();
        childCount=c.childCount(); singleParent=c.singleParent(); multicultural=c.multicultural(); disability=c.disability();
        dualIncome=c.dualIncome(); childcareStatus=c.childcareStatus(); healthDataConsent=c.healthDataConsent(); updatedAt=now;
    }
    public void withdraw(Instant now) { storageConsent=false; consentedAt=null; updatedAt=now; }
    public record WelfareProfileCommand(boolean storageConsent, String regionCode, Integer householdSize, String incomeBand,
                                        Integer childCount, Boolean singleParent, Boolean multicultural, Boolean disability,
                                        Boolean dualIncome, String childcareStatus, boolean healthDataConsent) {}
}

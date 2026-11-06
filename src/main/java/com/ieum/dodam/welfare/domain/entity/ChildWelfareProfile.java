package com.ieum.dodam.welfare.domain.entity;

import com.ieum.dodam.welfare.domain.WelfareRegion;
import com.ieum.dodam.welfare.domain.WelfareRegionResolver;
import com.ieum.dodam.welfare.domain.WelfareRegionScope;
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
    @Column(name = "region_name", length = 120) private String regionName;
    @Column(name = "region_province", length = 40) private String regionProvince;
    @Column(name = "region_district", length = 60) private String regionDistrict;
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
        applyRegion(c);
        childCount=c.childCount(); singleParent=c.singleParent(); multicultural=c.multicultural(); disability=c.disability();
        dualIncome=c.dualIncome(); childcareStatus=c.childcareStatus(); healthDataConsent=c.healthDataConsent(); updatedAt=now;
    }
    public void withdraw(Instant now) { storageConsent=false; consentedAt=null; updatedAt=now; }

    /**
     * 거주지역 입력을 정규화해 보관한다. 프론트엔드는 법정동 코드({@code regionCode})와 전체 명칭
     * ({@code regionName})을 함께 보내지만, 명칭을 보내지 않던 과거 데이터는 코드 필드에 명칭이
     * 들어있을 수 있어 두 값을 모두 해석한다.
     */
    private void applyRegion(WelfareProfileCommand c) {
        String name = c.regionName() == null || c.regionName().isBlank() ? null : c.regionName().trim();
        regionName = name != null ? name : legacyRegionName();
        refreshRegion();
    }

    /** 저장된 명칭으로 시·도·시·군·구를 다시 판정한다. 값이 달라지면 true. */
    public boolean refreshRegion() {
        String previousProvince = regionProvince;
        String previousDistrict = regionDistrict;
        if (regionName == null) regionName = legacyRegionName();
        WelfareRegion region = WelfareRegionResolver.forProfile(regionName);
        // 명칭을 저장하지 못한 프로필은 법정동 코드로 시·도까지라도 판정한다.
        if (!region.hasProvince()) region = WelfareRegionResolver.forProfileCode(regionCode);
        regionProvince = region.province();
        regionDistrict = region.district();
        return !java.util.Objects.equals(previousProvince, regionProvince)
                || !java.util.Objects.equals(previousDistrict, regionDistrict);
    }

    /** 코드 형식이 아닌 과거 region_code 값은 행정구역 명칭으로 취급한다. */
    private String legacyRegionName() {
        return regionCode != null && !regionCode.isBlank() && !regionCode.matches("\\d{10}") ? regionCode.trim() : null;
    }

    /** 추천 규칙에서 사용하는 거주지역. 시·도를 확인하지 못하면 미확인으로 취급한다. */
    public WelfareRegion region() {
        if (regionProvince == null) {
            WelfareRegion byName = WelfareRegionResolver.forProfile(regionName != null ? regionName : legacyRegionName());
            return byName.hasProvince() ? byName : WelfareRegionResolver.forProfileCode(regionCode);
        }
        return new WelfareRegion(regionDistrict == null ? WelfareRegionScope.PROVINCE : WelfareRegionScope.DISTRICT,
                regionProvince, regionDistrict);
    }

    public record WelfareProfileCommand(boolean storageConsent, String regionCode, String regionName, Integer householdSize, String incomeBand,
                                        Integer childCount, Boolean singleParent, Boolean multicultural, Boolean disability,
                                        Boolean dualIncome, String childcareStatus, boolean healthDataConsent) {}
}

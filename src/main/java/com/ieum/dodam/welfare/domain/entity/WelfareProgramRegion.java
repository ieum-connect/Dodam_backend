package com.ieum.dodam.welfare.domain.entity;

import com.ieum.dodam.welfare.domain.WelfareRegion;
import com.ieum.dodam.welfare.domain.WelfareRegionScope;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * 사업이 제공되는 행정구역 한 건. 한 사업이 여러 지역을 가질 수 있다.
 *
 * <p>범위별로 채우는 칸이 정해져 있다. 전국·미확인은 시·도와 시·군·구가 모두 비고, 시·도 사업은
 * 시·도만, 시·군·구 사업은 시·군·구를 채운다. DB 체크 제약과 같은 규칙을 {@link #of}에서 적용해,
 * 판정에 쓰이지 않는 값이 남아 추천을 흔드는 일을 막는다.
 */
@Entity
@Table(name = "welfare_program_regions")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class WelfareProgramRegion {
    @Id
    private UUID id;

    @Column(name = "program_id", nullable = false)
    private UUID programId;

    @Enumerated(EnumType.STRING)
    @Column(name = "region_scope", nullable = false, length = 20)
    private WelfareRegionScope regionScope;

    @Column(name = "region_province", length = 40)
    private String regionProvince;

    @Column(name = "region_district", length = 60)
    private String regionDistrict;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public static WelfareProgramRegion of(UUID programId, WelfareRegion region, Instant now) {
        WelfareRegion normalized = normalize(region);
        WelfareProgramRegion entity = new WelfareProgramRegion();
        entity.id = UUID.randomUUID();
        entity.programId = programId;
        entity.regionScope = normalized.scope();
        entity.regionProvince = normalized.province();
        entity.regionDistrict = normalized.district();
        entity.createdAt = now;
        return entity;
    }

    /** 범위와 값이 어긋나면 표현할 수 있는 가장 넓은 범위로 낮춘다. */
    public static WelfareRegion normalize(WelfareRegion region) {
        if (region == null || region.scope() == null) return WelfareRegion.UNKNOWN;
        return switch (region.scope()) {
            case NATIONAL -> WelfareRegion.NATIONWIDE;
            case UNKNOWN -> WelfareRegion.UNKNOWN;
            case DISTRICT -> region.hasProvince() && region.hasDistrict()
                    ? new WelfareRegion(WelfareRegionScope.DISTRICT, region.province(), region.district())
                    : normalize(new WelfareRegion(WelfareRegionScope.PROVINCE, region.province(), null));
            case PROVINCE -> region.hasProvince()
                    ? new WelfareRegion(WelfareRegionScope.PROVINCE, region.province(), null)
                    : WelfareRegion.UNKNOWN;
        };
    }

    public WelfareRegion toRegion() {
        return new WelfareRegion(regionScope == null ? WelfareRegionScope.UNKNOWN : regionScope,
                regionProvince, regionDistrict);
    }

    /** 같은 지역인지 비교하는 키. 동기화에서 추가·삭제할 행을 가려내는 데 쓴다. */
    public String key() {
        return key(toRegion());
    }

    public static String key(WelfareRegion region) {
        WelfareRegion normalized = normalize(region);
        return normalized.scope() + "|"
                + (normalized.province() == null ? "" : normalized.province()) + "|"
                + (normalized.district() == null ? "" : normalized.district());
    }
}

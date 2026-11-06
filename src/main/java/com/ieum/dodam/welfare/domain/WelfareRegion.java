package com.ieum.dodam.welfare.domain;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 복지서비스 또는 사용자 프로필의 행정구역.
 *
 * <p>{@code province}는 정식 시·도명으로 정규화하고, {@code district}는 시·군·구 토큰을
 * 공백으로 이어 보관한다(예: {@code "수원시 장안구"}). 토큰 집합 포함 관계로 상위·하위를 판정한다.
 */
public record WelfareRegion(WelfareRegionScope scope, String province, String district) {

    public static final WelfareRegion NATIONWIDE = new WelfareRegion(WelfareRegionScope.NATIONAL, null, null);
    public static final WelfareRegion UNKNOWN = new WelfareRegion(WelfareRegionScope.UNKNOWN, null, null);

    public WelfareRegion {
        province = blankToNull(province);
        district = blankToNull(district);
    }

    public boolean hasProvince() { return province != null; }

    public boolean hasDistrict() { return district != null; }

    public Set<String> districtTokens() {
        if (district == null) return Set.of();
        return new LinkedHashSet<>(List.of(district.split("\\s+")));
    }

    /** 사람이 읽을 수 있는 지역 표기. */
    public String label() {
        return switch (scope) {
            case NATIONAL -> "전국";
            case UNKNOWN -> "지역 미확인";
            case PROVINCE, DISTRICT -> String.join(" ", province == null ? "" : province, district == null ? "" : district).trim();
        };
    }

    /**
     * 이 지역(사업 제공 범위)이 {@code user}가 사는 지역을 포함하는지 판정한다.
     *
     * <p>전국 사업은 항상 포함한다. 시·도 사업은 시·도가 같아야 한다. 시·군·구 사업은
     * 시·도가 같고 시·군·구 토큰이 사용자의 토큰에 포함되어야 한다.
     */
    public Coverage covers(WelfareRegion user) {
        if (scope == WelfareRegionScope.NATIONAL) return Coverage.COVERED;
        if (scope == WelfareRegionScope.UNKNOWN) return Coverage.UNDETERMINED;
        if (user == null || !user.hasProvince()) return Coverage.UNDETERMINED;
        if (hasProvince() && !province.equals(user.province())) return Coverage.OUTSIDE;
        if (!hasDistrict()) return Coverage.COVERED;
        if (!user.hasDistrict()) return Coverage.UNDETERMINED;
        return user.districtTokens().containsAll(districtTokens()) ? Coverage.COVERED : Coverage.OUTSIDE;
    }

    /** 사업 제공 지역과 사용자 거주지의 포함 관계. */
    public enum Coverage { COVERED, OUTSIDE, UNDETERMINED }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}

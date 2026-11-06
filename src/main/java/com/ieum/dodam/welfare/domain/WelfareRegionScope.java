package com.ieum.dodam.welfare.domain;

/** 복지서비스가 적용되는 행정구역 범위. 숫자가 클수록 더 좁은 지역에만 적용된다. */
public enum WelfareRegionScope {
    /** 전국 단위(중앙행정기관) 사업. */
    NATIONAL(0),
    /** 시·도 단위 사업. */
    PROVINCE(1),
    /** 시·군·구 단위 사업. */
    DISTRICT(2),
    /** 지역 정보를 판별하지 못한 사업. */
    UNKNOWN(0);

    private final int specificity;

    WelfareRegionScope(int specificity) { this.specificity = specificity; }

    /** 같은 점수일 때 더 좁은 지역 사업을 먼저 노출하기 위한 우선순위. */
    public int specificity() { return specificity; }
}

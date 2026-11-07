package com.ieum.dodam.welfare.domain;

import java.util.Arrays;
import java.util.Optional;

/**
 * 사용자가 선택하는 기준중위소득 구간.
 *
 * <p>프론트엔드는 실제 소득으로 가장 좁은 구간을 선택하므로 각 구간은 하한(초과)과 상한(이하)을
 * 함께 갖는다. 예를 들어 {@code BELOW_150}은 120% 초과 150% 이하를 뜻한다. 하한이 있으면
 * "이 가구는 최소 몇 % 이상"을 알 수 있어, 사업의 소득기준 초과를 근거 있게 판정할 수 있다.
 */
public enum WelfareIncomeBand {
    BELOW_50("기준중위소득 50% 이하", 0, 50),
    BELOW_75("기준중위소득 75% 이하", 50, 75),
    BELOW_100("기준중위소득 100% 이하", 75, 100),
    BELOW_120("기준중위소득 120% 이하", 100, 120),
    BELOW_150("기준중위소득 150% 이하", 120, 150),
    ABOVE_150("기준중위소득 150% 초과", 150, null);

    private final String label;
    private final int lowerExclusivePercent;
    private final Integer upperInclusivePercent;

    WelfareIncomeBand(String label, int lowerExclusivePercent, Integer upperInclusivePercent) {
        this.label = label;
        this.lowerExclusivePercent = lowerExclusivePercent;
        this.upperInclusivePercent = upperInclusivePercent;
    }

    public String label() { return label; }

    /** 이 구간에 속한 가구의 소득은 이 값보다 크다. */
    public int lowerExclusivePercent() { return lowerExclusivePercent; }

    /** 이 구간의 상한. 최상위 구간은 상한이 없어 null. */
    public Integer upperInclusivePercent() { return upperInclusivePercent; }

    public static Optional<WelfareIncomeBand> from(String value) {
        if (value == null || value.isBlank()) return Optional.empty();
        String normalized = value.trim().toUpperCase();
        return Arrays.stream(values()).filter(band -> band.name().equals(normalized)).findFirst();
    }
}

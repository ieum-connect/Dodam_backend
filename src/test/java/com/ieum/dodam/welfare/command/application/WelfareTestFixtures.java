package com.ieum.dodam.welfare.command.application;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.ieum.dodam.welfare.domain.WelfareProgramSource;
import com.ieum.dodam.welfare.domain.entity.ChildWelfareProfile;
import com.ieum.dodam.welfare.domain.entity.WelfareProgram;

import java.time.Instant;
import java.util.UUID;

/** 복지 추천 테스트에서 공유하는 사업·프로필 생성 도우미. */
final class WelfareTestFixtures {
    static final Instant NOW = Instant.parse("2026-11-06T00:00:00Z");

    private WelfareTestFixtures() {}

    static WelfareProgram program(String name, String agencyName, String target) {
        return program(name, null, agencyName, target, "지원금을 지급", NOW);
    }

    static WelfareProgram program(String name, String agencyType, String agencyName, String target, String benefit, Instant modifiedAt) {
        WelfareProgramSource source = new WelfareProgramSource(
                "SVC-" + UUID.randomUUID(), name, "현금", "요약", "목적",
                target, "신청기관 확인", benefit, "온라인", "상시", "상시", "신청서", null, null,
                null, "https://detail.example", "A1", agencyName, agencyType, "담당부서", "접수처", "02-0000-0000",
                "보육", "영유아", 1L, NOW, modifiedAt, true,
                JsonNodeFactory.instance.objectNode(), JsonNodeFactory.instance.objectNode(),
                JsonNodeFactory.instance.objectNode());
        return WelfareProgram.create(source, NOW);
    }

    static ProfileBuilder profile(String regionName) {
        return new ProfileBuilder(regionName);
    }

    /** 체크하지 않은 항목은 null로 남겨 "선택하지 않음"을 그대로 표현한다. */
    static final class ProfileBuilder {
        private final String regionName;
        private String incomeBand;
        private Integer householdSize = 4;
        private Integer childCount;
        private Boolean singleParent;
        private Boolean multicultural;
        private Boolean disability;
        private Boolean dualIncome;
        private String childcareStatus;

        private ProfileBuilder(String regionName) { this.regionName = regionName; }

        ProfileBuilder income(String band) { this.incomeBand = band; return this; }
        ProfileBuilder childCount(Integer value) { this.childCount = value; return this; }
        ProfileBuilder singleParent(Boolean value) { this.singleParent = value; return this; }
        ProfileBuilder multicultural(Boolean value) { this.multicultural = value; return this; }
        ProfileBuilder disability(Boolean value) { this.disability = value; return this; }
        ProfileBuilder dualIncome(Boolean value) { this.dualIncome = value; return this; }
        ProfileBuilder childcare(String value) { this.childcareStatus = value; return this; }

        ChildWelfareProfile build() {
            return ChildWelfareProfile.create(UUID.randomUUID(), new ChildWelfareProfile.WelfareProfileCommand(
                    true, null, regionName, householdSize, incomeBand, childCount,
                    singleParent, multicultural, disability, dualIncome, childcareStatus, false), NOW);
        }
    }
}

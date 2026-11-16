package com.ieum.dodam.welfare.domain.entity;

import com.ieum.dodam.welfare.domain.WelfareRegion;
import com.ieum.dodam.welfare.domain.WelfareRegionScope;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class WelfareProgramRegionTest {
    @Test
    void rejectsDistrictWithoutProvince() {
        WelfareRegion normalized = WelfareProgramRegion.normalize(
                new WelfareRegion(WelfareRegionScope.DISTRICT, null, "기장군"));

        assertThat(normalized).isEqualTo(WelfareRegion.UNKNOWN);
    }

    @Test
    void keepsDistrictWithProvince() {
        WelfareRegion district = new WelfareRegion(WelfareRegionScope.DISTRICT, "부산광역시", "기장군");

        assertThat(WelfareProgramRegion.normalize(district)).isEqualTo(district);
    }
}

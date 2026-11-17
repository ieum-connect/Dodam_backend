package com.ieum.dodam.welfare.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

class WelfareRegionResolverTest {

    @ParameterizedTest(name = "소관기관 \"{0}\" -> {1}/{2}/{3}")
    @CsvSource(nullValues = "null", value = {
            "'전라남도 영광군', DISTRICT, 전라남도, 영광군",
            "'영광군',         UNKNOWN, null,   null",
            "'서울특별시 강동구', DISTRICT, 서울특별시, 강동구",
            "'강동구청',       UNKNOWN, null,   null",
            "'경기도 수원시 장안구', DISTRICT, 경기도, '수원시 장안구'",
            "'경기도 광주시',  DISTRICT, 경기도, 광주시",
            "'대구광역시',     PROVINCE, 대구광역시, null",
            "'서울특별시교육청', PROVINCE, 서울특별시, null",
            "'보건복지부',     NATIONAL, null,   null"
    })
    void parsesAgencyNameIntoRegionHierarchy(String agencyName, WelfareRegionScope scope, String province, String district) {
        WelfareRegion region = WelfareRegionResolver.forProgram(null, agencyName, "");

        assertThat(region.scope()).isEqualTo(scope);
        assertThat(region.province()).isEqualTo(province);
        assertThat(region.district()).isEqualTo(district);
    }

    @Test
    @DisplayName("중앙행정기관 유형은 기관명과 무관하게 전국 사업이다")
    void treatsCentralAgencyAsNationwide() {
        assertThat(WelfareRegionResolver.forProgram("중앙행정기관", "서울특별시 소재 기관", "").scope())
                .isEqualTo(WelfareRegionScope.NATIONAL);
    }

    @Test
    @DisplayName("기관명에 지역이 없으면 본문의 시·도 약칭으로 범위를 좁힌다")
    void fallsBackToProgramTextForProvince() {
        WelfareRegion region = WelfareRegionResolver.forProgram(null, "", "전남 거주 아동 대상 지원");

        assertThat(region.scope()).isEqualTo(WelfareRegionScope.PROVINCE);
        assertThat(region.province()).isEqualTo("전라남도");
    }

    @Test
    @DisplayName("사용자가 고른 행정구역 명칭을 시·도와 시·군·구로 정규화한다")
    void normalizesProfileRegionName() {
        WelfareRegion region = WelfareRegionResolver.forProfile("서울특별시 강동구 길동");

        assertThat(region.province()).isEqualTo("서울특별시");
        assertThat(region.district()).isEqualTo("강동구");
    }

    @Test
    @DisplayName("서울 강동구 길동 거주자는 전국·서울시·강동구 사업을 모두 신청할 수 있다")
    void coversNationwideProvinceAndOwnDistrict() {
        WelfareRegion user = WelfareRegionResolver.forProfile("서울특별시 강동구 길동");

        assertThat(WelfareRegion.NATIONWIDE.covers(user)).isEqualTo(WelfareRegion.Coverage.COVERED);
        assertThat(program("서울특별시").covers(user)).isEqualTo(WelfareRegion.Coverage.COVERED);
        assertThat(program("서울특별시 강동구").covers(user)).isEqualTo(WelfareRegion.Coverage.COVERED);
        assertThat(program("강동구청").covers(user)).isEqualTo(WelfareRegion.Coverage.UNDETERMINED);
    }

    @Test
    @DisplayName("다른 시·도와 다른 시·군·구 사업은 거주지 밖으로 판정한다")
    void rejectsOtherRegions() {
        WelfareRegion user = WelfareRegionResolver.forProfile("서울특별시 강동구 길동");

        assertThat(program("서울특별시 송파구").covers(user)).isEqualTo(WelfareRegion.Coverage.OUTSIDE);
        assertThat(program("전라남도").covers(user)).isEqualTo(WelfareRegion.Coverage.OUTSIDE);
        assertThat(program("전라남도 영광군").covers(user)).isEqualTo(WelfareRegion.Coverage.OUTSIDE);
        assertThat(program("영광군").covers(user)).isEqualTo(WelfareRegion.Coverage.UNDETERMINED);
    }

    @Test
    @DisplayName("시·도만 선택한 사용자에게 시·군·구 사업은 추가 확인 대상이다")
    void keepsDistrictProgramUndeterminedWhenUserPickedProvinceOnly() {
        WelfareRegion user = WelfareRegionResolver.forProfile("대구광역시");

        assertThat(program("대구광역시 달서구").covers(user)).isEqualTo(WelfareRegion.Coverage.UNDETERMINED);
        assertThat(program("대구광역시").covers(user)).isEqualTo(WelfareRegion.Coverage.COVERED);
        assertThat(program("전라남도 영광군").covers(user)).isEqualTo(WelfareRegion.Coverage.OUTSIDE);
    }

    @Test
    @DisplayName("거주지를 입력하지 않으면 지역 사업은 판정하지 않고 전국 사업만 확정한다")
    void requiresUserRegionForLocalPrograms() {
        assertThat(program("서울특별시").covers(WelfareRegion.UNKNOWN)).isEqualTo(WelfareRegion.Coverage.UNDETERMINED);
        assertThat(WelfareRegion.NATIONWIDE.covers(WelfareRegion.UNKNOWN)).isEqualTo(WelfareRegion.Coverage.COVERED);
    }

    @Test
    @DisplayName("명칭 없이 법정동 코드만 저장된 프로필도 시·도까지는 판정한다")
    void fallsBackToLegalDongCodeForProvince() {
        WelfareRegion region = WelfareRegionResolver.forProfileCode("1171010100");

        assertThat(region.scope()).isEqualTo(WelfareRegionScope.PROVINCE);
        assertThat(region.province()).isEqualTo("서울특별시");
        assertThat(WelfareRegionResolver.forProfileCode("4611000000").province()).isEqualTo("전라남도");
        assertThat(WelfareRegionResolver.forProfileCode("서울특별시")).isEqualTo(WelfareRegion.UNKNOWN);
    }

    @Test
    @DisplayName("코드로만 판정한 시·도는 다른 시·도 사업을 그대로 걸러낸다")
    void codeOnlyProvinceStillFiltersOtherProvinces() {
        WelfareRegion user = WelfareRegionResolver.forProfileCode("1171010100");

        assertThat(program("서울특별시 송파구").covers(user)).isEqualTo(WelfareRegion.Coverage.UNDETERMINED);
        assertThat(program("전라남도 영광군").covers(user)).isEqualTo(WelfareRegion.Coverage.OUTSIDE);
        assertThat(program("서울특별시").covers(user)).isEqualTo(WelfareRegion.Coverage.COVERED);
    }

    @Test
    @DisplayName("기관 이름 일부를 시·군·구로 오인해도 지역 사업으로 확정하지 않는다")
    void doesNotTreatFalseDistrictTokenAsARegion() {
        WelfareRegion region = WelfareRegionResolver.forProgram(null, "보건의료인국가시험원", "");

        assertThat(region).isEqualTo(WelfareRegion.UNKNOWN);
    }

    @Test
    @DisplayName("시·도가 빠진 고유 시·군·구 사업은 같은 지역 사용자에게만 일치로 복원한다")
    void matchesUniqueUnqualifiedDistrictAgainstUser() {
        WelfareRegion gangdongUser = WelfareRegionResolver.forProfile("서울특별시 강동구 길동");
        WelfareRegion recovered = WelfareRegionResolver.matchUnqualifiedDistrictToUser("강동구시설관리공단", gangdongUser);

        assertThat(recovered).isEqualTo(new WelfareRegion(WelfareRegionScope.DISTRICT, "서울특별시", "강동구"));
        assertThat(WelfareRegionResolver.matchUnqualifiedDistrictToUser(
                "영광군시설관리공단", gangdongUser)).isEqualTo(WelfareRegion.UNKNOWN);
    }

    @Test
    @DisplayName("동명이 있는 구는 사용자 주소만으로 시·도를 추정하지 않는다")
    void leavesAmbiguousDistrictUnknown() {
        WelfareRegion seoulJungGu = WelfareRegionResolver.forProfile("서울특별시 중구 명동");

        assertThat(WelfareRegionResolver.matchUnqualifiedDistrictToUser("중구시설관리공단", seoulJungGu))
                .isEqualTo(WelfareRegion.UNKNOWN);
    }

    @Test
    @DisplayName("도시공사 명칭은 실제 시 이름으로 보정해 사용자 지역과 대조한다")
    void normalizesCityCorporationSuffix() {
        WelfareRegion user = WelfareRegionResolver.forProfile("경기도 고양시 일산동구");

        assertThat(WelfareRegionResolver.matchUnqualifiedDistrictToUser("고양도시관리공사", user).district())
                .isEqualTo("고양시");
    }

    private WelfareRegion program(String agencyName) {
        return WelfareRegionResolver.forProgram(null, agencyName, "");
    }
}

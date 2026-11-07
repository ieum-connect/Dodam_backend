package com.ieum.dodam.welfare.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class WelfareAgeRangeTest {

    @ParameterizedTest(name = "\"{0}\" -> 30개월 아이 포함 {1}")
    @CsvSource({
            "'만 0세~5세 아동', true",
            "'만 8세 이하 아동', true",
            "'36개월 미만 영아', true",
            "'24개월~71개월 유아', true",
            "'아동(만 18세 미만)', true",
            "'초등학생 자녀', false",
            "'만 3세 이상 미취학 아동', false",
            "'만 65세 이상 노인', false"
    })
    void decidesWhetherThirtyMonthOldIsEligible(String text, boolean contains) {
        assertThat(WelfareAgeRange.parse(text).orElseThrow().contains(30)).isEqualTo(contains);
    }

    @Test
    @DisplayName("연령 근거가 없으면 판정하지 않는다")
    void returnsEmptyWithoutAgeEvidence() {
        assertThat(WelfareAgeRange.parse("지원대상 제한 없음")).isEmpty();
        assertThat(WelfareAgeRange.parse("")).isEmpty();
    }

    @Test
    @DisplayName("보호자 연령과 아이 연령이 섞여 모순되면 판정을 포기한다")
    void givesUpWhenRangesContradict() {
        assertThat(WelfareAgeRange.parse("부모 만 19세 이상, 자녀 만 8세 이하")).isEmpty();
    }

    @Test
    @DisplayName("구체 키워드가 있으면 포괄 키워드로 범위를 넓히지 않는다")
    void prefersSpecificKeywordOverGenericOne() {
        Optional<WelfareAgeRange> range = WelfareAgeRange.parse("초등학생 자녀를 둔 가정");

        assertThat(range).isPresent();
        assertThat(range.get().minMonths()).isEqualTo(84);
        assertThat(range.get().maxMonths()).isEqualTo(155);
    }

    @Test
    @DisplayName("이하와 미만을 구분한다")
    void distinguishesInclusiveAndExclusiveUpperBounds() {
        assertThat(WelfareAgeRange.parse("만 5세 이하").orElseThrow().maxMonths()).isEqualTo(71);
        assertThat(WelfareAgeRange.parse("만 5세 미만").orElseThrow().maxMonths()).isEqualTo(59);
    }
}

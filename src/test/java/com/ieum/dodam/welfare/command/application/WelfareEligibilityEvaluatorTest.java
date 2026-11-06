package com.ieum.dodam.welfare.command.application;

import com.ieum.dodam.welfare.command.application.WelfareEligibilityEvaluator.Evaluation;
import com.ieum.dodam.welfare.command.application.WelfareEligibilityEvaluator.Status;
import com.ieum.dodam.welfare.domain.entity.ChildWelfareProfile;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static com.ieum.dodam.welfare.command.application.WelfareTestFixtures.profile;
import static com.ieum.dodam.welfare.command.application.WelfareTestFixtures.program;
import static org.assertj.core.api.Assertions.assertThat;

class WelfareEligibilityEvaluatorTest {
    private static final int THIRTY_MONTHS = 30;
    private final WelfareEligibilityEvaluator evaluator = new WelfareEligibilityEvaluator();

    @Nested
    @DisplayName("거주지역")
    class Region {
        private final ChildWelfareProfile seoulGangdong = profile("서울특별시 강동구 길동").build();

        @Test
        @DisplayName("다른 시·도 사업은 추천하지 않는다")
        void excludesOtherProvince() {
            Evaluation evaluation = evaluator.evaluate(
                    program("전남형 아동 지원", "전라남도 영광군", "만 0세~5세 아동"), seoulGangdong, THIRTY_MONTHS);

            assertThat(evaluation.recommendable()).isFalse();
            assertThat(evaluation.exclusionReason()).contains("서울특별시 강동구");
        }

        @Test
        @DisplayName("시·도명이 빠진 다른 시·군·구 사업도 추천하지 않는다")
        void excludesOtherDistrictWithoutProvincePrefix() {
            assertThat(evaluator.evaluate(program("영광군 아동 지원", "영광군", "만 0세~5세 아동"),
                    seoulGangdong, THIRTY_MONTHS).recommendable()).isFalse();
        }

        @Test
        @DisplayName("같은 시·도의 다른 시·군·구 사업도 추천하지 않는다")
        void excludesSiblingDistrict() {
            assertThat(evaluator.evaluate(program("송파구 아동 지원", "서울특별시 송파구", "만 0세~5세 아동"),
                    seoulGangdong, THIRTY_MONTHS).recommendable()).isFalse();
        }

        @Test
        @DisplayName("전국·거주 시·도·거주 시·군·구 사업은 모두 추천한다")
        void includesNationwideProvinceAndOwnDistrict() {
            assertThat(evaluator.evaluate(program("아동수당", "중앙행정기관", "보건복지부", "만 0세~7세 아동", "수당 지급", WelfareTestFixtures.NOW),
                    seoulGangdong, THIRTY_MONTHS).recommendable()).isTrue();
            assertThat(evaluator.evaluate(program("서울형 아이돌봄", "서울특별시", "만 0세~5세 아동"),
                    seoulGangdong, THIRTY_MONTHS).recommendable()).isTrue();
            assertThat(evaluator.evaluate(program("강동구 양육 지원", "서울특별시 강동구", "만 0세~5세 아동"),
                    seoulGangdong, THIRTY_MONTHS).recommendable()).isTrue();
        }

        @Test
        @DisplayName("시·도만 선택했으면 시·군·구 사업은 확인 필요로 남긴다")
        void marksDistrictProgramAsUnknownWhenOnlyProvinceSelected() {
            Evaluation evaluation = evaluator.evaluate(program("달서구 양육 지원", "대구광역시 달서구", "만 0세~5세 아동"),
                    profile("대구광역시").build(), THIRTY_MONTHS);

            assertThat(evaluation.recommendable()).isTrue();
            assertThat(status(evaluation, "REGION")).isEqualTo(Status.UNKNOWN);
            assertThat(evaluation.score()).isLessThan(100);
        }
    }

    @Nested
    @DisplayName("선택하지 않은 가구 요건")
    class HouseholdGates {
        private final ChildWelfareProfile nothingChecked = profile("서울특별시 강동구").build();

        @Test
        @DisplayName("장애를 체크하지 않으면 장애 대상 사업을 추천하지 않는다")
        void excludesDisabilityProgramWhenNotChecked() {
            Evaluation evaluation = evaluator.evaluate(
                    program("장애아동 양육 지원", "서울특별시", "장애아동을 양육하는 가정"), nothingChecked, THIRTY_MONTHS);

            assertThat(evaluation.recommendable()).isFalse();
            assertThat(evaluation.exclusionReason()).contains("장애");
        }

        @Test
        @DisplayName("한부모를 체크하지 않으면 한부모 대상 사업을 추천하지 않는다")
        void excludesSingleParentProgramWhenNotChecked() {
            assertThat(evaluator.evaluate(program("한부모가족 아동양육비", "서울특별시", "한부모가족 지원대상 가구"),
                    nothingChecked, THIRTY_MONTHS).recommendable()).isFalse();
        }

        @Test
        @DisplayName("다문화를 체크하지 않으면 다문화 대상 사업을 추천하지 않는다")
        void excludesMulticulturalProgramWhenNotChecked() {
            assertThat(evaluator.evaluate(program("다문화가족 자녀 지원", "서울특별시", "결혼이민자 자녀"),
                    nothingChecked, THIRTY_MONTHS).recommendable()).isFalse();
        }

        @Test
        @DisplayName("요건을 체크했으면 추천하고 충족 조건으로 표시한다")
        void includesGatedProgramWhenConditionSelected() {
            Evaluation evaluation = evaluator.evaluate(
                    program("장애아동 양육 지원", "서울특별시", "장애아동을 양육하는 가정"),
                    profile("서울특별시 강동구").disability(true).build(), THIRTY_MONTHS);

            assertThat(evaluation.recommendable()).isTrue();
            assertThat(status(evaluation, "DISABILITY")).isEqualTo(Status.MET);
        }

        @Test
        @DisplayName("자녀 수가 모자라면 다자녀 사업을 추천하지 않는다")
        void excludesMultiChildProgramWhenChildCountIsTooLow() {
            assertThat(evaluator.evaluate(program("다자녀 가정 지원", "서울특별시", "다자녀 가구의 아동"),
                    profile("서울특별시 강동구").childCount(1).build(), THIRTY_MONTHS).recommendable()).isFalse();
            assertThat(evaluator.evaluate(program("다자녀 가정 지원", "서울특별시", "다자녀 가구의 아동"),
                    profile("서울특별시 강동구").childCount(2).build(), THIRTY_MONTHS).recommendable()).isTrue();
        }

        @Test
        @DisplayName("프로필로 확인할 수 없는 특수 자격 사업은 추천하지 않는다")
        void excludesUnverifiableStatusProgram() {
            assertThat(evaluator.evaluate(program("북한이탈주민 자녀 지원", "서울특별시", "만 0세~5세 아동"),
                    nothingChecked, THIRTY_MONTHS).recommendable()).isFalse();
        }
    }

    @Nested
    @DisplayName("연령과 소득")
    class AgeAndIncome {
        @Test
        @DisplayName("대상 연령을 벗어나면 추천하지 않는다")
        void excludesProgramOutsideAgeRange() {
            Evaluation evaluation = evaluator.evaluate(program("초등 돌봄교실", "서울특별시", "초등학생 자녀"),
                    profile("서울특별시 강동구").build(), THIRTY_MONTHS);

            assertThat(evaluation.recommendable()).isFalse();
            assertThat(evaluation.exclusionReason()).contains("지원대상 연령");
        }

        @Test
        @DisplayName("소득구간 하한이 지원기준을 넘으면 미충족으로 표시한다")
        void marksIncomeAsUnmetWhenBandExceedsLimit() {
            Evaluation evaluation = evaluator.evaluate(
                    program("저소득 가구 양육 지원", "서울특별시", "기준중위소득 100% 이하 가구의 만 0세~5세 아동"),
                    profile("서울특별시 강동구").income("BELOW_150").build(), THIRTY_MONTHS);

            assertThat(status(evaluation, "INCOME")).isEqualTo(Status.UNMET);
            assertThat(evaluation.status()).isEqualTo("POSSIBLE");
        }

        @Test
        @DisplayName("소득구간이 지원기준 안에 들면 충족으로 표시한다")
        void marksIncomeAsMetWhenBandFitsLimit() {
            Evaluation evaluation = evaluator.evaluate(
                    program("저소득 가구 양육 지원", "서울특별시", "기준중위소득 150% 이하 가구의 만 0세~5세 아동"),
                    profile("서울특별시 강동구").income("BELOW_100").build(), THIRTY_MONTHS);

            assertThat(status(evaluation, "INCOME")).isEqualTo(Status.MET);
            assertThat(evaluation.score()).isEqualTo(100);
            assertThat(evaluation.status()).isEqualTo("ELIGIBLE");
        }

        @Test
        @DisplayName("문서에 여러 소득기준이 있으면 가장 넓은 기준을 적용한다")
        void usesMostGenerousIncomeLimit() {
            Evaluation evaluation = evaluator.evaluate(
                    program("양육 지원", "서울특별시",
                            "기준중위소득 75% 이하 우선 지원, 기준중위소득 150% 이하 가구의 만 0세~5세 아동"),
                    profile("서울특별시 강동구").income("BELOW_120").build(), THIRTY_MONTHS);

            assertThat(status(evaluation, "INCOME")).isEqualTo(Status.MET);
        }
    }

    @Nested
    @DisplayName("일치율")
    class Score {
        @Test
        @DisplayName("요구 조건을 모두 충족하면 100%가 된다")
        void reachesFullScoreWhenEveryConditionIsMet() {
            Evaluation evaluation = evaluator.evaluate(program("아동수당", "중앙행정기관", "보건복지부", "만 0세~7세 아동", "수당 지급", WelfareTestFixtures.NOW),
                    profile("서울특별시 강동구").build(), THIRTY_MONTHS);

            assertThat(evaluation.score()).isEqualTo(100);
            assertThat(evaluation.status()).isEqualTo("ELIGIBLE");
        }

        @Test
        @DisplayName("확인할 수 없는 조건은 부분 점수만 받고 상태도 확인 필요가 된다")
        void givesPartialCreditForUnknownConditions() {
            Evaluation evaluation = evaluator.evaluate(program("양육 지원", "서울특별시", "지원대상 제한 없음"),
                    profile("서울특별시 강동구").build(), THIRTY_MONTHS);

            assertThat(status(evaluation, "AGE")).isEqualTo(Status.UNKNOWN);
            assertThat(evaluation.score()).isBetween(50, 90);
            assertThat(evaluation.status()).isEqualTo("POSSIBLE");
        }

        @Test
        @DisplayName("가족정보가 없으면 지역·소득 조건을 확인 필요로 남긴다")
        void keepsConditionsUnknownWithoutProfile() {
            Evaluation evaluation = evaluator.evaluate(program("서울형 아이돌봄", "서울특별시", "만 0세~5세 아동"), null, THIRTY_MONTHS);

            assertThat(evaluation.recommendable()).isTrue();
            assertThat(status(evaluation, "REGION")).isEqualTo(Status.UNKNOWN);
            assertThat(evaluation.missingMessages()).isNotEmpty();
        }
    }

    private Status status(Evaluation evaluation, String key) {
        return evaluation.conditions().stream().filter(condition -> condition.key().equals(key))
                .findFirst().orElseThrow(() -> new AssertionError(key + " 조건이 없습니다")).status();
    }
}

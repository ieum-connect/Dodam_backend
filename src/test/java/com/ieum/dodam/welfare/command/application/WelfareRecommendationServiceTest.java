package com.ieum.dodam.welfare.command.application;

import com.ieum.dodam.patient.domain.Patient;
import com.ieum.dodam.patient.domain.repository.PatientRepository;
import com.ieum.dodam.welfare.command.application.WelfareRecommendationService.RecommendationResult;
import com.ieum.dodam.welfare.domain.WelfareRegionScope;
import com.ieum.dodam.welfare.domain.entity.ChildWelfareProfile;
import com.ieum.dodam.welfare.domain.entity.WelfareProgram;
import com.ieum.dodam.welfare.domain.repository.ChildWelfareProfileRepository;
import com.ieum.dodam.welfare.domain.repository.WelfareProgramRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static com.ieum.dodam.welfare.command.application.WelfareTestFixtures.profile;
import static com.ieum.dodam.welfare.command.application.WelfareTestFixtures.program;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class WelfareRecommendationServiceTest {
    private static final Instant NOW = Instant.parse("2026-11-06T00:00:00Z");
    private static final UUID CHILD_ID = UUID.randomUUID();

    private final PatientRepository patients = mock(PatientRepository.class);
    private final ChildWelfareProfileRepository profiles = mock(ChildWelfareProfileRepository.class);
    private final WelfareProgramRepository programs = mock(WelfareProgramRepository.class);
    private final WelfareRecommendationService service = new WelfareRecommendationService(
            patients, profiles, programs, new WelfareEligibilityEvaluator(), Clock.fixed(NOW, ZoneOffset.UTC));

    @Test
    void usesSeoulDateForChildrenBornJustAfterMidnight() {
        given(null);
        patients.findById(CHILD_ID).orElseThrow().setBirthDate(LocalDate.of(2026, 11, 6));
        var midnightService = new WelfareRecommendationService(patients, profiles, programs,
                new WelfareEligibilityEvaluator(), Clock.fixed(Instant.parse("2026-11-05T15:30:00Z"), ZoneOffset.UTC));

        assertThat(midnightService.recommend(CHILD_ID, false).child().ageMonths()).isZero();
    }

    @Test
    @DisplayName("거주지역 밖의 사업은 목록에서 빠지고 제외 건수로 집계된다")
    void dropsProgramsOutsideUserRegion() {
        given(profile("서울특별시 강동구 길동").build(),
                program("강동구 양육 지원", "서울특별시 강동구", "만 0세~5세 아동"),
                program("전남형 아동 지원", "전라남도 영광군", "만 0세~5세 아동"),
                program("송파구 양육 지원", "서울특별시 송파구", "만 0세~5세 아동"));

        RecommendationResult result = service.recommend(CHILD_ID, false);

        assertThat(result.items()).extracting(WelfareRecommendationService.RecommendationItem::name)
                .containsExactly("강동구 양육 지원");
        assertThat(result.excludedCount()).isEqualTo(2);
    }

    @Test
    @DisplayName("일치율이 같으면 더 좁은 지역 사업을 먼저 보여준다")
    void prefersMoreSpecificRegionOnTie() {
        given(profile("서울특별시 강동구 길동").build(),
                program("아동수당", "중앙행정기관", "보건복지부", "만 0세~7세 아동", "수당 지급", NOW),
                program("서울형 아이돌봄", "서울특별시", "만 0세~5세 아동"),
                program("강동구 양육 지원", "서울특별시 강동구", "만 0세~5세 아동"));

        RecommendationResult result = service.recommend(CHILD_ID, false);

        assertThat(result.items()).allSatisfy(item -> assertThat(item.score()).isEqualTo(100));
        assertThat(result.items()).extracting(WelfareRecommendationService.RecommendationItem::regionScope)
                .containsExactly(WelfareRegionScope.DISTRICT, WelfareRegionScope.PROVINCE, WelfareRegionScope.NATIONAL);
    }

    @Test
    @DisplayName("같은 지역에 중복 등록된 같은 사업명은 하나만 남긴다")
    void deduplicatesSameNameInSameRegion() {
        given(profile("서울특별시 강동구 길동").build(),
                program("서울형 아이돌봄", "서울특별시", "만 0세~5세 아동"),
                program("서울형 아이돌봄", "서울특별시", "만 0세~5세 아동"));

        assertThat(service.recommend(CHILD_ID, false).items()).hasSize(1);
    }

    @Test
    @DisplayName("아이·가구 요약을 함께 돌려줘 설명과 화면이 같은 기준을 쓴다")
    void returnsChildContext() {
        given(profile("서울특별시 강동구 길동").income("BELOW_100").build(),
                program("강동구 양육 지원", "서울특별시 강동구", "만 0세~5세 아동"));

        WelfareRecommendationService.ChildContext child = service.recommend(CHILD_ID, false).child();

        assertThat(child.childName()).isEqualTo("유하");
        assertThat(child.ageMonths()).isEqualTo(30);
        assertThat(child.ageLabel()).isEqualTo("만 2세");
        assertThat(child.regionLabel()).isEqualTo("서울특별시 강동구");
        assertThat(child.incomeBandLabel()).isEqualTo("기준중위소득 100% 이하");
        assertThat(child.profileSaved()).isTrue();
    }

    @Test
    @DisplayName("선제 추천은 저장된 가족정보가 있을 때만 수행한다")
    void requiresProfileForProactiveRecommendation() {
        given(null, program("강동구 양육 지원", "서울특별시 강동구", "만 0세~5세 아동"));

        RecommendationResult result = service.recommend(CHILD_ID, true);

        assertThat(result.profileRequired()).isTrue();
        assertThat(result.items()).isEmpty();
    }

    @Test
    @DisplayName("가족정보가 없어도 추천은 하지만 조건은 확인 필요로 남긴다")
    void recommendsWithoutProfile() {
        given(null, program("강동구 양육 지원", "서울특별시 강동구", "만 0세~5세 아동"));

        RecommendationResult result = service.recommend(CHILD_ID, false);

        assertThat(result.items()).hasSize(1);
        assertThat(result.items().get(0).missingConditions()).isNotEmpty();
        assertThat(result.child().profileSaved()).isFalse();
    }

    private void given(ChildWelfareProfile profile, WelfareProgram... catalog) {
        Patient child = new Patient();
        child.setChildName("유하");
        child.setBirthDate(LocalDate.of(2024, 5, 6));
        when(patients.findById(any())).thenReturn(Optional.of(child));
        when(profiles.findByPatientId(any())).thenReturn(Optional.ofNullable(profile));
        when(programs.findByActiveTrueAndChildCandidateTrueOrderByModifiedAtSourceDescIdDesc())
                .thenReturn(List.of(catalog));
    }
}

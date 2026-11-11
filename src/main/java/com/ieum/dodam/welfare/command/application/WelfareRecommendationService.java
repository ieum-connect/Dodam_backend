package com.ieum.dodam.welfare.command.application;

import com.ieum.dodam.patient.domain.Patient;
import com.ieum.dodam.patient.domain.repository.PatientRepository;
import com.ieum.dodam.welfare.domain.WelfareIncomeBand;
import com.ieum.dodam.welfare.domain.WelfareRegion;
import com.ieum.dodam.welfare.domain.WelfareRegionScope;
import com.ieum.dodam.welfare.domain.entity.ChildWelfareProfile;
import com.ieum.dodam.welfare.domain.entity.WelfareProgram;
import com.ieum.dodam.welfare.domain.repository.ChildWelfareProfileRepository;
import com.ieum.dodam.welfare.domain.repository.WelfareProgramRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.Period;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * 아이·가구 조건에 맞는 복지서비스를 골라 일치율 순으로 정렬한다.
 *
 * <p>추천 규칙의 우선순위는 다음과 같다.
 * <ol>
 *   <li>거주지역을 벗어난 사업, 대상 연령을 벗어난 사업, 선택하지 않은 가구 요건을 전제로 하는
 *       사업은 {@link WelfareEligibilityEvaluator}에서 제외한다.</li>
 *   <li>남은 사업은 조건 일치율(%)이 높은 순으로 정렬한다.</li>
 *   <li>일치율이 같으면 더 좁은 지역(시·군·구 &gt; 시·도 &gt; 전국) 사업을 먼저 보여준다.</li>
 *   <li>그 다음으로 원본 최신 수정일이 최근인 사업을 먼저 보여준다.</li>
 * </ol>
 */
@Service
public class WelfareRecommendationService {
    private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");
    /** 추천 목록에 노출하는 최소 일치율. */
    private static final int MINIMUM_SCORE = 30;
    private static final int MAX_ITEMS = 50;
    /** 만 18세 미만까지 아동 대상 사업을 평가한다. */
    private static final int MAX_AGE_MONTHS = 215;

    private final PatientRepository patients;
    private final ChildWelfareProfileRepository profiles;
    private final WelfareProgramRepository programs;
    private final WelfareEligibilityEvaluator evaluator;
    private final Clock clock;

    // 생성자가 둘이므로 Spring이 주입에 쓸 생성자를 명시한다(다른 하나는 테스트용 Clock 주입).
    @Autowired
    public WelfareRecommendationService(PatientRepository patients, ChildWelfareProfileRepository profiles,
                                        WelfareProgramRepository programs, WelfareEligibilityEvaluator evaluator) {
        this(patients, profiles, programs, evaluator, Clock.systemUTC());
    }

    WelfareRecommendationService(PatientRepository patients, ChildWelfareProfileRepository profiles,
                                 WelfareProgramRepository programs, WelfareEligibilityEvaluator evaluator, Clock clock) {
        this.patients = patients;
        this.profiles = profiles;
        this.programs = programs;
        this.evaluator = evaluator;
        this.clock = clock;
    }

    public RecommendationResult recommend(UUID patientId, boolean proactive) {
        Patient child = patients.findById(patientId)
                .orElseThrow(() -> new IllegalArgumentException("아이를 찾을 수 없습니다."));
        ChildWelfareProfile profile = profiles.findByPatientId(patientId)
                .filter(ChildWelfareProfile::isStorageConsent).orElse(null);
        if (proactive && profile == null) return RecommendationResult.needsProfile();

        int months = ageInMonths(child.getBirthDate());
        ChildContext context = ChildContext.of(child, profile, months);
        if (months < 0 || months > MAX_AGE_MONTHS) return new RecommendationResult(false, context, List.of(), 0);

        List<Candidate> candidates = new ArrayList<>();
        int excluded = 0;
        for (WelfareProgram program : programs.findByActiveTrueAndChildCandidateTrueOrderByModifiedAtSourceDescIdDesc()) {
            WelfareEligibilityEvaluator.Evaluation evaluation = evaluator.evaluate(program, profile, months);
            if (!evaluation.recommendable() || evaluation.score() < MINIMUM_SCORE) {
                excluded++;
                continue;
            }
            candidates.add(new Candidate(program, evaluation));
        }

        // 같은 사업명이 같은 지역에 중복 등록된 경우 더 높은 일치율 하나만 남긴다.
        LinkedHashMap<String, Candidate> deduplicated = new LinkedHashMap<>();
        candidates.stream().sorted(ordering()).forEach(candidate -> deduplicated.putIfAbsent(candidate.dedupeKey(), candidate));

        List<RecommendationItem> items = deduplicated.values().stream()
                .sorted(ordering())
                .limit(MAX_ITEMS)
                .map(Candidate::toItem)
                .toList();
        return new RecommendationResult(false, context, items, excluded);
    }

    private Comparator<Candidate> ordering() {
        Comparator<Candidate> byScore = Comparator.comparingInt(candidate -> candidate.evaluation().score());
        Comparator<Candidate> bySpecificity = Comparator.comparingInt(candidate -> candidate.region().scope().specificity());
        Comparator<Candidate> byModifiedAt = Comparator.comparing(
                Candidate::modifiedAtSource, Comparator.<Instant>nullsLast(Comparator.<Instant>reverseOrder()));
        Comparator<Candidate> byName = Comparator.comparing(
                candidate -> candidate.program().getName(), Comparator.<String>nullsLast(Comparator.<String>naturalOrder()));
        return byScore.reversed().thenComparing(bySpecificity.reversed()).thenComparing(byModifiedAt).thenComparing(byName);
    }

    private int ageInMonths(LocalDate birthDate) {
        if (birthDate == null) return -1;
        Period age = Period.between(birthDate, LocalDate.now(clock.withZone(SEOUL)));
        if (age.isNegative()) return -1;
        return age.getYears() * 12 + age.getMonths();
    }

    private record Candidate(WelfareProgram program, WelfareEligibilityEvaluator.Evaluation evaluation) {
        WelfareRegion region() { return program.region(); }

        Instant modifiedAtSource() { return program.getModifiedAtSource(); }

        String dedupeKey() {
            String name = program.getName() == null ? "" : program.getName().toLowerCase(Locale.KOREAN).replaceAll("[^가-힣a-z0-9]", "");
            return name + "|" + region().label();
        }

        RecommendationItem toItem() {
            return new RecommendationItem(program.getId(), program.getExternalServiceId(), program.getName(),
                    program.getPurposeSummary(), program.getAgencyName(), program.getApplicationDeadline(),
                    evaluation.score(), evaluation.status(), region().scope(), region().label(),
                    evaluation.matchedMessages(), evaluation.missingMessages(),
                    evaluation.conditions().stream().map(ConditionView::from).toList());
        }
    }

    /** 추천 결과 전체. {@code excludedCount}는 제외 규칙으로 걸러낸 사업 수다. */
    public record RecommendationResult(boolean profileRequired, ChildContext child, List<RecommendationItem> items, int excludedCount) {
        static RecommendationResult needsProfile() {
            return new RecommendationResult(true, null, List.of(), 0);
        }
    }

    /** AI 설명과 화면 안내에 쓰는 아이·가구 요약. */
    public record ChildContext(String childName, Integer ageMonths, String ageLabel, String regionLabel,
                               String incomeBandLabel, Integer householdSize, Integer childCount,
                               Boolean dualIncome, Boolean singleParent, Boolean multicultural, Boolean disability,
                               String childcareStatus, boolean profileSaved) {
        static ChildContext of(Patient child, ChildWelfareProfile profile, int months) {
            WelfareRegion region = profile == null ? null : profile.region();
            String regionLabel = region == null || !region.hasProvince() ? null : region.label();
            String incomeLabel = profile == null ? null : WelfareIncomeBand.from(profile.getIncomeBand())
                    .map(WelfareIncomeBand::label).orElse(null);
            return new ChildContext(child.getChildName(), months < 0 ? null : months, ageLabel(months), regionLabel, incomeLabel,
                    profile == null ? null : profile.getHouseholdSize(),
                    profile == null ? null : profile.getChildCount(),
                    profile == null ? null : profile.getDualIncome(),
                    profile == null ? null : profile.getSingleParent(),
                    profile == null ? null : profile.getMulticultural(),
                    profile == null ? null : profile.getDisability(),
                    profile == null ? null : profile.getChildcareStatus(),
                    profile != null);
        }

        private static String ageLabel(int months) {
            if (months < 0) return null;
            return months < 24 ? "생후 " + months + "개월" : "만 " + months / 12 + "세";
        }
    }

    public record RecommendationItem(UUID programId, String externalServiceId, String name, String summary,
                                     String agencyName, String applicationDeadline, int score, String status,
                                     WelfareRegionScope regionScope, String regionLabel,
                                     List<String> matchedConditions, List<String> missingConditions,
                                     List<ConditionView> conditions) {}

    /** 조건별 판정 상세. 프론트엔드와 AI 설명이 같은 근거를 공유한다. */
    public record ConditionView(String key, String label, String status, double weight, String message) {
        static ConditionView from(WelfareEligibilityEvaluator.Condition condition) {
            return new ConditionView(condition.key(), condition.label(), condition.status().name(),
                    condition.weight(), condition.message());
        }
    }
}

package com.ieum.dodam.welfare.command.application;

import com.ieum.dodam.patient.domain.Patient;
import com.ieum.dodam.patient.domain.repository.PatientRepository;
import com.ieum.dodam.welfare.domain.entity.ChildWelfareProfile;
import com.ieum.dodam.welfare.domain.entity.WelfareProgram;
import com.ieum.dodam.welfare.domain.repository.ChildWelfareProfileRepository;
import com.ieum.dodam.welfare.domain.repository.WelfareProgramRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDate;
import java.time.Period;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class WelfareRecommendationService {
    private static final Pattern INCOME_PERCENT = Pattern.compile("(?:중위소득|기준중위소득)[^0-9]{0,12}(\\d{2,3})\\s*%");
    private final PatientRepository patients;
    private final ChildWelfareProfileRepository profiles;
    private final WelfareProgramRepository programs;
    private final Clock clock;

    @Autowired
    public WelfareRecommendationService(PatientRepository patients, ChildWelfareProfileRepository profiles, WelfareProgramRepository programs) {
        this(patients, profiles, programs, Clock.systemUTC());
    }

    WelfareRecommendationService(PatientRepository patients, ChildWelfareProfileRepository profiles, WelfareProgramRepository programs, Clock clock) {
        this.patients = patients;
        this.profiles = profiles;
        this.programs = programs;
        this.clock = clock;
    }

    public RecommendationResult recommend(UUID patientId, boolean proactive) {
        Patient child = patients.findById(patientId).orElseThrow(() -> new IllegalArgumentException("아이를 찾을 수 없습니다."));
        ChildWelfareProfile profile = profiles.findByPatientId(patientId).filter(ChildWelfareProfile::isStorageConsent).orElse(null);
        if (proactive && profile == null) return new RecommendationResult(true, List.of());
        int months = ageInMonths(child.getBirthDate());
        if (months < 0 || months > 95) return new RecommendationResult(false, List.of());

        LinkedHashMap<String, WelfareProgram> uniquePrograms = new LinkedHashMap<>();
        programs.findByActiveTrueAndChildCandidateTrueOrderByModifiedAtSourceDescIdDesc()
                .forEach(program -> uniquePrograms.putIfAbsent(canonicalName(program.getName()), program));
        List<RecommendationItem> items = uniquePrograms.values().stream()
                .map(program -> RecommendationItem.of(program, evaluate(program, profile, months)))
                .filter(item -> item.score() >= 35)
                .sorted((left, right) -> Double.compare(right.score(), left.score()))
                .limit(50)
                .toList();
        return new RecommendationResult(false, items);
    }

    private int ageInMonths(LocalDate birthDate) {
        if (birthDate == null) return -1;
        Period age = Period.between(birthDate, LocalDate.now(clock));
        return age.getYears() * 12 + age.getMonths();
    }

    private ScoreResult evaluate(WelfareProgram program, ChildWelfareProfile profile, int months) {
        String text = normalizedText(program);
        double score = 30;
        List<String> matched = new ArrayList<>();
        List<String> missing = new ArrayList<>();

        if (ageMatches(text, months)) {
            score += 20;
            matched.add("아이 연령과 지원대상이 일치해요");
        } else if (containsAny(text, "영아", "유아", "아동", "보육", "자녀")) {
            missing.add("세부 연령 조건 확인이 필요해요");
        }

        if (profile == null) {
            missing.add("가족정보를 저장하면 지역·소득·가구 조건을 반영할 수 있어요");
            return new ScoreResult(Math.min(score, 100), matched, missing);
        }

        String region = lower(profile.getRegionCode());
        if (!region.isBlank()) {
            List<String> tokens = regionTokens(region);
            if (tokens.stream().anyMatch(text::contains)) {
                score += 12;
                matched.add("거주지역 조건이 일치해요");
            } else if (containsAny(text, "전국", "소득", "가구", "아동", "자녀")) {
                score += 4;
                matched.add("전국 단위 서비스로 확인돼요");
            }
        }

        Integer income = incomeMaximum(profile.getIncomeBand());
        Integer programIncome = programIncomeLimit(text);
        if (income != null && programIncome != null) {
            if (income <= programIncome) {
                score += 14;
                matched.add("입력한 소득구간이 지원기준 범위에 들어요");
            } else {
                score -= 18;
                missing.add("소득기준을 초과할 가능성이 있어요");
            }
        } else if (programIncome != null) {
            missing.add("소득구간 확인이 필요해요");
        }

        score += flagScore(text, profile.getDualIncome(), matched, "맞벌이", "맞벌이 조건이 일치해요");
        score += flagScore(text, profile.getSingleParent(), matched, "한부모", "한부모 조건이 일치해요");
        score += flagScore(text, profile.getMulticultural(), matched, "다문화", "다문화가정 조건이 일치해요");
        score += flagScore(text, profile.getDisability(), matched, "장애", "장애 관련 조건이 일치해요");

        if (profile.getChildCount() != null && profile.getChildCount() >= 2 && containsAny(text, "다자녀", "2자녀", "두 자녀", "자녀 2")) {
            score += 10;
            matched.add("자녀 수 조건이 일치해요");
        }
        String childcare = lower(profile.getChildcareStatus());
        if (!childcare.isBlank() && text.contains(childcare)) {
            score += 8;
            matched.add("현재 양육 형태와 관련된 서비스예요");
        }
        if (program.getModifiedAtSource() != null) score += 2;
        if (matched.isEmpty()) missing.add("신청기관에서 세부 자격을 확인해 주세요");
        return new ScoreResult(Math.max(0, Math.min(Math.round(score), 100)), matched, missing);
    }

    private double flagScore(String text, Boolean enabled, List<String> matched, String keyword, String message) {
        if (Boolean.TRUE.equals(enabled) && text.contains(keyword)) {
            matched.add(message);
            return 10;
        }
        return 0;
    }

    private boolean ageMatches(String text, int months) {
        if (months <= 35 && containsAny(text, "영아", "0세", "1세", "2세", "36개월", "보육")) return true;
        if (months >= 36 && months <= 83 && containsAny(text, "유아", "3세", "4세", "5세", "6세", "미취학", "보육")) return true;
        return containsAny(text, "아동", "자녀", "만 18세 미만", "만18세 미만");
    }

    private List<String> regionTokens(String region) {
        return List.of(region.split("\\s+")).stream().filter(token -> token.length() >= 2).toList();
    }

    private Integer incomeMaximum(String value) {
        if (value == null || value.isBlank() || value.equals("ABOVE_150")) return null;
        Matcher matcher = Pattern.compile("(\\d{2,3})").matcher(value);
        return matcher.find() ? Integer.parseInt(matcher.group(1)) : null;
    }

    private Integer programIncomeLimit(String text) {
        Matcher matcher = INCOME_PERCENT.matcher(text);
        if (!matcher.find()) return null;
        int value = Integer.parseInt(matcher.group(1));
        return value >= 20 && value <= 300 ? value : null;
    }

    private String normalizedText(WelfareProgram program) {
        return lower(String.join(" ", safe(program.getName()), safe(program.getPurposeSummary()), safe(program.getPurposeDetail()), safe(program.getTargetText()), safe(program.getSelectionCriteriaText()), safe(program.getBenefitText()), safe(program.getAgencyName())));
    }

    private boolean containsAny(String text, String... keywords) {
        for (String keyword : keywords) if (text.contains(keyword)) return true;
        return false;
    }

    private String safe(String value) { return value == null ? "" : value; }
    private String lower(String value) { return safe(value).toLowerCase(Locale.KOREAN); }
    private String canonicalName(String value) { return lower(value).replaceAll("[^가-힣a-z0-9]", ""); }

    public record RecommendationResult(boolean profileRequired, List<RecommendationItem> items) {}
    public record RecommendationItem(UUID programId, String name, double score, String status, List<String> matchedConditions, List<String> missingConditions) {
        static RecommendationItem of(WelfareProgram program, ScoreResult result) {
            String status = result.score() >= 70 ? "ELIGIBLE" : "POSSIBLE";
            return new RecommendationItem(program.getId(), program.getName(), result.score(), status, result.matched(), result.missing());
        }
    }
    private record ScoreResult(double score, List<String> matched, List<String> missing) {}
}

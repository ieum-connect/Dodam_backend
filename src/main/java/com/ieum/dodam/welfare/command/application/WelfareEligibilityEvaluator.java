package com.ieum.dodam.welfare.command.application;

import com.ieum.dodam.welfare.domain.WelfareAgeRange;
import com.ieum.dodam.welfare.domain.WelfareIncomeBand;
import com.ieum.dodam.welfare.domain.WelfareRegion;
import com.ieum.dodam.welfare.domain.entity.ChildWelfareProfile;
import com.ieum.dodam.welfare.domain.entity.WelfareProgram;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 하나의 복지서비스가 아이·가구 조건에 맞는지 판정한다.
 *
 * <p>판정은 두 단계로 나뉜다.
 * <ol>
 *   <li><b>제외 규칙</b> — 거주지역 밖의 사업, 대상 연령을 벗어난 사업, 사용자가 선택하지 않은
 *       가구 요건(장애·한부모·다문화·다자녀 등)을 전제로 하는 사업은 추천 목록에서 제거한다.
 *       점수로 깎지 않고 아예 제외한다.</li>
 *   <li><b>일치율 산정</b> — 남은 사업에 대해 "이 사업이 요구하는 조건" 가운데 충족한 비중을
 *       백분율로 계산한다. 확인할 수 없는 조건은 부분 점수만 준다.</li>
 * </ol>
 */
@Component
public class WelfareEligibilityEvaluator {
    /** 확인 불가 조건에 주는 부분 가중치. */
    private static final double UNKNOWN_CREDIT = 0.4;
    private static final double WEIGHT_REGION = 30;
    private static final double WEIGHT_AGE = 25;
    private static final double WEIGHT_INCOME = 18;
    private static final double WEIGHT_HOUSEHOLD = 12;
    private static final double WEIGHT_CHILDCARE = 10;
    private static final double WEIGHT_DUAL_INCOME = 8;
    private static final int ELIGIBLE_SCORE = 75;

    private static final Pattern INCOME_PERCENT = Pattern.compile("(?:기준\\s*)?중위소득[^0-9%]{0,20}?(\\d{2,3})\\s*%");
    private static final List<String> MEANS_TESTED = List.of("기초생활", "수급자", "수급권자", "차상위", "생계급여", "의료급여", "주거급여", "교육급여");

    /** 프로필 항목과 1:1로 대응하는 가구 요건. */
    private static final List<HouseholdGate> HOUSEHOLD_GATES = List.of(
            new HouseholdGate("DISABILITY", "장애 요건",
                    List.of("장애아", "장애인", "장애아동", "장애가 있는", "장애를 가진", "중증장애", "발달장애",
                            "장애등급", "장애정도", "장애자녀", "특수교육대상자", "특수교육 대상"),
                    List.of("장애", "특수교육"), "장애·특수교육대상 아동을 대상으로 하는 사업이에요"),
            new HouseholdGate("SINGLE_PARENT", "한부모 요건",
                    List.of("한부모", "모자가정", "부자가정", "조손가정", "미혼모", "미혼부"),
                    List.of("한부모", "모자가정", "부자가정"), "한부모·조손 가구를 대상으로 하는 사업이에요"),
            new HouseholdGate("MULTICULTURAL", "다문화 요건",
                    List.of("다문화", "결혼이민", "중도입국", "국제결혼"),
                    List.of("다문화", "결혼이민"), "다문화·결혼이민 가구를 대상으로 하는 사업이에요")
    );

    /** 프로필로 확인할 수 없는 특수 자격. 서비스명에 드러난 경우에만 제외한다. */
    private static final List<String> UNVERIFIABLE_STATUS = List.of("북한이탈", "탈북", "입양", "가정위탁", "위탁아동", "소년소녀가정", "난민", "보호대상아동", "자립준비");

    /** 양육 형태 전제 조건. */
    private static final List<CareSetting> CARE_SETTINGS = List.of(
            new CareSetting("어린이집 이용", List.of("어린이집", "보육료", "보육시설")),
            new CareSetting("유치원 이용", List.of("유치원", "누리과정")),
            new CareSetting("가정양육", List.of("가정양육", "양육수당", "가정에서 양육")),
            new CareSetting("학교 재학", List.of("초등학교", "재학", "학교급식"))
    );

    public Evaluation evaluate(WelfareProgram program, ChildWelfareProfile profile, int ageMonths) {
        String gateText = text(program.getName(), program.getTargetText(), program.getSelectionCriteriaText());
        String fullText = text(program.getName(), program.getPurposeSummary(), program.getPurposeDetail(),
                program.getTargetText(), program.getSelectionCriteriaText(), program.getBenefitText());
        String programName = safe(program.getName());
        List<Condition> conditions = new ArrayList<>();

        Condition region = evaluateRegion(program.region(), profile);
        if (region.status() == Status.UNMET) {
            return Evaluation.excluded("거주지역(" + describeRegion(profile) + ") 밖에서만 신청할 수 있는 사업이에요", region);
        }
        conditions.add(region);

        Condition age = evaluateAge(program, ageMonths);
        if (age.status() == Status.UNMET) return Evaluation.excluded(age.message(), age);
        conditions.add(age);

        String unverifiable = UNVERIFIABLE_STATUS.stream().filter(programName::contains).findFirst().orElse(null);
        if (unverifiable != null) {
            return Evaluation.excluded("'" + unverifiable + "' 자격이 필요한 사업이어서 맞춤 추천에서 제외했어요",
                    new Condition("SPECIAL_STATUS", "특수 자격 요건", Status.UNMET, 0, "프로필로 확인할 수 없는 자격 요건이에요"));
        }

        for (HouseholdGate gate : HOUSEHOLD_GATES) {
            if (!gate.required(gateText, programName)) continue;
            Boolean selected = gate.profileValue(profile);
            if (!Boolean.TRUE.equals(selected)) {
                return Evaluation.excluded(gate.exclusionMessage(),
                        new Condition(gate.key(), gate.label(), Status.UNMET, WEIGHT_HOUSEHOLD, gate.exclusionMessage()));
            }
            conditions.add(new Condition(gate.key(), gate.label(), Status.MET, WEIGHT_HOUSEHOLD,
                    "선택한 " + gate.label().replace(" 요건", "") + " 조건과 일치해요"));
        }

        int requiredChildren = requiredChildCount(gateText, programName);
        if (requiredChildren > 0) {
            Integer childCount = profile == null ? null : profile.getChildCount();
            if (childCount == null || childCount < requiredChildren) {
                return Evaluation.excluded("자녀 " + requiredChildren + "명 이상 가구를 대상으로 하는 사업이에요",
                        new Condition("MULTI_CHILD", "다자녀 요건", Status.UNMET, WEIGHT_HOUSEHOLD, "자녀 수 조건을 충족하지 않아요"));
            }
            conditions.add(new Condition("MULTI_CHILD", "다자녀 요건", Status.MET, WEIGHT_HOUSEHOLD,
                    "자녀 " + childCount + "명으로 다자녀 조건을 충족해요"));
        }

        evaluateIncome(fullText, profile).ifPresent(conditions::add);
        evaluateCareSetting(fullText, profile).ifPresent(conditions::add);
        evaluateDualIncome(fullText, profile).ifPresent(conditions::add);

        if (profile == null) {
            conditions.add(new Condition("PROFILE", "가족정보", Status.UNKNOWN, WEIGHT_INCOME,
                    "가족정보를 저장하면 소득·가구 조건까지 반영할 수 있어요"));
        }
        return Evaluation.of(conditions, ELIGIBLE_SCORE);
    }

    private Condition evaluateRegion(WelfareRegion programRegion, ChildWelfareProfile profile) {
        WelfareRegion userRegion = profile == null ? null : profile.region();
        WelfareRegion.Coverage coverage = programRegion.covers(userRegion);
        return switch (coverage) {
            case COVERED -> new Condition("REGION", "거주지역", Status.MET, WEIGHT_REGION,
                    programRegion.scope() == com.ieum.dodam.welfare.domain.WelfareRegionScope.NATIONAL
                            ? "전국에서 신청할 수 있는 사업이에요"
                            : programRegion.label() + " 거주 조건과 일치해요");
            case OUTSIDE -> new Condition("REGION", "거주지역", Status.UNMET, WEIGHT_REGION,
                    programRegion.label() + " 주민만 신청할 수 있어요");
            case UNDETERMINED -> new Condition("REGION", "거주지역", Status.UNKNOWN, WEIGHT_REGION,
                    userRegion == null || !userRegion.hasProvince()
                            ? "거주지역을 입력하면 지역 조건을 확인할 수 있어요"
                            : (programRegion.hasDistrict()
                                    ? programRegion.label() + " 단위 사업이에요. 시·군·구까지 선택하면 더 정확해져요"
                                    : "사업의 신청 가능 지역이 명확하지 않아 확인이 필요해요"));
        };
    }

    private Condition evaluateAge(WelfareProgram program, int ageMonths) {
        Optional<WelfareAgeRange> parsed = WelfareAgeRange.parse(text(program.getTargetText(), program.getSelectionCriteriaText()))
                .or(() -> WelfareAgeRange.parse(text(program.getName(), program.getPurposeSummary())));
        if (parsed.isEmpty()) {
            return new Condition("AGE", "대상 연령", Status.UNKNOWN, WEIGHT_AGE, "지원대상 연령이 명시되어 있지 않아 확인이 필요해요");
        }
        WelfareAgeRange range = parsed.get();
        if (!range.contains(ageMonths)) {
            return new Condition("AGE", "대상 연령", Status.UNMET, WEIGHT_AGE,
                    "지원대상 연령(" + range.describe() + ")에 해당하지 않아요");
        }
        if (range.broad()) {
            return new Condition("AGE", "대상 연령", Status.UNKNOWN, WEIGHT_AGE,
                    "지원대상이 '" + range.describe() + "'로 넓게만 적혀 있어 세부 연령 확인이 필요해요");
        }
        return new Condition("AGE", "대상 연령", Status.MET, WEIGHT_AGE,
                "지원대상 연령(" + range.describe() + ")에 해당해요");
    }

    private Optional<Condition> evaluateIncome(String text, ChildWelfareProfile profile) {
        Integer limit = incomeLimitPercent(text);
        boolean meansTested = MEANS_TESTED.stream().anyMatch(text::contains);
        if (limit == null && !meansTested) return Optional.empty();

        Optional<WelfareIncomeBand> band = profile == null ? Optional.empty() : WelfareIncomeBand.from(profile.getIncomeBand());
        if (band.isEmpty()) {
            return Optional.of(new Condition("INCOME", "소득 기준", Status.UNKNOWN, WEIGHT_INCOME,
                    limit == null ? "기초생활수급·차상위 가구 기준이 있어 소득 확인이 필요해요"
                            : "소득기준(기준중위소득 " + limit + "% 이하)이 있어 소득구간 입력이 필요해요"));
        }
        WelfareIncomeBand selected = band.get();
        // 수급·차상위 전용 사업은 소득구간 하한이 기준을 넘으면 신청 자격이 없다.
        int effectiveLimit = limit == null ? 50 : limit;
        if (selected.lowerExclusivePercent() >= effectiveLimit) {
            return Optional.of(new Condition("INCOME", "소득 기준", Status.UNMET, WEIGHT_INCOME,
                    "선택한 소득구간(" + selected.label() + ")은 지원기준(" + describeLimit(limit, meansTested) + ")을 넘어요"));
        }
        Integer upper = selected.upperInclusivePercent();
        if (upper != null && upper <= effectiveLimit) {
            return Optional.of(new Condition("INCOME", "소득 기준", Status.MET, WEIGHT_INCOME,
                    "선택한 소득구간(" + selected.label() + ")이 지원기준(" + describeLimit(limit, meansTested) + ") 안에 들어요"));
        }
        return Optional.of(new Condition("INCOME", "소득 기준", Status.UNKNOWN, WEIGHT_INCOME,
                "지원기준(" + describeLimit(limit, meansTested) + ")과 선택한 소득구간이 일부만 겹쳐 확인이 필요해요"));
    }

    private Optional<Condition> evaluateCareSetting(String text, ChildWelfareProfile profile) {
        List<CareSetting> mentioned = CARE_SETTINGS.stream().filter(setting -> setting.mentioned(text)).toList();
        if (mentioned.size() != 1) return Optional.empty();
        CareSetting setting = mentioned.get(0);
        String status = profile == null ? null : profile.getChildcareStatus();
        if (status == null || status.isBlank()) {
            return Optional.of(new Condition("CARE_SETTING", "양육 형태", Status.UNKNOWN, WEIGHT_CHILDCARE,
                    setting.label() + " 아동을 전제로 하는 사업이에요. 양육 형태를 입력하면 확인할 수 있어요"));
        }
        if (status.equals(setting.label())) {
            return Optional.of(new Condition("CARE_SETTING", "양육 형태", Status.MET, WEIGHT_CHILDCARE,
                    "현재 양육 형태(" + status + ")와 일치하는 사업이에요"));
        }
        return Optional.of(new Condition("CARE_SETTING", "양육 형태", Status.UNMET, WEIGHT_CHILDCARE,
                setting.label() + " 아동을 전제로 해서 현재 양육 형태(" + status + ")와 달라요"));
    }

    private Optional<Condition> evaluateDualIncome(String text, ChildWelfareProfile profile) {
        if (!text.contains("맞벌이")) return Optional.empty();
        Boolean dualIncome = profile == null ? null : profile.getDualIncome();
        if (dualIncome == null) {
            return Optional.of(new Condition("DUAL_INCOME", "맞벌이 여부", Status.UNKNOWN, WEIGHT_DUAL_INCOME,
                    "맞벌이 가구를 우선 지원하는 사업이에요"));
        }
        return Optional.of(dualIncome
                ? new Condition("DUAL_INCOME", "맞벌이 여부", Status.MET, WEIGHT_DUAL_INCOME, "맞벌이 가구 우선 지원 조건과 일치해요")
                : new Condition("DUAL_INCOME", "맞벌이 여부", Status.UNMET, WEIGHT_DUAL_INCOME, "맞벌이 가구를 우선 지원하는 사업이에요"));
    }

    private int requiredChildCount(String gateText, String programName) {
        if (containsAny(gateText, "셋째", "세 자녀", "3자녀", "자녀 3", "삼자녀")) return 3;
        if (containsAny(gateText, "다자녀", "2자녀", "두 자녀", "자녀 2", "둘 이상의 자녀") || programName.contains("다자녀")) return 2;
        return 0;
    }

    /** 사업이 허용하는 가장 넓은 소득기준을 사용한다(여러 기준이 함께 적힌 문서가 많다). */
    private Integer incomeLimitPercent(String text) {
        Matcher matcher = INCOME_PERCENT.matcher(text);
        Integer limit = null;
        while (matcher.find()) {
            int value = Integer.parseInt(matcher.group(1));
            if (value < 20 || value > 300) continue;
            limit = limit == null ? value : Math.max(limit, value);
        }
        return limit;
    }

    private String describeLimit(Integer limit, boolean meansTested) {
        if (limit != null) return "기준중위소득 " + limit + "% 이하";
        return meansTested ? "기초생활수급·차상위 가구" : "소득기준";
    }

    private String describeRegion(ChildWelfareProfile profile) {
        WelfareRegion region = profile == null ? null : profile.region();
        return region == null || !region.hasProvince() ? "미입력" : region.label();
    }

    private boolean containsAny(String text, String... keywords) {
        for (String keyword : keywords) if (text.contains(keyword)) return true;
        return false;
    }

    private String text(String... values) {
        StringBuilder builder = new StringBuilder();
        for (String value : values) builder.append(safe(value)).append(' ');
        return builder.toString();
    }

    private String safe(String value) { return value == null ? "" : value; }

    /** 가구 요건 하나. {@code strongKeywords}는 지원대상 문구, {@code nameKeywords}는 서비스명에서 찾는다. */
    private record HouseholdGate(String key, String label, List<String> strongKeywords, List<String> nameKeywords, String exclusionMessage) {
        boolean required(String gateText, String programName) {
            return strongKeywords.stream().anyMatch(gateText::contains)
                    || nameKeywords.stream().anyMatch(programName::contains);
        }

        Boolean profileValue(ChildWelfareProfile profile) {
            if (profile == null) return null;
            return switch (key) {
                case "DISABILITY" -> profile.getDisability();
                case "SINGLE_PARENT" -> profile.getSingleParent();
                case "MULTICULTURAL" -> profile.getMulticultural();
                default -> null;
            };
        }
    }

    private record CareSetting(String label, List<String> keywords) {
        boolean mentioned(String text) { return keywords.stream().anyMatch(text::contains); }
    }

    public enum Status { MET, UNMET, UNKNOWN }

    /** 조건 하나의 판정 결과. */
    public record Condition(String key, String label, Status status, double weight, String message) {}

    /**
     * 사업 하나에 대한 판정 결과.
     *
     * @param score 사업이 요구하는 조건 중 충족 비중(%). 확인 불가 조건은 부분 가중치만 반영한다.
     */
    public record Evaluation(boolean recommendable, String exclusionReason, int score, String status, List<Condition> conditions) {
        static Evaluation excluded(String reason, Condition cause) {
            return new Evaluation(false, reason, 0, "EXCLUDED", List.of(cause));
        }

        static Evaluation of(List<Condition> conditions, int eligibleScore) {
            double total = conditions.stream().mapToDouble(Condition::weight).sum();
            double met = conditions.stream().filter(c -> c.status() == Status.MET).mapToDouble(Condition::weight).sum();
            double unknown = conditions.stream().filter(c -> c.status() == Status.UNKNOWN).mapToDouble(Condition::weight).sum();
            int score = total <= 0 ? 0 : (int) Math.round(100 * (met + UNKNOWN_CREDIT * unknown) / total);
            boolean clean = conditions.stream().noneMatch(c -> c.status() == Status.UNMET)
                    && conditions.stream().filter(c -> c.key().equals("REGION") || c.key().equals("AGE"))
                            .allMatch(c -> c.status() == Status.MET);
            return new Evaluation(true, null, score, clean && score >= eligibleScore ? "ELIGIBLE" : "POSSIBLE", List.copyOf(conditions));
        }

        public List<String> matchedMessages() {
            return conditions.stream().filter(c -> c.status() == Status.MET).map(Condition::message).toList();
        }

        public List<String> missingMessages() {
            return conditions.stream().filter(c -> c.status() != Status.MET).map(Condition::message).toList();
        }
    }
}

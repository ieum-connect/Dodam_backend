package com.ieum.dodam.welfare.domain;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 복지서비스 지원대상 문구에서 추출한 대상 연령 범위(개월 단위).
 *
 * <p>문구에는 아이 연령과 보호자 연령이 섞여 있을 수 있어, 추출한 값들의 합집합을 사용하고
 * 하한이 상한보다 큰 모순이 생기면 판정을 포기한다(과도한 제외를 막는다).
 */
public record WelfareAgeRange(int minMonths, int maxMonths) {
    private static final Pattern YEAR_RANGE = Pattern.compile("(?:만\\s*)?(\\d{1,2})\\s*세\\s*(?:~|∼|-|–|부터)\\s*(?:만\\s*)?(\\d{1,2})\\s*세");
    private static final Pattern MONTH_RANGE = Pattern.compile("(\\d{1,3})\\s*개월\\s*(?:~|∼|-|–|부터)\\s*(\\d{1,3})\\s*개월");
    private static final Pattern YEAR_MAX = Pattern.compile("(?:만\\s*)?(\\d{1,2})\\s*세\\s*(이하|미만|까지)");
    private static final Pattern YEAR_MIN = Pattern.compile("(?:만\\s*)?(\\d{1,2})\\s*세\\s*(이상|초과|부터)");
    private static final Pattern MONTH_MAX = Pattern.compile("(\\d{1,3})\\s*개월\\s*(이하|미만|까지)");
    private static final Pattern MONTH_MIN = Pattern.compile("(\\d{1,3})\\s*개월\\s*(이상|초과|부터)");
    private static final int UNBOUNDED = 12 * 100;

    /**
     * 숫자 표현이 없을 때 사용하는 생애주기 키워드.
     *
     * <p>구체 키워드(초등학생 등)와 포괄 키워드(아동·자녀)가 함께 등장하면 구체 키워드만 사용한다.
     * "초등학생 자녀"를 0~17세로 넓게 보면 영유아에게도 추천되기 때문이다.
     */
    private static final Map<String, WelfareAgeRange> SPECIFIC_RANGES = Map.ofEntries(
            Map.entry("영아", new WelfareAgeRange(0, 23)),
            Map.entry("영유아", new WelfareAgeRange(0, 71)),
            Map.entry("유아", new WelfareAgeRange(36, 71)),
            Map.entry("미취학", new WelfareAgeRange(0, 83)),
            Map.entry("취학전", new WelfareAgeRange(0, 83)),
            Map.entry("어린이집", new WelfareAgeRange(0, 71)),
            Map.entry("유치원", new WelfareAgeRange(36, 83)),
            Map.entry("초등학생", new WelfareAgeRange(84, 155)),
            Map.entry("초등학교", new WelfareAgeRange(84, 155)),
            Map.entry("중학생", new WelfareAgeRange(156, 191)),
            Map.entry("고등학생", new WelfareAgeRange(192, 227)),
            Map.entry("신생아", new WelfareAgeRange(0, 2)),
            Map.entry("산모", new WelfareAgeRange(0, 5)),
            Map.entry("임산부", new WelfareAgeRange(0, 11)),
            Map.entry("출산", new WelfareAgeRange(0, 11))
    );

    /** 연령대를 좁히지 못하는 포괄 키워드. 구체 키워드가 없을 때만 사용한다. */
    private static final Map<String, WelfareAgeRange> GENERIC_RANGES = Map.of(
            "아동", new WelfareAgeRange(0, 215),
            "자녀", new WelfareAgeRange(0, 215),
            "청소년", new WelfareAgeRange(108, 287)
    );

    public boolean contains(int months) { return months >= minMonths && months <= maxMonths; }

    /**
     * 연령대를 사실상 좁히지 못한 범위인지 여부. "아동", "자녀"처럼 0~17세를 통째로 가리키는
     * 표현은 연령이 맞았다고 보기 어려워 일치율에 부분 점수만 반영한다.
     */
    public boolean broad() { return maxMonths - minMonths > 120; }

    public boolean unbounded() { return minMonths == 0 && maxMonths >= UNBOUNDED; }

    public String describe() {
        if (unbounded()) return "연령 제한 없음";
        String from = minMonths == 0 ? "0개월" : format(minMonths);
        String to = maxMonths >= UNBOUNDED ? "제한 없음" : format(maxMonths);
        return from + " ~ " + to;
    }

    private static String format(int months) {
        return months < 24 ? months + "개월" : "만 " + months / 12 + "세";
    }

    /** 지원대상 문구에서 연령 범위를 추출한다. 판정 근거가 없으면 빈 값. */
    public static Optional<WelfareAgeRange> parse(String text) {
        if (text == null || text.isBlank()) return Optional.empty();
        List<Integer> mins = new ArrayList<>();
        List<Integer> maxes = new ArrayList<>();

        Matcher yearRange = YEAR_RANGE.matcher(text);
        while (yearRange.find()) {
            mins.add(years(yearRange.group(1)));
            maxes.add(years(yearRange.group(2)) + 11);
        }
        Matcher monthRange = MONTH_RANGE.matcher(text);
        while (monthRange.find()) {
            mins.add(months(monthRange.group(1)));
            maxes.add(months(monthRange.group(2)));
        }
        collect(YEAR_MAX.matcher(text), maxes, true, true);
        collect(YEAR_MIN.matcher(text), mins, true, false);
        collect(MONTH_MAX.matcher(text), maxes, false, true);
        collect(MONTH_MIN.matcher(text), mins, false, false);

        if (mins.isEmpty() && maxes.isEmpty()) return keywordRange(text);
        int min = mins.stream().mapToInt(Integer::intValue).min().orElse(0);
        int max = maxes.stream().mapToInt(Integer::intValue).max().orElse(UNBOUNDED);
        if (min > max) return Optional.empty();
        return Optional.of(new WelfareAgeRange(min, max));
    }

    private static void collect(Matcher matcher, List<Integer> target, boolean yearUnit, boolean upperBound) {
        while (matcher.find()) {
            int value = yearUnit ? years(matcher.group(1)) : months(matcher.group(1));
            String qualifier = matcher.group(2);
            if (upperBound) {
                // "만 5세 이하"는 71개월까지, "만 5세 미만"은 59개월까지를 뜻한다.
                boolean inclusive = !qualifier.equals("미만");
                target.add(yearUnit ? value + (inclusive ? 11 : -1) : value - (inclusive ? 0 : 1));
            } else {
                target.add(qualifier.equals("초과") ? value + 1 : value);
            }
        }
    }

    private static Optional<WelfareAgeRange> keywordRange(String text) {
        return union(text, SPECIFIC_RANGES).or(() -> union(text, GENERIC_RANGES));
    }

    private static Optional<WelfareAgeRange> union(String text, Map<String, WelfareAgeRange> ranges) {
        Integer min = null;
        Integer max = null;
        for (Map.Entry<String, WelfareAgeRange> entry : ranges.entrySet()) {
            if (!text.contains(entry.getKey())) continue;
            min = min == null ? entry.getValue().minMonths() : Math.min(min, entry.getValue().minMonths());
            max = max == null ? entry.getValue().maxMonths() : Math.max(max, entry.getValue().maxMonths());
        }
        return min == null ? Optional.empty() : Optional.of(new WelfareAgeRange(min, max));
    }

    private static int years(String value) { return Math.min(Integer.parseInt(value), 100) * 12; }

    private static int months(String value) { return Math.min(Integer.parseInt(value), UNBOUNDED); }
}

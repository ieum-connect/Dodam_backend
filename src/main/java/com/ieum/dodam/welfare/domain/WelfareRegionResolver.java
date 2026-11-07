package com.ieum.dodam.welfare.domain;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 정부24 소관기관 정보와 사용자가 선택한 행정구역 문자열을 {@link WelfareRegion}으로 정규화한다.
 *
 * <p>소관기관명은 "전라남도 영광군", "서울특별시 강동구", "경기도" 처럼 시·도명이 앞에 오는 형식을
 * 기본으로 하되, 약칭("전남")과 시·도명이 빠진 형식("영광군")도 함께 해석한다.
 */
public final class WelfareRegionResolver {

    /** 정식 시·도명 -> 허용 약칭. 첫 항목이 정규화 결과다. */
    private static final Map<String, List<String>> PROVINCE_ALIASES = new LinkedHashMap<>();
    /** 약칭 -> 정식 명칭. 긴 약칭부터 비교하기 위해 길이 내림차순으로 정렬해 사용한다. */
    private static final List<Map.Entry<String, String>> ALIAS_INDEX;
    /** 시·도명이 생략된 본문에서 지역을 추정할 때 쓰는 약칭(동명 지자체와 충돌하지 않는 것만). */
    private static final List<Map.Entry<String, String>> TEXT_ALIAS_INDEX;
    // "달서구청", "남양주시복지재단"처럼 기관 이름이 이어 붙은 형태까지 시·군·구 토큰으로 인정한다.
    private static final Pattern DISTRICT_TOKEN = Pattern.compile("([가-힣]{2,6}?(?:시|군|구))");
    private static final List<String> CENTRAL_AGENCY_TYPES = List.of("중앙", "국가");
    /** 지자체명으로 오인되기 쉬운 기관 접미사. */
    private static final List<String> NON_REGION_TOKENS = List.of("교육청", "지원청", "출입국", "세무서", "경찰서", "소방서", "공사", "공단", "지사", "본부", "센터");

    static {
        PROVINCE_ALIASES.put("서울특별시", List.of("서울특별시", "서울시", "서울"));
        PROVINCE_ALIASES.put("부산광역시", List.of("부산광역시", "부산시", "부산"));
        PROVINCE_ALIASES.put("대구광역시", List.of("대구광역시", "대구시", "대구"));
        PROVINCE_ALIASES.put("인천광역시", List.of("인천광역시", "인천시", "인천"));
        PROVINCE_ALIASES.put("광주광역시", List.of("광주광역시"));
        PROVINCE_ALIASES.put("대전광역시", List.of("대전광역시", "대전시", "대전"));
        PROVINCE_ALIASES.put("울산광역시", List.of("울산광역시", "울산시", "울산"));
        PROVINCE_ALIASES.put("세종특별자치시", List.of("세종특별자치시", "세종시", "세종"));
        PROVINCE_ALIASES.put("경기도", List.of("경기도", "경기"));
        PROVINCE_ALIASES.put("강원특별자치도", List.of("강원특별자치도", "강원도", "강원"));
        PROVINCE_ALIASES.put("충청북도", List.of("충청북도", "충북"));
        PROVINCE_ALIASES.put("충청남도", List.of("충청남도", "충남"));
        PROVINCE_ALIASES.put("전북특별자치도", List.of("전북특별자치도", "전라북도", "전북"));
        PROVINCE_ALIASES.put("전라남도", List.of("전라남도", "전남"));
        PROVINCE_ALIASES.put("경상북도", List.of("경상북도", "경북"));
        PROVINCE_ALIASES.put("경상남도", List.of("경상남도", "경남"));
        PROVINCE_ALIASES.put("제주특별자치도", List.of("제주특별자치도", "제주도", "제주"));

        List<Map.Entry<String, String>> index = new ArrayList<>();
        PROVINCE_ALIASES.forEach((canonical, aliases) -> aliases.forEach(alias -> index.add(Map.entry(alias, canonical))));
        index.sort(Comparator.comparingInt((Map.Entry<String, String> entry) -> entry.getKey().length()).reversed());
        ALIAS_INDEX = List.copyOf(index);
        // 본문 추정에서는 "광주"(경기도 광주시)처럼 다른 지자체와 겹치는 2글자 약칭을 제외한다.
        TEXT_ALIAS_INDEX = ALIAS_INDEX.stream()
                .filter(entry -> entry.getKey().length() >= 3 || !entry.getKey().equals("광주"))
                .toList();
    }

    /** 법정동 코드 시·도 접두 2자리 -> 정식 시·도명. */
    private static final Map<String, String> PROVINCE_BY_CODE = Map.ofEntries(
            Map.entry("11", "서울특별시"), Map.entry("26", "부산광역시"), Map.entry("27", "대구광역시"),
            Map.entry("28", "인천광역시"), Map.entry("29", "광주광역시"), Map.entry("30", "대전광역시"),
            Map.entry("31", "울산광역시"), Map.entry("36", "세종특별자치시"), Map.entry("41", "경기도"),
            Map.entry("42", "강원특별자치도"), Map.entry("51", "강원특별자치도"), Map.entry("43", "충청북도"),
            Map.entry("44", "충청남도"), Map.entry("45", "전북특별자치도"), Map.entry("52", "전북특별자치도"),
            Map.entry("46", "전라남도"), Map.entry("47", "경상북도"), Map.entry("48", "경상남도"),
            Map.entry("50", "제주특별자치도"));

    private WelfareRegionResolver() {}

    /**
     * 복지서비스의 제공 지역을 판정한다.
     *
     * @param agencyType  소관기관유형(중앙행정기관/광역자치단체/기초자치단체 등)
     * @param agencyName  소관기관명
     * @param fallbackText 소관기관명으로 판정하지 못했을 때 참고할 본문(서비스명·지원대상 등)
     */
    public static WelfareRegion forProgram(String agencyType, String agencyName, String fallbackText) {
        String type = safe(agencyType);
        if (CENTRAL_AGENCY_TYPES.stream().anyMatch(type::contains)) return WelfareRegion.NATIONWIDE;

        WelfareRegion fromAgency = parse(safe(agencyName), true);
        if (fromAgency.scope() != WelfareRegionScope.UNKNOWN) return fromAgency;

        WelfareRegion fromText = parse(safe(fallbackText), false);
        // 본문 추정은 시·도 단위까지만 신뢰한다. 본문에 등장한 시·군·구는 신청 창구 안내일 수 있다.
        if (fromText.hasProvince()) return new WelfareRegion(WelfareRegionScope.PROVINCE, fromText.province(), null);
        // 지역 표기가 전혀 없는 중앙부처·공공기관 사업은 전국 단위로 본다.
        if (!safe(agencyName).isBlank()) return WelfareRegion.NATIONWIDE;
        return WelfareRegion.UNKNOWN;
    }

    /** 사용자가 선택한 행정구역 명칭("서울특별시 강동구 길동")을 정규화한다. */
    public static WelfareRegion forProfile(String regionName) {
        WelfareRegion parsed = parse(safe(regionName), true);
        if (!parsed.hasProvince()) return WelfareRegion.UNKNOWN;
        return parsed;
    }

    /**
     * 법정동 코드 앞 2자리로 시·도를 판정한다. 명칭을 함께 저장하지 못한 프로필에서도
     * 최소한 시·도 단위 필터는 동작하게 하기 위한 보조 경로다. 코드가 아니면 미확인.
     *
     * <p>강원·전북은 특별자치도 전환 전후 코드(42/51, 45/52)를 모두 인정한다.
     */
    public static WelfareRegion forProfileCode(String regionCode) {
        String value = safe(regionCode);
        if (!value.matches("\\d{10}")) return WelfareRegion.UNKNOWN;
        String province = PROVINCE_BY_CODE.get(value.substring(0, 2));
        return province == null ? WelfareRegion.UNKNOWN : new WelfareRegion(WelfareRegionScope.PROVINCE, province, null);
    }

    /** "재단법인화성시복지재단"처럼 기관 형태가 앞에 붙은 이름에서 지역명만 남긴다. */
    private static final List<String> ORG_PREFIXES = List.of("재단법인", "사단법인", "주식회사", "(재)", "(사)", "의료법인", "사회복지법인");

    private static WelfareRegion parse(String value, boolean allowDistrict) {
        if (value.isBlank()) return WelfareRegion.UNKNOWN;
        for (String prefix : ORG_PREFIXES) {
            if (value.startsWith(prefix)) { value = value.substring(prefix.length()).trim(); break; }
        }
        String province = null;
        String remainder = value;
        for (Map.Entry<String, String> entry : allowDistrict ? ALIAS_INDEX : TEXT_ALIAS_INDEX) {
            int index = value.indexOf(entry.getKey());
            if (index < 0) continue;
            // 약칭은 문자열 선두 또는 공백 뒤에서만 인정한다("경기도 광주시"가 광주광역시로 잡히지 않도록).
            if (index > 0 && !Character.isWhitespace(value.charAt(index - 1))) continue;
            province = entry.getValue();
            remainder = value.substring(index + entry.getKey().length());
            break;
        }
        List<String> districts = allowDistrict ? districtTokens(remainder) : List.of();
        if (province == null && districts.isEmpty()) return WelfareRegion.UNKNOWN;
        if (districts.isEmpty()) return new WelfareRegion(WelfareRegionScope.PROVINCE, province, null);
        return new WelfareRegion(WelfareRegionScope.DISTRICT, province, String.join(" ", districts));
    }

    private static List<String> districtTokens(String value) {
        List<String> tokens = new ArrayList<>();
        Matcher matcher = DISTRICT_TOKEN.matcher(value);
        while (matcher.find()) {
            String token = matcher.group(1);
            if (token.length() < 2) continue;
            if (NON_REGION_TOKENS.stream().anyMatch(token::contains)) continue;
            if (!tokens.contains(token)) tokens.add(token);
        }
        return tokens;
    }

    private static String safe(String value) { return value == null ? "" : value.trim(); }
}

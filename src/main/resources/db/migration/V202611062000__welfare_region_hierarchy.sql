-- 2026-11-06 복지 추천 지역 계층 정규화
-- 소관기관 문자열을 매번 검색하던 방식을 버리고, 사업의 제공 지역을
-- (전국 / 시·도 / 시·군·구) 범위로 정규화해 추천 단계에서 하드 필터로 사용한다.

ALTER TABLE welfare_programs
    ADD COLUMN region_scope VARCHAR(20) NOT NULL DEFAULT 'UNKNOWN',
    ADD COLUMN region_province VARCHAR(40),
    ADD COLUMN region_district VARCHAR(60),
    ADD CONSTRAINT welfare_programs_region_scope_check
        CHECK (region_scope IN ('NATIONAL', 'PROVINCE', 'DISTRICT', 'UNKNOWN'));

-- 기존 행은 UNKNOWN으로 두고, 애플리케이션 기동 시 소관기관 정보로 재판정한다.
CREATE INDEX welfare_programs_region_idx
    ON welfare_programs (active, child_candidate, region_scope, region_province)
    WHERE active = TRUE AND child_candidate = TRUE;

ALTER TABLE child_welfare_profiles
    ADD COLUMN region_name VARCHAR(120),
    ADD COLUMN region_province VARCHAR(40),
    ADD COLUMN region_district VARCHAR(60);

-- 과거에는 region_code 컬럼에 행정구역 명칭을 저장했다. 코드 형식이 아닌 값은 명칭으로 옮긴다.
UPDATE child_welfare_profiles
SET region_name = region_code
WHERE region_code IS NOT NULL AND region_code <> '' AND region_code !~ '^[0-9]{10}$';

COMMENT ON COLUMN welfare_programs.region_scope IS '사업 제공 범위: NATIONAL/PROVINCE/DISTRICT/UNKNOWN';
COMMENT ON COLUMN welfare_programs.region_province IS '정규화한 시·도명';
COMMENT ON COLUMN welfare_programs.region_district IS '시·군·구 토큰(공백 구분, 예: 수원시 장안구)';
COMMENT ON COLUMN child_welfare_profiles.region_name IS '사용자가 선택한 행정구역 전체 명칭';

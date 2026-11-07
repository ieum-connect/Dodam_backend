-- 2026-11-06 복지 추천 운영 보강
-- 기존 복지 테이블을 유지하면서 프로필 검증과 조회 경로를 보강한다.

ALTER TABLE child_welfare_profiles
    ADD CONSTRAINT child_welfare_profiles_household_size_check
        CHECK (household_size IS NULL OR household_size BETWEEN 1 AND 30),
    ADD CONSTRAINT child_welfare_profiles_child_count_check
        CHECK (child_count IS NULL OR child_count BETWEEN 1 AND 30);

CREATE INDEX child_welfare_profiles_consent_idx
    ON child_welfare_profiles (storage_consent, updated_at DESC)
    WHERE storage_consent = TRUE;

CREATE INDEX welfare_programs_child_search_idx
    ON welfare_programs (active, child_candidate, service_category, modified_at_source DESC)
    WHERE active = TRUE AND child_candidate = TRUE;

CREATE INDEX welfare_recommendation_items_program_idx
    ON welfare_recommendation_items (program_id, created_at DESC);

COMMENT ON TABLE welfare_programs IS '정부24 공공서비스 원문과 영유아 후보 복지 프로그램';
COMMENT ON TABLE child_welfare_profiles IS '보호자 동의 기반 아이별 복지 추천 프로필';

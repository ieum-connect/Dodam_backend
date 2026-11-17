-- 정규화 전용 테이블에 모든 사업의 원문과 지역 행이 존재하는지 확인한 뒤 구 컬럼을 제거한다.
DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM welfare_programs p
        LEFT JOIN welfare_program_raw r ON r.program_id = p.id
        WHERE r.program_id IS NULL
    ) THEN
        RAISE EXCEPTION 'welfare_program_raw 이관이 끝나지 않아 구 컬럼을 제거할 수 없습니다';
    END IF;

    IF EXISTS (
        SELECT 1 FROM welfare_programs p
        LEFT JOIN welfare_program_regions r ON r.program_id = p.id
        WHERE r.program_id IS NULL
    ) THEN
        RAISE EXCEPTION 'welfare_program_regions 이관이 끝나지 않아 구 컬럼을 제거할 수 없습니다';
    END IF;
END $$;

DROP INDEX IF EXISTS welfare_programs_region_idx;

ALTER TABLE welfare_programs
    DROP CONSTRAINT IF EXISTS welfare_programs_region_scope_check,
    DROP COLUMN raw_list_json,
    DROP COLUMN raw_detail_json,
    DROP COLUMN raw_condition_json,
    DROP COLUMN region_scope,
    DROP COLUMN region_province,
    DROP COLUMN region_district;

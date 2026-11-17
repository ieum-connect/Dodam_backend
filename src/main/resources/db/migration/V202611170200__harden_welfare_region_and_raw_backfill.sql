-- 시·도 없이 시·군·구만 저장된 값은 동명 지역을 다른 시·도로 잘못 추천할 수 있다.
-- 안전하게 UNKNOWN으로 낮추고 이후 동기화에서 소관기관 원문으로 다시 판정한다.
DELETE FROM welfare_program_regions invalid
WHERE invalid.region_scope = 'DISTRICT'
  AND (invalid.region_province IS NULL OR invalid.region_district IS NULL)
  AND EXISTS (
      SELECT 1
      FROM welfare_program_regions unknown_region
      WHERE unknown_region.program_id = invalid.program_id
        AND unknown_region.region_scope = 'UNKNOWN'
  );

UPDATE welfare_program_regions
SET region_scope = 'UNKNOWN', region_province = NULL, region_district = NULL
WHERE region_scope = 'DISTRICT'
  AND (region_province IS NULL OR region_district IS NULL);

UPDATE welfare_programs
SET region_scope = 'UNKNOWN', region_province = NULL, region_district = NULL
WHERE region_scope = 'DISTRICT'
  AND (region_province IS NULL OR region_district IS NULL);

ALTER TABLE welfare_program_regions
    DROP CONSTRAINT welfare_program_regions_shape_check;

ALTER TABLE welfare_program_regions
    ADD CONSTRAINT welfare_program_regions_shape_check CHECK (
        (region_scope IN ('NATIONAL', 'UNKNOWN') AND region_province IS NULL AND region_district IS NULL)
        OR (region_scope = 'PROVINCE' AND region_province IS NOT NULL AND region_district IS NULL)
        OR (region_scope = 'DISTRICT' AND region_province IS NOT NULL AND region_district IS NOT NULL)
    );

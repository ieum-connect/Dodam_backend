ALTER TABLE welfare_program_raw
    ADD COLUMN enrichment_pending BOOLEAN NOT NULL DEFAULT FALSE;

UPDATE welfare_program_raw
SET enrichment_pending = TRUE
WHERE detail_json = '{}'::jsonb
   OR condition_json = '{}'::jsonb;

COMMENT ON COLUMN welfare_program_raw.enrichment_pending IS
    '목록 변경 후 상세·지원조건 재수집이 아직 완료되지 않았는지 표시한다';

-- 2026-11-17 복지 카탈로그 정규화 1단계
--
-- (1) 정부24 응답 원문(raw_list_json/raw_detail_json/raw_condition_json)을 welfare_program_raw로 분리한다.
--     원문은 재처리의 기준이 되는 불변 데이터이고 목록 조회에는 쓰이지 않으므로, 본문 테이블에서
--     떼어내 welfare_programs의 행 크기를 줄인다.
-- (2) 사업의 제공 지역을 welfare_program_regions로 분리한다. 기존 단일 컬럼 구조에서는 한 사업이
--     여러 지역(예: 시·도 + 특정 시·군·구)을 제공하는 경우를 표현할 수 없었다.
--
-- welfare_programs의 raw_*_json과 region_* 컬럼은 병행 운용을 위해 그대로 둔다.
-- 애플리케이션이 새 테이블로 완전히 전환된 뒤 별도 마이그레이션에서 제거한다.

CREATE TABLE welfare_program_raw (
    program_id UUID PRIMARY KEY REFERENCES welfare_programs(id) ON DELETE CASCADE,
    list_json JSONB NOT NULL DEFAULT '{}'::jsonb,
    detail_json JSONB NOT NULL DEFAULT '{}'::jsonb,
    condition_json JSONB NOT NULL DEFAULT '{}'::jsonb,
    payload_hash VARCHAR(64),
    fetched_at TIMESTAMPTZ(6) NOT NULL DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE welfare_program_raw IS '정부24 응답 원문. 재처리 기준이며 파생 데이터는 여기서 다시 만든다';
COMMENT ON COLUMN welfare_program_raw.payload_hash IS '세 원문을 이어붙인 SHA-256. 내용이 바뀐 사업만 재파싱·재색인하는 데 쓴다. 이관 시점에는 비워 두고 다음 동기화에서 애플리케이션이 채운다';

-- payload_hash는 비워 둔다. jsonb를 text로 바꾸면 Postgres가 콜론 뒤에 공백을 넣어
-- 애플리케이션(Jackson)이 만드는 문자열과 달라지므로, 여기서 계산한 해시는 비교에 쓸 수 없다.
-- NULL은 "아직 해시를 모름"을 뜻하고, 다음 동기화에서 애플리케이션이 같은 규칙으로 채운다.
INSERT INTO welfare_program_raw (program_id, list_json, detail_json, condition_json, payload_hash, fetched_at)
SELECT p.id,
       COALESCE(p.raw_list_json, '{}'::jsonb),
       COALESCE(p.raw_detail_json, '{}'::jsonb),
       COALESCE(p.raw_condition_json, '{}'::jsonb),
       NULL,
       COALESCE(p.last_synced_at, CURRENT_TIMESTAMP)
FROM welfare_programs p
ON CONFLICT (program_id) DO NOTHING;

CREATE TABLE welfare_program_regions (
    id UUID PRIMARY KEY,
    program_id UUID NOT NULL REFERENCES welfare_programs(id) ON DELETE CASCADE,
    region_scope VARCHAR(20) NOT NULL,
    region_province VARCHAR(40),
    region_district VARCHAR(60),
    created_at TIMESTAMPTZ(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT welfare_program_regions_scope_check
        CHECK (region_scope IN ('NATIONAL', 'PROVINCE', 'DISTRICT', 'UNKNOWN')),
    -- 범위별로 채워야 하는 칸을 고정해, 판정에 쓰이지 않는 값이 남아 추천을 흔들지 않게 한다.
    CONSTRAINT welfare_program_regions_shape_check CHECK (
        (region_scope IN ('NATIONAL', 'UNKNOWN') AND region_province IS NULL AND region_district IS NULL)
        OR (region_scope = 'PROVINCE' AND region_province IS NOT NULL AND region_district IS NULL)
        OR (region_scope = 'DISTRICT' AND region_district IS NOT NULL)
    )
);

COMMENT ON TABLE welfare_program_regions IS '사업이 제공되는 행정구역. 한 사업이 여러 지역을 가질 수 있다';
COMMENT ON COLUMN welfare_program_regions.region_district IS '시·군·구 토큰(공백 구분, 예: 수원시 장안구)';

CREATE UNIQUE INDEX welfare_program_regions_unique_idx
    ON welfare_program_regions (program_id, region_scope,
        COALESCE(region_province, ''), COALESCE(region_district, ''));
CREATE INDEX welfare_program_regions_program_idx
    ON welfare_program_regions (program_id);
CREATE INDEX welfare_program_regions_lookup_idx
    ON welfare_program_regions (region_scope, region_province, region_district);

-- 기존 단일 컬럼 값을 그대로 옮긴다. 범위와 값이 어긋난 행(예: PROVINCE인데 시·도가 비어 있음)은
-- UNKNOWN으로 낮춰 담고, 애플리케이션 기동 시 소관기관 정보로 다시 판정한다.
WITH normalized AS (
    SELECT p.id,
           CASE
               WHEN p.region_scope = 'NATIONAL' THEN 'NATIONAL'
               WHEN p.region_scope = 'DISTRICT' AND p.region_district IS NOT NULL THEN 'DISTRICT'
               WHEN p.region_scope IN ('PROVINCE', 'DISTRICT') AND p.region_province IS NOT NULL THEN 'PROVINCE'
               ELSE 'UNKNOWN'
           END AS scope,
           p.region_province,
           p.region_district
    FROM welfare_programs p
)
INSERT INTO welfare_program_regions (id, program_id, region_scope, region_province, region_district, created_at)
SELECT gen_random_uuid(), n.id, n.scope,
       CASE WHEN n.scope IN ('PROVINCE', 'DISTRICT') THEN n.region_province END,
       CASE WHEN n.scope = 'DISTRICT' THEN n.region_district END,
       CURRENT_TIMESTAMP
FROM normalized n;

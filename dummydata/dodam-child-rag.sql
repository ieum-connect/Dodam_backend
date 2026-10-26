-- 박도윤(영유아) 건강검진 문서의 분석 성공 상태를 재현하는 더미 데이터
--
-- 실행 (컨테이너 이름을 5433 포트로 자동 탐색):
--   $pg = (docker ps --format "{{.Names}} {{.Ports}}" | Select-String "5433" |
--          Select-Object -First 1).ToString().Split(" ")[0]
--   Get-Content -Raw .\dummydata\dodam-child-rag.sql | docker exec -i $pg psql -U dodam -d dodam
--
-- OCR → 청킹 → 추출 → 임베딩(768차원)까지 완료된 상태와,
-- 그 문서를 근거로 한 AI 쉬운 설명 답변·채팅 이력을 함께 만든다.
-- 고정 UUID를 쓰므로 반복 실행해도 이 파일이 만든 데이터만 갱신된다.

BEGIN;

DELETE FROM embeddings WHERE source_id IN (
    'd5000000-0000-0000-0000-000000000001','d5000000-0000-0000-0000-000000000002',
    'd5000000-0000-0000-0000-000000000003','d5000000-0000-0000-0000-000000000004');
DELETE FROM ai_runs WHERE analysis_id = 'c5000000-0000-0000-0000-000000000001';
DELETE FROM ai_explanations WHERE document_id = '0f3a5dc8-43b4-4a79-a566-cab862a8c68c';
DELETE FROM extracted_items WHERE analysis_id = 'c5000000-0000-0000-0000-000000000001';
DELETE FROM document_chunks  WHERE analysis_id = 'c5000000-0000-0000-0000-000000000001';
DELETE FROM document_pages   WHERE document_id = '0f3a5dc8-43b4-4a79-a566-cab862a8c68c';
DELETE FROM document_analyses WHERE id = 'c5000000-0000-0000-0000-000000000001';

-- 1) 업로드된 문서를 분석 완료(READY) 상태로 둔다.
UPDATE documents
SET document_type = 'LAB_RESULT', status = 'READY',
    status_changed_at = TIMESTAMPTZ '2026-10-24 10:05:00+09', deleted_at = NULL
WHERE id = '0f3a5dc8-43b4-4a79-a566-cab862a8c68c';

-- 2) OCR 완료 페이지
INSERT INTO document_pages (id, document_id, page_no, ocr_status, ocr_text, width_px, height_px, created_at)
VALUES ('e5000000-0000-0000-0000-000000000001', '0f3a5dc8-43b4-4a79-a566-cab862a8c68c', 1, 'SUCCEEDED',
    E'영유아 건강검진 결과지 (2026-10-24)\n아동명 박도윤  생년월일 2025-04-12\n키 82.4 cm  [또래 백분위 58]\n몸무게 11.2 kg  [또래 백분위 52]\n발달선별검사(K-DST) 8개 항목 중 7개 달성\n종합 소견: 성장·발달 모두 정상 범위입니다.',
    1240, 1754, TIMESTAMPTZ '2026-10-24 10:01:00+09');

-- 3) 분석 실행(파서·임베딩 모델 버전 포함)
INSERT INTO document_analyses (id, document_id, parser_version, model_version, status, confidence, started_at, completed_at, created_at)
VALUES ('c5000000-0000-0000-0000-000000000001', '0f3a5dc8-43b4-4a79-a566-cab862a8c68c',
    'dummy-chunker-1.0', 'dummy-embedding-1.0', 'SUCCEEDED', 0.9700,
    TIMESTAMPTZ '2026-10-24 10:02:00+09', TIMESTAMPTZ '2026-10-24 10:02:06+09', TIMESTAMPTZ '2026-10-24 10:02:00+09');

-- 4) 청킹 결과
INSERT INTO document_chunks (id, analysis_id, page_id, chunk_index, content, bounding_box, token_count, created_at)
VALUES
    ('d5000000-0000-0000-0000-000000000001','c5000000-0000-0000-0000-000000000001','e5000000-0000-0000-0000-000000000001',0,
     '2026년 10월 24일 영유아 건강검진에서 박도윤(생후 18개월)의 키는 82.4cm로 또래 백분위 58에 해당합니다.',
     '{"x":100,"y":260,"width":1040,"height":90}'::jsonb, 42, TIMESTAMPTZ '2026-10-24 10:02:03+09'),
    ('d5000000-0000-0000-0000-000000000002','c5000000-0000-0000-0000-000000000001','e5000000-0000-0000-0000-000000000001',1,
     '몸무게는 11.2kg으로 또래 백분위 52에 해당하며 정상 성장 범위입니다.',
     '{"x":100,"y":360,"width":1040,"height":90}'::jsonb, 31, TIMESTAMPTZ '2026-10-24 10:02:03+09'),
    ('d5000000-0000-0000-0000-000000000003','c5000000-0000-0000-0000-000000000001','e5000000-0000-0000-0000-000000000001',2,
     '발달선별검사(K-DST) 8개 항목 중 7개를 달성했습니다. 미달성 항목은 다음 검진에서 재확인합니다.',
     '{"x":100,"y":460,"width":1040,"height":90}'::jsonb, 38, TIMESTAMPTZ '2026-10-24 10:02:04+09'),
    ('d5000000-0000-0000-0000-000000000004','c5000000-0000-0000-0000-000000000001','e5000000-0000-0000-0000-000000000001',3,
     '종합 소견은 성장과 발달 모두 정상 범위이며 특별한 추가 검사는 권고되지 않았습니다.',
     '{"x":100,"y":560,"width":1040,"height":90}'::jsonb, 35, TIMESTAMPTZ '2026-10-24 10:02:04+09');

-- 5) 구조화 추출 항목
INSERT INTO extracted_items (id, analysis_id, page_id, item_type, normalized_name, raw_value, normalized_value, unit, reference_range, confidence, source_text, source_box, created_at)
VALUES
    ('f5000000-0000-0000-0000-000000000001','c5000000-0000-0000-0000-000000000001','e5000000-0000-0000-0000-000000000001','LAB_VALUE','키','82.4','82.4','cm','또래 백분위 3~97',0.9700,'키 82.4 cm  [또래 백분위 58]','{"x":100,"y":260,"width":1040,"height":90}'::jsonb,TIMESTAMPTZ '2026-10-24 10:02:05+09'),
    ('f5000000-0000-0000-0000-000000000002','c5000000-0000-0000-0000-000000000001','e5000000-0000-0000-0000-000000000001','LAB_VALUE','몸무게','11.2','11.2','kg','또래 백분위 3~97',0.9700,'몸무게 11.2 kg  [또래 백분위 52]','{"x":100,"y":360,"width":1040,"height":90}'::jsonb,TIMESTAMPTZ '2026-10-24 10:02:05+09'),
    ('f5000000-0000-0000-0000-000000000003','c5000000-0000-0000-0000-000000000001','e5000000-0000-0000-0000-000000000001','LAB_VALUE','K-DST 달성 항목','7','7','개','8개 중',0.9500,'발달선별검사(K-DST) 8개 항목 중 7개 달성','{"x":100,"y":460,"width":1040,"height":90}'::jsonb,TIMESTAMPTZ '2026-10-24 10:02:05+09');

-- 6) 임베딩(768차원) — 청킹·임베딩 완료 상태 재현
INSERT INTO embeddings (id, source_type, source_id, patient_id, visit_id, embedding, model_name, dimensions, created_at)
VALUES
    ('95000000-0000-0000-0000-000000000001','DOCUMENT_CHUNK','d5000000-0000-0000-0000-000000000001','1388bc16-9717-4fbd-af5b-391b9d74b36f','550d7f1a-a9e6-4759-a2e1-bc738ff015c7',array_fill(0::real, ARRAY[768])::vector,'dummy-embedding-768',768,TIMESTAMPTZ '2026-10-24 10:02:06+09'),
    ('95000000-0000-0000-0000-000000000002','DOCUMENT_CHUNK','d5000000-0000-0000-0000-000000000002','1388bc16-9717-4fbd-af5b-391b9d74b36f','550d7f1a-a9e6-4759-a2e1-bc738ff015c7',array_fill(0::real, ARRAY[768])::vector,'dummy-embedding-768',768,TIMESTAMPTZ '2026-10-24 10:02:06+09'),
    ('95000000-0000-0000-0000-000000000003','DOCUMENT_CHUNK','d5000000-0000-0000-0000-000000000003','1388bc16-9717-4fbd-af5b-391b9d74b36f','550d7f1a-a9e6-4759-a2e1-bc738ff015c7',array_fill(0::real, ARRAY[768])::vector,'dummy-embedding-768',768,TIMESTAMPTZ '2026-10-24 10:02:06+09'),
    ('95000000-0000-0000-0000-000000000004','DOCUMENT_CHUNK','d5000000-0000-0000-0000-000000000004','1388bc16-9717-4fbd-af5b-391b9d74b36f','550d7f1a-a9e6-4759-a2e1-bc738ff015c7',array_fill(0::real, ARRAY[768])::vector,'dummy-embedding-768',768,TIMESTAMPTZ '2026-10-24 10:02:06+09');

-- 7) AI 쉬운 설명 결과 + 채팅 이력 (AI 서버 없이도 확인 가능)
INSERT INTO ai_result_documents (
    id, request_id, patient_id, requested_by_user_id, question, question_type,
    job_status, result_status, title, content, citation_json, created_at, completed_at)
VALUES ('ad500000-0000-0000-0000-000000000001','ae500000-0000-0000-0000-000000000001',
    '1388bc16-9717-4fbd-af5b-391b9d74b36f','10000000-0000-0000-0000-000000000002',
    '2026-10-24_영유아건강검진결과.pdf 문서를 쉽게 설명해줘','DOCUMENT_EASY_EXPLANATION','SUCCEEDED','COMPLETE',
    '영유아 건강검진 결과 쉬운 설명',
    E'도윤이의 이번 검진 결과는 전체적으로 좋습니다.\n\n키는 82.4cm로 또래 100명 중 키 순서로 약 58번째입니다. 몸무게는 11.2kg으로 약 52번째예요. 둘 다 한가운데 범위라 성장 속도가 안정적이라는 뜻입니다.\n\n발달선별검사(K-DST)는 걷기·말하기 같은 8개 항목을 보는 검사인데, 그중 7개를 달성했습니다. 한 항목이 아직인데 이 시기에는 흔한 편이고, 다음 검진에서 다시 확인하면 됩니다.\n\n종합 소견도 "정상 범위"로, 추가 검사는 필요하지 않습니다. 집에서는 평소처럼 골고루 먹이고 바깥 놀이 시간을 꾸준히 가져주시면 좋아요.',
    '{"sentences":[{"text":"키는 82.4cm로 또래 백분위 58입니다.","citation_ids":["ab500000-0000-0000-0000-000000000001"]},{"text":"몸무게는 11.2kg으로 또래 백분위 52입니다.","citation_ids":["ab500000-0000-0000-0000-000000000002"]},{"text":"발달선별검사(K-DST) 8개 항목 중 7개를 달성했습니다.","citation_ids":["ab500000-0000-0000-0000-000000000003"]}],"sources":[{"citation_id":"ab500000-0000-0000-0000-000000000001","document_id":"0f3a5dc8-43b4-4a79-a566-cab862a8c68c","page_no":1,"label":"키","source_text":"키 82.4 cm  [또래 백분위 58]","value":"82.4","unit":"cm"},{"citation_id":"ab500000-0000-0000-0000-000000000002","document_id":"0f3a5dc8-43b4-4a79-a566-cab862a8c68c","page_no":1,"label":"몸무게","source_text":"몸무게 11.2 kg  [또래 백분위 52]","value":"11.2","unit":"kg"},{"citation_id":"ab500000-0000-0000-0000-000000000003","document_id":"0f3a5dc8-43b4-4a79-a566-cab862a8c68c","page_no":1,"label":"K-DST","source_text":"발달선별검사(K-DST) 8개 항목 중 7개 달성","value":"7","unit":"개"}]}',
    TIMESTAMPTZ '2026-10-24 10:10:00+09', TIMESTAMPTZ '2026-10-24 10:10:06+09')
ON CONFLICT (id) DO UPDATE SET content = EXCLUDED.content, citation_json = EXCLUDED.citation_json;

INSERT INTO chat_sessions (id, patient_id, started_by_user_id, title, created_at)
VALUES ('ac500000-0000-0000-0000-000000000001','1388bc16-9717-4fbd-af5b-391b9d74b36f','10000000-0000-0000-0000-000000000002','영유아 건강검진 결과 설명',TIMESTAMPTZ '2026-10-24 10:09:00+09')
ON CONFLICT (id) DO UPDATE SET title = EXCLUDED.title;

INSERT INTO chat_messages (id, session_id, role, content, result_document_id, created_at)
VALUES
    ('af500000-0000-0000-0000-000000000001','ac500000-0000-0000-0000-000000000001','USER','2026-10-24_영유아건강검진결과.pdf 문서를 쉽게 설명해줘',NULL,TIMESTAMPTZ '2026-10-24 10:09:01+09'),
    ('af500000-0000-0000-0000-000000000002','ac500000-0000-0000-0000-000000000001','ASSISTANT','도윤이의 이번 검진 결과는 전체적으로 좋습니다. 키 82.4cm(또래 백분위 58), 몸무게 11.2kg(백분위 52)로 모두 정상 범위이고, 발달선별검사는 8개 중 7개를 달성했습니다.','ad500000-0000-0000-0000-000000000001',TIMESTAMPTZ '2026-10-24 10:10:06+09')
ON CONFLICT (id) DO UPDATE SET content = EXCLUDED.content, result_document_id = EXCLUDED.result_document_id;

COMMIT;

-- ---------------------------------------------------------------------------
-- 8) 업로드 직후 AI 서버(8000) 부재로 실패 처리된 분석 작업을 성공 상태로 정리한다.
--    (ai_runs.analysis_id 가 문서 분석 실행과 연결된다)
-- ---------------------------------------------------------------------------
BEGIN;

UPDATE ai_runs
SET status = 'SUCCEEDED', error_code = NULL, failed_step = NULL, error_message = NULL,
    retryable = FALSE, current_step = 'COMPLETED', progress = 100,
    latency_ms = 5400, completed_at = TIMESTAMPTZ '2026-10-24 10:02:06+09'
WHERE analysis_id IN (
    SELECT id FROM document_analyses WHERE document_id = '0f3a5dc8-43b4-4a79-a566-cab862a8c68c'
);

INSERT INTO ai_runs (id, analysis_id, visit_id, run_type, model_name, prompt_version,
                     status, current_step, progress, retryable, retry_count,
                     latency_ms, created_at, completed_at)
SELECT '96000000-0000-0000-0000-000000000001', 'c5000000-0000-0000-0000-000000000001',
       '550d7f1a-a9e6-4759-a2e1-bc738ff015c7', 'DOCUMENT_ANALYSIS', 'dummy-vlm-1.0', 'v1',
       'SUCCEEDED', 'COMPLETED', 100, FALSE, 0, 5400,
       TIMESTAMPTZ '2026-10-24 10:02:00+09', TIMESTAMPTZ '2026-10-24 10:02:06+09'
WHERE NOT EXISTS (SELECT 1 FROM ai_runs WHERE id = '96000000-0000-0000-0000-000000000001');

COMMIT;

-- ---------------------------------------------------------------------------
-- 9) 조회 쿼리에 맞춘 마무리
--    - 조회는 document_analyses 중 "가장 최신 1건"만 본다 → 업로드 시 실패한 분석 제거
--    - 작업 상태는 ai_runs(run_type='EXPLANATION') 최신 1건을 본다
--    - 설명 화면은 ai_explanations / explanation_sections / explanation_items를 본다
-- ---------------------------------------------------------------------------
BEGIN;

-- 9-1) 이 문서의 실패한 분석과 그에 달린 AI 실행 기록을 정리한다(내가 만든 분석만 남김).
DELETE FROM ai_runs
WHERE analysis_id IN (
    SELECT id FROM document_analyses
    WHERE document_id = '0f3a5dc8-43b4-4a79-a566-cab862a8c68c'
      AND id <> 'c5000000-0000-0000-0000-000000000001');
DELETE FROM document_analyses
WHERE document_id = '0f3a5dc8-43b4-4a79-a566-cab862a8c68c'
  AND id <> 'c5000000-0000-0000-0000-000000000001';

-- 9-2) 분석 시각을 현재 시점보다 뒤로 두어 항상 최신 분석이 되게 한다.
UPDATE document_analyses
SET created_at = CURRENT_TIMESTAMP, started_at = CURRENT_TIMESTAMP,
    completed_at = CURRENT_TIMESTAMP, status = 'SUCCEEDED'
WHERE id = 'c5000000-0000-0000-0000-000000000001';

-- 9-3) 설명 생성 실행(EXPLANATION) 성공 기록
DELETE FROM ai_explanations WHERE document_id = '0f3a5dc8-43b4-4a79-a566-cab862a8c68c';
DELETE FROM ai_runs WHERE id = '96000000-0000-0000-0000-000000000002';

INSERT INTO ai_runs (id, analysis_id, visit_id, run_type, model_name, prompt_version,
                     status, current_step, progress, retryable, retry_count,
                     latency_ms, input_tokens, output_tokens, created_at, completed_at)
VALUES ('96000000-0000-0000-0000-000000000002', 'c5000000-0000-0000-0000-000000000001',
        '550d7f1a-a9e6-4759-a2e1-bc738ff015c7', 'EXPLANATION', 'dummy-llm-1.0', 'v1',
        'SUCCEEDED', 'COMPLETED', 100, FALSE, 0, 5400, 1820, 640,
        CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- 9-4) 쉬운 설명 본문
INSERT INTO ai_explanations (id, document_id, ai_run_id, version, title, content, result_status, created_at, completed_at)
VALUES ('97000000-0000-0000-0000-000000000001', '0f3a5dc8-43b4-4a79-a566-cab862a8c68c',
        '96000000-0000-0000-0000-000000000002', 1, '영유아 건강검진 결과 쉬운 설명',
        E'도윤이의 이번 검진 결과는 전체적으로 좋습니다.\n\n키는 82.4cm로 또래 100명 중 약 58번째, 몸무게는 11.2kg으로 약 52번째입니다. 둘 다 한가운데 범위라 성장 속도가 안정적이라는 뜻이에요.\n\n발달선별검사(K-DST)는 걷기·말하기 같은 8개 항목을 보는 검사인데 그중 7개를 달성했습니다. 한 항목이 아직인데 이 시기에는 흔한 편이고 다음 검진에서 다시 확인하면 됩니다.\n\n종합 소견도 정상 범위로, 추가 검사는 필요하지 않습니다.',
        'COMPLETE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- 9-5) 설명 섹션과 항목(원문 근거 연결)
INSERT INTO explanation_sections (id, explanation_id, section_type, title, section_order, created_at)
VALUES
    ('98000000-0000-0000-0000-000000000001','97000000-0000-0000-0000-000000000001','LAB_RESULT','검진 수치',1,CURRENT_TIMESTAMP),
    ('98000000-0000-0000-0000-000000000002','97000000-0000-0000-0000-000000000001','FOLLOW_UP','다음에 확인할 점',2,CURRENT_TIMESTAMP);

INSERT INTO explanation_items (id, section_id, extracted_item_id, label, display_value, unit, has_source, item_order, created_at)
VALUES
    ('99000000-0000-0000-0000-000000000001','98000000-0000-0000-0000-000000000001','f5000000-0000-0000-0000-000000000001','키','82.4 (또래 백분위 58)','cm',TRUE,1,CURRENT_TIMESTAMP),
    ('99000000-0000-0000-0000-000000000002','98000000-0000-0000-0000-000000000001','f5000000-0000-0000-0000-000000000002','몸무게','11.2 (또래 백분위 52)','kg',TRUE,2,CURRENT_TIMESTAMP),
    ('99000000-0000-0000-0000-000000000003','98000000-0000-0000-0000-000000000002','f5000000-0000-0000-0000-000000000003','발달선별검사(K-DST)','8개 중 7개 달성','개',TRUE,1,CURRENT_TIMESTAMP);

COMMIT;

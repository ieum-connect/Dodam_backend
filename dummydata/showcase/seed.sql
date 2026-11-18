\set ON_ERROR_STOP on
BEGIN;
SET LOCAL TIME ZONE 'Asia/Seoul';

INSERT INTO users (id, kakao_id, name, role, status, account_group_id) VALUES
    ('d0da0000-0000-4000-8000-000000000001', 'demo-caregiver', '김민지', 'CAREGIVER', 'ACTIVE', 'd0da0000-0000-4000-8000-000000000001'),
    ('d0da0000-0000-4000-8000-000000000002', 'demo-jihun', '박지훈', 'CAREGIVER', 'ACTIVE', 'd0da0000-0000-4000-8000-000000000002')
ON CONFLICT (kakao_id) DO NOTHING;

SELECT id AS minji FROM users WHERE kakao_id = 'demo-caregiver' \gset
SELECT id AS jihun FROM users WHERE kakao_id = 'demo-jihun' \gset
\set doyoon 'd0da0000-0000-4000-8000-000000000011'
\set dayeon 'd0da0000-0000-4000-8000-000000000012'

INSERT INTO patients (id, user_id, child_name, birth_date, gender, invite_code, status) VALUES
    (:'doyoon', :'minji', '박도윤 (시연)', (CURRENT_DATE - INTERVAL '25 months')::date, 'MALE', 'D0DM25', 'ACTIVE'),
    (:'dayeon', :'minji', '박다연 (시연)', (CURRENT_DATE - INTERVAL '49 months')::date, 'FEMALE', 'D0DM49', 'ACTIVE')
ON CONFLICT (id) DO NOTHING;

INSERT INTO caregiver_links (id, patient_id, caregiver_user_id, relation, status, accepted_at) VALUES
    ('d0da0000-0000-4000-8000-000000000021', :'doyoon', :'minji', '엄마', 'ACTIVE', CURRENT_TIMESTAMP),
    ('d0da0000-0000-4000-8000-000000000022', :'dayeon', :'minji', '엄마', 'ACTIVE', CURRENT_TIMESTAMP),
    ('d0da0000-0000-4000-8000-000000000023', :'doyoon', :'jihun', '아빠', 'ACTIVE', CURRENT_TIMESTAMP)
ON CONFLICT DO NOTHING;

CREATE TEMP TABLE showcase_visits (
    fixture_no integer, patient_id uuid, days_ago integer, hospital text, department text, memo text
) ON COMMIT DROP;
INSERT INTO showcase_visits VALUES
    (1, :'doyoon', 3, '도담소아청소년과', '영유아 건강검진', '성장 기록을 확인하고 다음 방문 일정을 정리했어요.'),
    (2, :'doyoon', 15, '이음소아청소년과', '기침으로 진료', '진료 안내문과 함께 가족에게 공유했어요.'),
    (3, :'doyoon', 32, '도담소아청소년과', '검사 결과 상담', '검사 결과지를 확인한 뒤 질문을 메모했어요.'),
    (4, :'dayeon', 7, '이음소아청소년과', '정기 건강검진', '정기 건강검진을 받았어요.'),
    (5, :'dayeon', 25, '도담소아청소년과', '진료 후 경과 확인', '가족이 함께 진료 내용을 확인했어요.');

INSERT INTO visits (id, patient_id, visited_on, hospital_name, department, memo, status)
SELECT ('d0da0000-0000-4000-8000-00000000003' || fixture_no)::uuid,
       patient_id, CURRENT_DATE - days_ago, hospital, department, memo, 'DONE'
FROM showcase_visits ON CONFLICT (id) DO NOTHING;

INSERT INTO health_records (id, patient_id, author_user_id, visit_id, input_type, content, recorded_at)
SELECT ('d0da0000-0000-4000-8000-00000000004' || f.fixture_no)::uuid,
       f.patient_id, :'minji', v.id, 'TEXT', f.memo, v.visited_on + TIME '16:00'
FROM showcase_visits f
JOIN visits v ON v.id = ('d0da0000-0000-4000-8000-00000000003' || f.fixture_no)::uuid
ON CONFLICT (id) DO NOTHING;

INSERT INTO growth_records (id, patient_id, author_user_id, measured_on, height_cm, weight_kg, note, created_at, updated_at)
SELECT ('d0da0000-0000-4000-8000-' || lpad((100 + child.ordinal * 20 + sample.ordinal)::text, 12, '0'))::uuid,
       child.patient_id, :'minji', (p.birth_date + make_interval(months => child.anchor_age))::date - sample.days_ago,
       round(child.height - sample.days_ago * child.height_per_year / 365, 1),
       round(child.weight - sample.days_ago * child.weight_per_year / 365, 1),
       '시연용 가상 측정값', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM (VALUES (1, :'doyoon'::uuid, 25, 84.1, 14.2, 9.4, 2.5),
             (2, :'dayeon'::uuid, 49, 103.5, 16.8, 7.0, 2.0))
     AS child(ordinal, patient_id, anchor_age, height, weight, height_per_year, weight_per_year)
JOIN patients p ON p.id = child.patient_id
CROSS JOIN unnest(ARRAY[345,315,285,255,225,195,165,135,105,75,60,45,30,14,0])
    WITH ORDINALITY AS sample(days_ago, ordinal)
ON CONFLICT DO NOTHING;

INSERT INTO documents (id, visit_id, patient_id, uploader_user_id, storage_key, file_name, mime_type,
                       file_size_bytes, document_type, declared_doc_type, status)
SELECT ('d0da0000-0000-4000-8000-00000000005' || doc.ordinal)::uuid,
       v.id, v.patient_id, :'minji', 'showcase/' || doc.asset, doc.filename, 'image/png',
       doc.size, doc.doc_type, doc.doc_type, 'READY'
FROM (VALUES
    (1, 1, 'checkup.png', '영유아 건강검진 결과지 (시연용).png', :'checkup_size'::bigint, 'DIAGNOSIS'),
    (2, 2, 'medication.png', '복약 안내문 (시연용).png', :'medication_size'::bigint, 'PRESCRIPTION'),
    (3, 3, 'lab-result.png', '검사 결과 확인서 (시연용).png', :'lab_result_size'::bigint, 'LAB_RESULT'),
    (4, 2, 'outpatient.png', '외래 진료 안내문 (시연용).png', :'outpatient_size'::bigint, 'UNKNOWN'),
    (5, 4, 'sibling-checkup.png', '정기 건강검진 확인서 (시연용).png', :'sibling_checkup_size'::bigint, 'DIAGNOSIS')
) AS doc(ordinal, visit_no, asset, filename, size, doc_type)
JOIN visits v ON v.id = ('d0da0000-0000-4000-8000-00000000003' || doc.visit_no)::uuid
ON CONFLICT (id) DO NOTHING;

INSERT INTO chat_sessions (id, patient_id, started_by_user_id, title)
VALUES ('d0da0000-0000-4000-8000-000000000061', :'doyoon', :'minji', '[시연용] 영유아 건강검진 결과지 설명')
ON CONFLICT (id) DO NOTHING;

INSERT INTO chat_messages (id, session_id, role, content, created_at) VALUES
    ('d0da0000-0000-4000-8000-000000000062', 'd0da0000-0000-4000-8000-000000000061', 'USER',
     '영유아 건강검진 결과지에 어떤 내용이 적혀 있나요?', CURRENT_TIMESTAMP),
    ('d0da0000-0000-4000-8000-000000000063', 'd0da0000-0000-4000-8000-000000000061', 'ASSISTANT',
     '[시연용 예시 답변] 결과지에는 도담소아청소년과에서 성장 기록을 확인하고 다음 방문 일정을 정리했다는 내용이 적혀 있어요.',
     CURRENT_TIMESTAMP + INTERVAL '1 second')
ON CONFLICT (id) DO NOTHING;

INSERT INTO action_items (id, visit_id, document_id, type, title, description, approval_status)
SELECT ('d0da0000-0000-4000-8000-00000000007' || item.ordinal)::uuid,
       d.visit_id, d.id, item.kind, item.title, '문서 검토 흐름을 보여 주기 위한 시연용 후보입니다.', 'PENDING'
FROM (VALUES (1, 'REVISIT', '다음 방문 일정 정하기 (시연)'),
             (2, 'MEDICATION', '복약 알림 등록 (시연)')) AS item(ordinal, kind, title)
JOIN documents d ON d.id = ('d0da0000-0000-4000-8000-00000000005' || item.ordinal)::uuid
ON CONFLICT (id) DO NOTHING;

INSERT INTO schedules (id, patient_id, schedule_type, title, starts_at, status, notes) VALUES
    ('d0da0000-0000-4000-8000-000000000081', :'doyoon', 'REVISIT', '소아과 경과 확인 (시연)', CURRENT_DATE + TIME '15:00', 'SCHEDULED', '검진 후 경과 확인 예시'),
    ('d0da0000-0000-4000-8000-000000000082', :'doyoon', 'MEDICATION', '복용 시간 확인 (시연)', CURRENT_DATE + TIME '18:00', 'SCHEDULED', '가상 복약 일정'),
    ('d0da0000-0000-4000-8000-000000000083', :'dayeon', 'REVISIT', '정기 검진 일정 확인 (시연)', CURRENT_DATE + TIME '16:00', 'SCHEDULED', '가상 정기 검진 일정'),
    ('d0da0000-0000-4000-8000-000000000084', :'dayeon', 'MEDICATION', '복약 알림 연습 (시연)', CURRENT_DATE + TIME '18:30', 'SCHEDULED', '가상 복약 일정')
ON CONFLICT (id) DO UPDATE SET starts_at = EXCLUDED.starts_at
WHERE schedules.status = 'SCHEDULED';

INSERT INTO vaccination_dose_checks (id, patient_id, dose_id, checked_by)
SELECT ('d0da0000-0000-4000-8000-' || lpad((200 + dose.ordinal)::text, 12, '0'))::uuid,
       dose.patient_id, dose.dose_id, :'minji'
FROM (VALUES (1, :'doyoon'::uuid, 'BCG-1'), (2, :'doyoon'::uuid, 'HEPB-1'),
             (3, :'doyoon'::uuid, 'HEPB-2'), (4, :'doyoon'::uuid, 'HEPB-3'),
             (5, :'doyoon'::uuid, 'DTAP-1'), (6, :'dayeon'::uuid, 'BCG-1'),
             (7, :'dayeon'::uuid, 'HEPB-1'), (8, :'dayeon'::uuid, 'HEPB-2')) AS dose(ordinal, patient_id, dose_id)
ON CONFLICT DO NOTHING;

INSERT INTO child_welfare_profiles (id, patient_id, storage_consent, consented_at, region_code, region_name,
                                    household_size, child_count, income_band, childcare_status, is_dual_income,
                                    is_single_parent, is_multicultural, has_disability) VALUES
    ('d0da0000-0000-4000-8000-000000000091', :'doyoon', TRUE, CURRENT_TIMESTAMP, '1144000000',
     '서울특별시 마포구', 4, 2, 'BELOW_100', '어린이집 이용', TRUE, FALSE, FALSE, FALSE),
    ('d0da0000-0000-4000-8000-000000000092', :'dayeon', TRUE, CURRENT_TIMESTAMP, '1144000000',
     '서울특별시 마포구', 4, 2, 'BELOW_100', '유치원 이용', TRUE, FALSE, FALSE, FALSE)
ON CONFLICT (patient_id) DO NOTHING;

INSERT INTO welfare_programs (id, external_service_id, name, purpose_summary, target_text, benefit_text,
    application_method, agency_name, agency_type, region_scope, region_province, region_district, active,
    child_candidate, raw_list_json, raw_detail_json, raw_condition_json, last_synced_at, created_at, updated_at)
SELECT ('d0da0000-0000-4000-8000-' || lpad((300 + program.ordinal)::text, 12, '0'))::uuid,
    'DODAM-LOCAL-SHOWCASE-' || program.ordinal, '[시연용] ' || program.name, '맞춤 복지 화면을 위한 가상 예시입니다.',
    program.target, '시연용 서비스 예시', '가상의 시연용 사업입니다.', '도담 시연기관',
    CASE WHEN program.district IS NULL THEN '중앙행정기관' ELSE '지방자치단체' END,
    CASE WHEN program.district IS NULL THEN 'NATIONAL' ELSE 'DISTRICT' END,
    CASE WHEN program.district IS NOT NULL THEN '서울특별시' END, program.district, TRUE, TRUE,
    '{}'::jsonb, '{}'::jsonb, '{}'::jsonb, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM (VALUES (1, '영유아 가족 돌봄 상담', '만 0세~6세 아동을 양육하는 가구', NULL),
             (2, '마포구 영유아 놀이 지원', '서울특별시 마포구에 거주하는 만 0세~6세 아동', '마포구'),
             (3, '두 자녀 가족 양육 지원', '만 0세~6세 아동을 포함해 자녀 2명 이상을 양육하는 가구', NULL),
             (4, '강남구 영유아 놀이 지원', '서울특별시 강남구에 거주하는 만 0세~6세 아동', '강남구'))
     AS program(ordinal, name, target, district)
ON CONFLICT (external_service_id) DO NOTHING;

-- Extra examples for list filters, previous activity and repeat management.
INSERT INTO health_records (id, patient_id, author_user_id, input_type, content, recorded_at)
SELECT ('d0da0000-0000-4000-8000-' || lpad((400 + sample.ordinal)::text, 12, '0'))::uuid,
       sample.patient_id, sample.author_id, 'TEXT', sample.content, CURRENT_DATE - sample.days_ago + sample.recorded_time
FROM (VALUES
    (1, :'doyoon'::uuid, :'minji'::uuid, 0, TIME '08:20', '아침을 먹고 어린이집에 갔어요. 오늘은 하원 후 책을 같이 읽기로 했어요.'),
    (2, :'doyoon'::uuid, :'jihun'::uuid, 1, TIME '15:10', '낮잠을 한 시간 반 잤어요. 일어나서 물을 잘 마셨어요.'),
    (3, :'doyoon'::uuid, :'minji'::uuid, 2, TIME '19:10', '복약 알림 시연을 위해 저녁 복용 시간을 메모했어요.'),
    (4, :'dayeon'::uuid, :'minji'::uuid, 0, TIME '08:30', '유치원에 가져갈 물병과 손수건을 챙겼어요.'),
    (5, :'dayeon'::uuid, :'minji'::uuid, 1, TIME '18:30', '하원 후 공원에서 뛰어놀고 저녁을 잘 먹었어요.')
) AS sample(ordinal, patient_id, author_id, days_ago, recorded_time, content)
ON CONFLICT (id) DO NOTHING;

INSERT INTO memo_tags (id, patient_id, name)
SELECT ('d0da0000-0000-4000-8000-' || lpad((410 + tag.ordinal)::text, 12, '0'))::uuid, tag.patient_id, tag.name
FROM (VALUES (1, :'doyoon'::uuid, '식사'), (2, :'doyoon'::uuid, '수면'), (3, :'doyoon'::uuid, '복약'),
             (4, :'dayeon'::uuid, '유치원'), (5, :'dayeon'::uuid, '활동')) AS tag(ordinal, patient_id, name)
ON CONFLICT DO NOTHING;

INSERT INTO health_record_tags (record_id, tag_id)
SELECT r.id, t.id
FROM generate_series(1, 5) AS sample(ordinal)
JOIN health_records r ON r.id = ('d0da0000-0000-4000-8000-' || lpad((400 + sample.ordinal)::text, 12, '0'))::uuid
JOIN memo_tags t ON t.id = ('d0da0000-0000-4000-8000-' || lpad((410 + sample.ordinal)::text, 12, '0'))::uuid
WHERE r.deleted_at IS NULL AND t.deleted_at IS NULL
ON CONFLICT DO NOTHING;

INSERT INTO medication_series (id, patient_id, title, notes, start_date, time_of_day, time_zone, next_date, weekday_mask, rule_from)
VALUES
    ('d0da0000-0000-4000-8000-000000000421', :'doyoon', '저녁 복약 확인 (매일 · 시연)',
     '등록과 반복 규칙 수정을 보여 주기 위한 예시입니다.', CURRENT_DATE, TIME '19:00', 'Asia/Seoul', CURRENT_DATE, 127, CURRENT_DATE),
    ('d0da0000-0000-4000-8000-000000000422', :'doyoon', '복약 기록 점검 (월·수·금 · 시연)',
     '요일별 반복 설정 예시입니다.', CURRENT_DATE, TIME '08:30', 'Asia/Seoul', CURRENT_DATE, 21, CURRENT_DATE)
ON CONFLICT (id) DO NOTHING;

INSERT INTO schedules (id, patient_id, schedule_type, title, starts_at, status, notes) VALUES
    ('d0da0000-0000-4000-8000-000000000431', :'doyoon', 'MEDICATION', '아침 복약 기록 (시연)', CURRENT_DATE + TIME '08:00', 'COMPLETED', '완료된 복약 예시'),
    ('d0da0000-0000-4000-8000-000000000432', :'doyoon', 'TEST', '검진 결과 상담 (시연)', CURRENT_DATE + 2 + TIME '11:00', 'SCHEDULED', '다른 날짜의 건강 일정 예시'),
    ('d0da0000-0000-4000-8000-000000000433', :'dayeon', 'MEDICATION', '아침 복용 확인 (시연)', CURRENT_DATE + TIME '08:15', 'COMPLETED', '완료된 복약 예시')
ON CONFLICT (id) DO NOTHING;

INSERT INTO system_notifications (id, recipient_user_id, type, message, path, created_at, read_at) VALUES
    ('d0da0000-0000-4000-8000-000000000441', :'minji', 'SCHEDULE_DUE',
     '복용 시간 확인 (시연) — 복용 여부를 확인해주세요.', '/meds?patientId=' || :'doyoon' || '&date=' || CURRENT_DATE,
     CURRENT_TIMESTAMP - INTERVAL '5 minutes', NULL),
    ('d0da0000-0000-4000-8000-000000000442', :'minji', 'SCHEDULE_DUE',
     '소아과 경과 확인 (시연) — 일정을 확인해주세요.', '/calendar?patientId=' || :'doyoon' || '&date=' || CURRENT_DATE,
     CURRENT_TIMESTAMP - INTERVAL '1 hour', NULL),
    ('d0da0000-0000-4000-8000-000000000443', :'minji', 'RELATION_ACCEPTED',
     '박지훈 보호자와 아이 기록을 공유하고 있어요. (시연)', '/home',
     CURRENT_TIMESTAMP - INTERVAL '1 day', CURRENT_TIMESTAMP - INTERVAL '12 hours')
ON CONFLICT (id) DO NOTHING;

INSERT INTO chat_sessions (id, patient_id, started_by_user_id, title)
VALUES ('d0da0000-0000-4000-8000-000000000451', :'doyoon', :'minji', '[시연용] 복약 안내문 확인'),
       ('d0da0000-0000-4000-8000-000000000454', :'dayeon', :'minji', '[시연용] 정기 건강검진 확인서 설명')
ON CONFLICT (id) DO NOTHING;

INSERT INTO chat_messages (id, session_id, role, content, created_at) VALUES
    ('d0da0000-0000-4000-8000-000000000452', 'd0da0000-0000-4000-8000-000000000451', 'USER', '복약 안내문에 약 이름이 적혀 있나요?', CURRENT_TIMESTAMP),
    ('d0da0000-0000-4000-8000-000000000453', 'd0da0000-0000-4000-8000-000000000451', 'ASSISTANT',
     '[시연용 예시 답변] 이 안내문은 복약 알림 등록을 보여 주는 자료예요. 약 이름이나 용량은 적혀 있지 않아요.', CURRENT_TIMESTAMP + INTERVAL '1 second'),
    ('d0da0000-0000-4000-8000-000000000455', 'd0da0000-0000-4000-8000-000000000454', 'USER', '확인서의 방문 목적을 알려주세요.', CURRENT_TIMESTAMP),
    ('d0da0000-0000-4000-8000-000000000456', 'd0da0000-0000-4000-8000-000000000454', 'ASSISTANT',
     '[시연용 예시 답변] 확인서에 적힌 방문 목적은 정기 건강검진이에요.', CURRENT_TIMESTAMP + INTERVAL '1 second')
ON CONFLICT (id) DO NOTHING;

INSERT INTO welfare_favorites (id, patient_id, program_id) VALUES
    ('d0da0000-0000-4000-8000-000000000461', :'doyoon', 'd0da0000-0000-4000-8000-000000000302'),
    ('d0da0000-0000-4000-8000-000000000462', :'dayeon', 'd0da0000-0000-4000-8000-000000000301')
ON CONFLICT DO NOTHING;

COMMIT;

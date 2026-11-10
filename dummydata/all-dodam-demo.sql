-- IEUM/도담 통합 데모 데이터 진입점
-- 이 파일 하나만 실행하면 기존 데모·RAG·소견서 데이터를 순서대로 적용합니다.
-- 실행 예:
--   Get-Content -Raw .\dummydata\all-dodam-demo.sql |
--     docker exec -i dodam_backend-postgres-1 psql -U dodam -d dodam
--
-- 각 SQL은 기능별 원본으로 보존하고, 이 파일을 재현용 단일 진입점으로 사용합니다.

\set ON_ERROR_STOP on
\ir seed-dodam-demo.sql
\ir dodam-child-rag.sql
\ir guided-rag-chat.sql
\ir issue-4-opinion-document.sql

-- 현재 데모 DB의 채팅 기록만 초기화합니다.
-- 문서, 방문 기록, AI 결과와 가족 연결 데이터는 삭제하지 않습니다.
\set ON_ERROR_STOP on
BEGIN;
DELETE FROM chat_messages;
DELETE FROM chat_sessions;
COMMIT;

-- 의료 문서 RAG 색인 상태와 채팅 답변의 검색 근거 메타데이터를 저장한다.
-- 기존 문서는 PENDING으로 초기화하여 AI 백엔드의 재색인 대상 조회에 포함시킨다.
ALTER TABLE documents
    ADD COLUMN embedding_status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    ADD COLUMN embedding_model VARCHAR(100),
    ADD COLUMN embedding_dimensions INTEGER,
    ADD COLUMN embedding_started_at TIMESTAMPTZ(6),
    ADD COLUMN embedding_completed_at TIMESTAMPTZ(6),
    ADD COLUMN embedding_error_code VARCHAR(100),
    ADD COLUMN embedding_retry_count INTEGER NOT NULL DEFAULT 0;

ALTER TABLE documents
    ADD CONSTRAINT documents_embedding_status_check
        CHECK (embedding_status IN ('PENDING', 'INDEXING', 'READY', 'FAILED')),
    ADD CONSTRAINT documents_embedding_dimensions_check
        CHECK (embedding_dimensions IS NULL OR embedding_dimensions > 0),
    ADD CONSTRAINT documents_embedding_retry_count_check
        CHECK (embedding_retry_count >= 0);

CREATE INDEX documents_embedding_reindex_idx
    ON documents (embedding_status, created_at, id)
    WHERE deleted_at IS NULL;

ALTER TABLE chat_messages
    ADD COLUMN rag_metadata_json TEXT;

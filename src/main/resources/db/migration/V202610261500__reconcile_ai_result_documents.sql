-- Some development databases received this schema under the provisional
-- V202610140900 version. Keep this migration idempotent so those databases and
-- databases built from the canonical V202610141300 migration converge.
CREATE TABLE IF NOT EXISTS ai_result_documents (
    id UUID PRIMARY KEY,
    request_id UUID NOT NULL UNIQUE,
    patient_id UUID NOT NULL,
    requested_by_user_id UUID NOT NULL,
    question TEXT NOT NULL,
    question_type VARCHAR(40) NOT NULL,
    job_status VARCHAR(20) NOT NULL,
    result_status VARCHAR(20),
    title VARCHAR(200),
    content TEXT,
    citation_json TEXT,
    error_message VARCHAR(500),
    created_at TIMESTAMPTZ(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMPTZ(6)
);

ALTER TABLE ai_result_documents
    ADD COLUMN IF NOT EXISTS request_id UUID,
    ADD COLUMN IF NOT EXISTS patient_id UUID,
    ADD COLUMN IF NOT EXISTS requested_by_user_id UUID,
    ADD COLUMN IF NOT EXISTS question TEXT,
    ADD COLUMN IF NOT EXISTS question_type VARCHAR(40),
    ADD COLUMN IF NOT EXISTS job_status VARCHAR(20),
    ADD COLUMN IF NOT EXISTS result_status VARCHAR(20),
    ADD COLUMN IF NOT EXISTS title VARCHAR(200),
    ADD COLUMN IF NOT EXISTS content TEXT,
    ADD COLUMN IF NOT EXISTS citation_json TEXT,
    ADD COLUMN IF NOT EXISTS error_message VARCHAR(500),
    ADD COLUMN IF NOT EXISTS created_at TIMESTAMPTZ(6) DEFAULT CURRENT_TIMESTAMP,
    ADD COLUMN IF NOT EXISTS completed_at TIMESTAMPTZ(6);

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conrelid = 'ai_result_documents'::regclass
          AND conname = 'ai_result_documents_request_id_key'
    ) THEN
        ALTER TABLE ai_result_documents
            ADD CONSTRAINT ai_result_documents_request_id_key UNIQUE (request_id);
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conrelid = 'ai_result_documents'::regclass
          AND conname = 'ai_result_documents_job_status_check'
    ) THEN
        ALTER TABLE ai_result_documents
            ADD CONSTRAINT ai_result_documents_job_status_check
            CHECK (job_status IN ('QUEUED', 'RUNNING', 'SUCCEEDED', 'FAILED', 'CANCELED'));
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conrelid = 'ai_result_documents'::regclass
          AND conname = 'ai_result_documents_result_status_check'
    ) THEN
        ALTER TABLE ai_result_documents
            ADD CONSTRAINT ai_result_documents_result_status_check
            CHECK (result_status IS NULL OR result_status IN ('COMPLETE', 'PARTIAL', 'INSUFFICIENT'));
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conrelid = 'ai_result_documents'::regclass
          AND conname = 'ai_result_documents_patient_fk'
    ) THEN
        ALTER TABLE ai_result_documents
            ADD CONSTRAINT ai_result_documents_patient_fk
            FOREIGN KEY (patient_id) REFERENCES patients (id);
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conrelid = 'ai_result_documents'::regclass
          AND conname = 'ai_result_documents_requester_fk'
    ) THEN
        ALTER TABLE ai_result_documents
            ADD CONSTRAINT ai_result_documents_requester_fk
            FOREIGN KEY (requested_by_user_id) REFERENCES users (id);
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS ai_result_documents_patient_created_idx
    ON ai_result_documents (patient_id, created_at DESC);

ALTER TABLE chat_messages
    ADD COLUMN IF NOT EXISTS result_document_id UUID;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conrelid = 'chat_messages'::regclass
          AND conname = 'chat_messages_result_document_fk'
    ) THEN
        ALTER TABLE chat_messages
            ADD CONSTRAINT chat_messages_result_document_fk
            FOREIGN KEY (result_document_id) REFERENCES ai_result_documents (id);
    END IF;
END $$;

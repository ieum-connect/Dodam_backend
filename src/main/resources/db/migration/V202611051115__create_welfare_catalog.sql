-- 행정안전부 대한민국 공공서비스 API 원문과 검증된 추천 규칙을 분리해 저장한다.
-- 외부 서비스 삭제는 물리 삭제 대신 welfare_programs.active=false로 보존한다.

CREATE TABLE welfare_programs (
    id UUID PRIMARY KEY,
    external_service_id VARCHAR(100) NOT NULL,
    name VARCHAR(500) NOT NULL,
    support_type VARCHAR(100),
    purpose_summary TEXT,
    purpose_detail TEXT,
    target_text TEXT,
    selection_criteria_text TEXT,
    benefit_text TEXT,
    application_method TEXT,
    application_deadline TEXT,
    application_deadline_type VARCHAR(100),
    required_documents TEXT,
    official_check_documents TEXT,
    identity_check_documents TEXT,
    online_application_url TEXT,
    detail_url TEXT,
    agency_code VARCHAR(100),
    agency_name VARCHAR(500),
    agency_type VARCHAR(100),
    department_name VARCHAR(500),
    reception_agency VARCHAR(500),
    contact TEXT,
    service_category VARCHAR(200),
    user_category VARCHAR(200),
    view_count BIGINT,
    registered_at_source TIMESTAMPTZ(6),
    modified_at_source TIMESTAMPTZ(6),
    last_synced_at TIMESTAMPTZ(6) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    child_candidate BOOLEAN NOT NULL DEFAULT FALSE,
    raw_list_json JSONB NOT NULL DEFAULT '{}'::jsonb,
    raw_detail_json JSONB NOT NULL DEFAULT '{}'::jsonb,
    raw_condition_json JSONB NOT NULL DEFAULT '{}'::jsonb,
    created_at TIMESTAMPTZ(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT welfare_programs_external_service_id_key UNIQUE (external_service_id),
    CONSTRAINT welfare_programs_view_count_check CHECK (view_count IS NULL OR view_count >= 0)
);

CREATE INDEX welfare_programs_active_modified_idx
    ON welfare_programs (active, modified_at_source DESC, id);
CREATE INDEX welfare_programs_child_candidate_idx
    ON welfare_programs (child_candidate, active, modified_at_source DESC)
    WHERE child_candidate = TRUE AND active = TRUE;
CREATE INDEX welfare_programs_agency_idx
    ON welfare_programs (agency_name, id);

CREATE TABLE welfare_condition_code_map (
    id UUID PRIMARY KEY,
    condition_code VARCHAR(20) NOT NULL,
    display_name VARCHAR(200) NOT NULL,
    description TEXT,
    normalized_rule_type VARCHAR(30),
    value_mapping JSONB NOT NULL DEFAULT '{}'::jsonb,
    review_status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    reviewed_by_user_id UUID REFERENCES users(id),
    reviewed_at TIMESTAMPTZ(6),
    created_at TIMESTAMPTZ(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT welfare_condition_code_map_code_key UNIQUE (condition_code),
    CONSTRAINT welfare_condition_code_map_code_check CHECK (condition_code ~ '^JA[0-9]{4}$'),
    CONSTRAINT welfare_condition_code_map_rule_type_check CHECK (
        normalized_rule_type IS NULL OR normalized_rule_type IN
        ('AGE', 'REGION', 'INCOME', 'HOUSEHOLD', 'DISABILITY', 'EMPLOYMENT', 'CHILDCARE', 'INTEREST')
    ),
    CONSTRAINT welfare_condition_code_map_review_status_check CHECK (
        review_status IN ('PENDING', 'APPROVED', 'REJECTED')
    ),
    CONSTRAINT welfare_condition_code_map_review_check CHECK (
        (review_status = 'PENDING' AND reviewed_at IS NULL)
        OR (review_status IN ('APPROVED', 'REJECTED') AND reviewed_at IS NOT NULL)
    )
);

CREATE TABLE welfare_eligibility_rules (
    id UUID PRIMARY KEY,
    program_id UUID NOT NULL REFERENCES welfare_programs(id) ON DELETE CASCADE,
    rule_type VARCHAR(30) NOT NULL,
    operator VARCHAR(20) NOT NULL,
    value_json JSONB NOT NULL,
    required BOOLEAN NOT NULL DEFAULT FALSE,
    source_field VARCHAR(100) NOT NULL,
    source_code VARCHAR(20),
    confidence NUMERIC(4,3) NOT NULL DEFAULT 1.000,
    review_status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMPTZ(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT welfare_eligibility_rules_type_check CHECK (
        rule_type IN ('AGE', 'REGION', 'INCOME', 'HOUSEHOLD', 'DISABILITY', 'EMPLOYMENT', 'CHILDCARE', 'INTEREST')
    ),
    CONSTRAINT welfare_eligibility_rules_operator_check CHECK (
        operator IN ('EQ', 'IN', 'MIN', 'MAX', 'CONTAINS')
    ),
    CONSTRAINT welfare_eligibility_rules_confidence_check CHECK (confidence >= 0 AND confidence <= 1),
    CONSTRAINT welfare_eligibility_rules_review_status_check CHECK (
        review_status IN ('PENDING', 'APPROVED', 'REJECTED')
    )
);

CREATE INDEX welfare_eligibility_rules_program_idx
    ON welfare_eligibility_rules (program_id, review_status, rule_type);

CREATE TABLE welfare_sync_logs (
    id UUID PRIMARY KEY,
    program_id UUID REFERENCES welfare_programs(id) ON DELETE SET NULL,
    endpoint VARCHAR(50) NOT NULL,
    sync_type VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL,
    requested_page BIGINT,
    received_count BIGINT,
    error_code VARCHAR(100),
    error_message TEXT,
    started_at TIMESTAMPTZ(6) NOT NULL,
    finished_at TIMESTAMPTZ(6),
    CONSTRAINT welfare_sync_logs_endpoint_check CHECK (
        endpoint IN ('SERVICE_LIST', 'SERVICE_DETAIL', 'SUPPORT_CONDITIONS')
    ),
    CONSTRAINT welfare_sync_logs_sync_type_check CHECK (
        sync_type IN ('FULL', 'INCREMENTAL', 'DETAIL_REFRESH')
    ),
    CONSTRAINT welfare_sync_logs_status_check CHECK (
        status IN ('STARTED', 'SUCCEEDED', 'FAILED', 'PARTIAL')
    ),
    CONSTRAINT welfare_sync_logs_page_check CHECK (requested_page IS NULL OR requested_page >= 1),
    CONSTRAINT welfare_sync_logs_count_check CHECK (received_count IS NULL OR received_count >= 0),
    CONSTRAINT welfare_sync_logs_time_check CHECK (finished_at IS NULL OR finished_at >= started_at)
);

CREATE INDEX welfare_sync_logs_started_idx
    ON welfare_sync_logs (started_at DESC, id DESC);
CREATE INDEX welfare_sync_logs_program_idx
    ON welfare_sync_logs (program_id, started_at DESC)
    WHERE program_id IS NOT NULL;

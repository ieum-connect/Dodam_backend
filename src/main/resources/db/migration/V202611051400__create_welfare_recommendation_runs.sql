CREATE TABLE welfare_recommendation_runs (
    id UUID PRIMARY KEY,
    patient_id UUID NOT NULL REFERENCES patients(id) ON DELETE CASCADE,
    profile_version BIGINT,
    catalog_synced_at TIMESTAMPTZ(6),
    profile_snapshot JSONB NOT NULL,
    created_at TIMESTAMPTZ(6) NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE welfare_recommendation_items (
    id UUID PRIMARY KEY,
    run_id UUID NOT NULL REFERENCES welfare_recommendation_runs(id) ON DELETE CASCADE,
    program_id UUID NOT NULL REFERENCES welfare_programs(id) ON DELETE CASCADE,
    recommendation_type VARCHAR(20) NOT NULL,
    eligibility_status VARCHAR(20) NOT NULL,
    score NUMERIC(5,2) NOT NULL,
    matched_conditions JSONB NOT NULL DEFAULT '[]'::jsonb,
    missing_conditions JSONB NOT NULL DEFAULT '[]'::jsonb,
    created_at TIMESTAMPTZ(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT welfare_recommendation_items_type_check CHECK (recommendation_type IN ('PROACTIVE','SUGGESTIVE')),
    CONSTRAINT welfare_recommendation_items_status_check CHECK (eligibility_status IN ('ELIGIBLE','POSSIBLE','INELIGIBLE','EXPIRED')),
    CONSTRAINT welfare_recommendation_items_score_check CHECK (score >= 0 AND score <= 100)
);
CREATE INDEX welfare_recommendation_runs_patient_idx ON welfare_recommendation_runs(patient_id, created_at DESC);
CREATE INDEX welfare_recommendation_items_run_idx ON welfare_recommendation_items(run_id, score DESC);

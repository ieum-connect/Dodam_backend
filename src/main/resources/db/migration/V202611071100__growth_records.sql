CREATE TABLE growth_records (
    id UUID PRIMARY KEY,
    patient_id UUID NOT NULL REFERENCES patients(id),
    author_user_id UUID NOT NULL REFERENCES users(id),
    measured_on DATE NOT NULL,
    height_cm NUMERIC(4,1),
    weight_kg NUMERIC(4,1),
    note VARCHAR(500),
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    deleted_at TIMESTAMPTZ,
    version BIGINT NOT NULL DEFAULT 0,
    CHECK (height_cm IS NOT NULL OR weight_kg IS NOT NULL),
    CHECK (height_cm IS NULL OR height_cm BETWEEN 0.1 AND 300.0),
    CHECK (weight_kg IS NULL OR weight_kg BETWEEN 0.1 AND 500.0)
);
CREATE UNIQUE INDEX ux_growth_patient_day ON growth_records(patient_id, measured_on) WHERE deleted_at IS NULL;

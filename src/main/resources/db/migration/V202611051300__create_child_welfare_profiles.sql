CREATE TABLE child_welfare_profiles (
    id UUID PRIMARY KEY,
    patient_id UUID NOT NULL UNIQUE REFERENCES patients(id) ON DELETE CASCADE,
    storage_consent BOOLEAN NOT NULL,
    consented_at TIMESTAMPTZ(6),
    profile_version BIGINT NOT NULL DEFAULT 1,
    region_code VARCHAR(20),
    household_size INTEGER,
    income_band VARCHAR(50),
    child_count INTEGER,
    is_single_parent BOOLEAN,
    is_multicultural BOOLEAN,
    has_disability BOOLEAN,
    is_dual_income BOOLEAN,
    childcare_status VARCHAR(50),
    health_data_consent BOOLEAN NOT NULL DEFAULT FALSE,
    updated_at TIMESTAMPTZ(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT child_welfare_profiles_consent_time_check CHECK (
        (storage_consent = TRUE AND consented_at IS NOT NULL) OR
        (storage_consent = FALSE AND consented_at IS NULL)
    )
);

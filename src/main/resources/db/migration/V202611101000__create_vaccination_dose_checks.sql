-- Doses a caregiver marked as already given. The AI server computes the schedule from these ids.
CREATE TABLE vaccination_dose_checks (
    id UUID PRIMARY KEY,
    patient_id UUID NOT NULL REFERENCES patients(id) ON DELETE CASCADE,
    dose_id VARCHAR(50) NOT NULL,
    checked_by UUID REFERENCES users(id) ON DELETE SET NULL,
    created_at TIMESTAMPTZ(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT vaccination_dose_checks_patient_dose_key UNIQUE (patient_id, dose_id)
);

CREATE TABLE welfare_favorites (
    id UUID PRIMARY KEY,
    patient_id UUID NOT NULL REFERENCES patients(id) ON DELETE CASCADE,
    program_id UUID NOT NULL REFERENCES welfare_programs(id) ON DELETE CASCADE,
    created_at TIMESTAMPTZ(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT welfare_favorites_patient_program_key UNIQUE (patient_id, program_id)
);

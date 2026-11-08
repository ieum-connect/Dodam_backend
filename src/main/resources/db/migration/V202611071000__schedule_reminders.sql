ALTER TABLE schedules ADD COLUMN notes VARCHAR(500);
ALTER TABLE schedules ADD COLUMN reminded_at TIMESTAMPTZ;
ALTER TABLE schedules ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
UPDATE schedules SET reminded_at = CURRENT_TIMESTAMP WHERE starts_at < CURRENT_TIMESTAMP;
CREATE INDEX schedules_due_idx ON schedules(starts_at) WHERE status = 'SCHEDULED' AND reminded_at IS NULL;
CREATE TABLE schedule_batches (
    id UUID PRIMARY KEY,
    patient_id UUID NOT NULL REFERENCES patients(id),
    fingerprint VARCHAR(64) NOT NULL
);
ALTER TABLE schedules ADD COLUMN batch_id UUID REFERENCES schedule_batches(id);
CREATE INDEX schedules_batch_idx ON schedules(batch_id);

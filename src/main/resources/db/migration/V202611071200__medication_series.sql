CREATE TABLE medication_series (
 id UUID PRIMARY KEY, patient_id UUID NOT NULL REFERENCES patients(id), title VARCHAR(200) NOT NULL,
 notes VARCHAR(500), start_date DATE NOT NULL, time_of_day TIME NOT NULL, time_zone VARCHAR(80) NOT NULL,
 end_date DATE, next_date DATE NOT NULL, active BOOLEAN NOT NULL DEFAULT TRUE, version BIGINT NOT NULL DEFAULT 0,
 CHECK(end_date IS NULL OR end_date>=start_date)
);
CREATE INDEX medication_series_patient_idx ON medication_series(patient_id);
ALTER TABLE schedules ADD COLUMN series_id UUID REFERENCES medication_series(id);
ALTER TABLE schedules ADD COLUMN occurrence_date DATE;
CREATE UNIQUE INDEX schedule_series_occurrence_idx ON schedules(series_id,occurrence_date);

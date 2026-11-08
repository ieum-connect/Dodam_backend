CREATE TABLE memo_tags (
 id UUID PRIMARY KEY, patient_id UUID NOT NULL REFERENCES patients(id), name VARCHAR(20) NOT NULL,
 deleted_at TIMESTAMPTZ, version BIGINT NOT NULL DEFAULT 0
);
CREATE UNIQUE INDEX memo_tags_patient_name_idx ON memo_tags(patient_id,name) WHERE deleted_at IS NULL;
CREATE TABLE health_record_tags (
 record_id UUID NOT NULL REFERENCES health_records(id),tag_id UUID NOT NULL REFERENCES memo_tags(id),PRIMARY KEY(record_id,tag_id)
);
CREATE INDEX health_record_tags_tag_idx ON health_record_tags(tag_id);

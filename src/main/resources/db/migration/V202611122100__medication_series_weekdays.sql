ALTER TABLE medication_series
    ADD COLUMN weekday_mask INTEGER NOT NULL DEFAULT 127 CHECK (weekday_mask BETWEEN 1 AND 127),
    ADD COLUMN rule_from DATE;

UPDATE medication_series SET rule_from = start_date;
ALTER TABLE medication_series ALTER COLUMN rule_from SET NOT NULL;

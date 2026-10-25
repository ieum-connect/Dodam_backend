ALTER TABLE patients ADD COLUMN IF NOT EXISTS child_name VARCHAR(100);
ALTER TABLE patients DROP CONSTRAINT IF EXISTS patients_user_id_key;
UPDATE patients SET child_name = '이름 확인 필요' WHERE child_name IS NULL;
-- Preserve existing records while granting their account owner the same parent access.
INSERT INTO caregiver_links (id, patient_id, caregiver_user_id, status, requested_at, accepted_at)
SELECT gen_random_uuid(), p.id, p.user_id, 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM patients p
WHERE NOT EXISTS (
    SELECT 1 FROM caregiver_links c
    WHERE c.patient_id = p.id AND c.caregiver_user_id = p.user_id AND c.status = 'ACTIVE'
);
UPDATE users AS patient_user
SET role = 'CAREGIVER'
WHERE patient_user.role = 'PATIENT'
  AND NOT EXISTS (
      SELECT 1
      FROM users AS caregiver_user
      WHERE caregiver_user.account_group_id = patient_user.account_group_id
        AND caregiver_user.role = 'CAREGIVER'
        AND caregiver_user.status = 'ACTIVE'
  );
-- Existing patient rows stay readable during migration; new child profiles require a name in the API.
CREATE INDEX IF NOT EXISTS caregiver_links_active_parent_idx
    ON caregiver_links (caregiver_user_id, patient_id) WHERE status = 'ACTIVE';

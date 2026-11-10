-- Directly entered schedules do not have an AI candidate.
ALTER TABLE schedules ALTER COLUMN action_item_id DROP NOT NULL;
CREATE UNIQUE INDEX schedules_action_item_unique ON schedules(action_item_id);

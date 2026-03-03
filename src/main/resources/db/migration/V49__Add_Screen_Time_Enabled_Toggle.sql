ALTER TABLE screen_time_budgets 
  ADD COLUMN IF NOT EXISTS screen_time_enabled BOOLEAN NOT NULL DEFAULT TRUE;
ALTER TABLE screen_time_budgets 
  ADD COLUMN IF NOT EXISTS screen_time_controlled_by BIGINT;
ALTER TABLE screen_time_budgets 
  ADD COLUMN IF NOT EXISTS screen_time_control_locked BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE screen_time_budgets 
  ADD CONSTRAINT fk_screen_time_controller 
  FOREIGN KEY (screen_time_controlled_by) REFERENCES users(id) ON DELETE SET NULL;

CREATE INDEX idx_screen_time_controlled_by ON screen_time_budgets(screen_time_controlled_by);

-- Auto-set for existing child accounts
UPDATE screen_time_budgets stb
SET screen_time_controlled_by = ups.parent_user_id,
    screen_time_control_locked = TRUE
FROM user_parental_settings ups
WHERE stb.user_id = ups.user_id
  AND ups.is_child_account = TRUE
  AND ups.parent_user_id IS NOT NULL;

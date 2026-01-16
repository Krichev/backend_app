-- Add is_active column to quests table for soft delete support
ALTER TABLE quests 
    ADD COLUMN IF NOT EXISTS is_active BOOLEAN NOT NULL DEFAULT TRUE;

-- Create index for active quests filtering
CREATE INDEX IF NOT EXISTS idx_quests_is_active ON quests(is_active);

COMMENT ON COLUMN quests.is_active IS 'Soft delete flag (TRUE = active, FALSE = deleted)';

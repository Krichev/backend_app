-- ============================================================================
-- Flyway Migration V43: Add version column to account_lock_configs
-- ============================================================================
-- Description: Adds missing 'version' column for JPA optimistic locking
--              Entity AccountLockConfig has @Version annotation
-- ============================================================================

ALTER TABLE account_lock_configs 
ADD COLUMN IF NOT EXISTS version BIGINT DEFAULT 0;

COMMENT ON COLUMN account_lock_configs.version IS 'Optimistic locking version for JPA @Version';

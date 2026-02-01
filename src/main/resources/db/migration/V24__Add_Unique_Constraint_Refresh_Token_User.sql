-- ============================================================================
-- Migration V24: Enforce single refresh token per user
-- ============================================================================
-- Description: Adds a UNIQUE constraint on refresh_tokens.user_id to prevent
--              multiple refresh tokens per user at the database level.
--              First cleans up any existing duplicates (keeps newest per user).
-- ============================================================================

-- Step 1: Remove duplicate refresh tokens (keep only the newest per user)
DELETE FROM refresh_tokens rt1
USING refresh_tokens rt2
WHERE rt1.user_id = rt2.user_id
  AND rt1.id < rt2.id;

-- Step 2: Add unique constraint on user_id
ALTER TABLE refresh_tokens
    ADD CONSTRAINT uq_refresh_tokens_user_id UNIQUE (user_id);

-- Step 3: Drop the now-redundant plain index (unique constraint creates its own)
DROP INDEX IF EXISTS idx_refresh_token_user_id;

-- Step 4: Add documentation
COMMENT ON CONSTRAINT uq_refresh_tokens_user_id ON refresh_tokens IS 'Ensures one refresh token per user';

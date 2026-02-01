-- ============================================================================
-- Add sound effects and vibration toggles to user_app_settings
-- ============================================================================
-- These columns were expected by the service layer but missing from the entity
-- and database table. Both default to TRUE (enabled).

ALTER TABLE user_app_settings
ADD COLUMN IF NOT EXISTS enable_sound_effects BOOLEAN DEFAULT TRUE;

ALTER TABLE user_app_settings
ADD COLUMN IF NOT EXISTS enable_vibration BOOLEAN DEFAULT TRUE;

COMMENT ON COLUMN user_app_settings.enable_sound_effects IS 'Whether sound effects are enabled in the app';
COMMENT ON COLUMN user_app_settings.enable_vibration IS 'Whether haptic/vibration feedback is enabled in the app';

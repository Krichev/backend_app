-- ============================================================================
-- User Application Settings Table
-- Stores user preferences: language, theme, notification settings
-- ============================================================================

CREATE TABLE IF NOT EXISTS user_app_settings (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE,
    language VARCHAR(5) NOT NULL DEFAULT 'en',
    theme VARCHAR(10) NOT NULL DEFAULT 'system',
    notifications_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT fk_app_settings_user FOREIGN KEY (user_id) 
        REFERENCES users(id) ON DELETE CASCADE
);

-- Index for faster lookups by user_id
CREATE INDEX IF NOT EXISTS idx_user_app_settings_user_id ON user_app_settings(user_id);

-- Check constraints for valid values
ALTER TABLE user_app_settings 
    ADD CONSTRAINT chk_app_settings_language CHECK (language IN ('en', 'ru'));

ALTER TABLE user_app_settings 
    ADD CONSTRAINT chk_app_settings_theme CHECK (theme IN ('light', 'dark', 'system'));

-- Documentation comments
COMMENT ON TABLE user_app_settings IS 'Stores user application preferences like language and theme';
COMMENT ON COLUMN user_app_settings.language IS 'User preferred language: en (English) or ru (Russian)';
COMMENT ON COLUMN user_app_settings.theme IS 'User preferred theme: light, dark, or system';
COMMENT ON COLUMN user_app_settings.notifications_enabled IS 'Whether push notifications are enabled';

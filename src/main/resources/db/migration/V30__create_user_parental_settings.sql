-- Create user_parental_settings table for parental control functionality
CREATE TABLE IF NOT EXISTS user_parental_settings (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE,
    is_child_account BOOLEAN NOT NULL DEFAULT false,
    parent_user_id BIGINT,
    age_group VARCHAR(20) DEFAULT 'ADULT',
    content_restriction_level VARCHAR(20) DEFAULT 'NONE',
    require_parent_approval BOOLEAN NOT NULL DEFAULT false,
    allowed_topic_categories TEXT,
    blocked_topic_categories TEXT,
    max_daily_screen_time_minutes INTEGER,
    max_daily_quiz_count INTEGER,
    parent_pin_hash VARCHAR(255),
    last_parent_verification TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT fk_parental_settings_user 
        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_parental_parent_user 
        FOREIGN KEY (parent_user_id) REFERENCES users(id) ON DELETE SET NULL,
    CONSTRAINT chk_age_group 
        CHECK (age_group IN ('UNDER_13', '13_TO_17', 'ADULT')),
    CONSTRAINT chk_content_restriction 
        CHECK (content_restriction_level IN ('STRICT', 'MODERATE', 'NONE'))
);

-- Create indexes for efficient queries
CREATE INDEX idx_parental_parent_user ON user_parental_settings(parent_user_id);
CREATE INDEX idx_parental_is_child ON user_parental_settings(is_child_account);

-- Add comment for documentation
COMMENT ON TABLE user_parental_settings IS 'Stores parental control settings including child account linking and content restrictions';

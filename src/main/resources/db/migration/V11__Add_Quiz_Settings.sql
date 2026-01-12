-- ============================================================================
-- V11__Add_Quiz_Settings.sql
-- Enhanced Quiz Configuration
-- ============================================================================

-- New enum for result sharing policy
CREATE TYPE result_sharing_policy AS ENUM (
    'CREATOR_ONLY',
    'PARTICIPANTS_ONLY', 
    'ALL_PARTICIPANTS',
    'PUBLIC',
    'NONE'
);

-- New enum for consent status
CREATE TYPE participant_consent_status AS ENUM (
    'NOT_ASKED',
    'PENDING',
    'GRANTED',
    'DENIED'
);

-- Add new columns to challenges table
ALTER TABLE challenges 
ADD COLUMN IF NOT EXISTS max_participants INTEGER,
ADD COLUMN IF NOT EXISTS current_participant_count INTEGER DEFAULT 0,
ADD COLUMN IF NOT EXISTS allow_open_enrollment BOOLEAN DEFAULT true,
ADD COLUMN IF NOT EXISTS enrollment_deadline TIMESTAMP WITH TIME ZONE;

-- Create quiz participant settings table
CREATE TABLE IF NOT EXISTS quiz_participant_settings (
    id                      BIGSERIAL PRIMARY KEY,
    challenge_id            BIGINT NOT NULL,
    user_id                 BIGINT NOT NULL,
    result_consent_status   participant_consent_status DEFAULT 'NOT_ASKED',
    consent_requested_at    TIMESTAMP WITH TIME ZONE,
    consent_responded_at    TIMESTAMP WITH TIME ZONE,
    attempts_used           INTEGER DEFAULT 0,
    last_attempt_at         TIMESTAMP WITH TIME ZONE,
    best_score              DECIMAL(5,2),
    created_at              TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at              TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT fk_quiz_settings_challenge 
        FOREIGN KEY (challenge_id) REFERENCES challenges(id) ON DELETE CASCADE,
    CONSTRAINT fk_quiz_settings_user 
        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT uq_quiz_participant 
        UNIQUE(challenge_id, user_id)
);

-- Indexes for performance
CREATE INDEX IF NOT EXISTS idx_quiz_participant_challenge 
    ON quiz_participant_settings(challenge_id);
CREATE INDEX IF NOT EXISTS idx_quiz_participant_user 
    ON quiz_participant_settings(user_id);
CREATE INDEX IF NOT EXISTS idx_quiz_participant_consent 
    ON quiz_participant_settings(challenge_id, result_consent_status);

-- Comments
COMMENT ON TABLE quiz_participant_settings IS 'Per-participant settings for quizzes including consent and attempt tracking';
COMMENT ON COLUMN challenges.max_participants IS 'Maximum number of participants allowed (NULL = unlimited)';
COMMENT ON COLUMN challenges.allow_open_enrollment IS 'If true, anyone with access can join without pre-registration';

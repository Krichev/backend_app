-- ============================================================================
-- Migration: Add Audio Challenge Type Support
-- ============================================================================

-- 1. Create audio_challenge_type enum
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'audio_challenge_type') THEN
        CREATE TYPE audio_challenge_type AS ENUM (
            'RHYTHM_CREATION',
            'RHYTHM_REPEAT',
            'SOUND_MATCH',
            'SINGING'
        );
    END IF;
END$$;

COMMENT ON TYPE audio_challenge_type IS 'Types of audio-based challenges';

-- 2. Add audio challenge columns to quiz_questions table
ALTER TABLE quiz_questions
    ADD COLUMN IF NOT EXISTS audio_challenge_type audio_challenge_type,
    ADD COLUMN IF NOT EXISTS audio_reference_media_id BIGINT,
    ADD COLUMN IF NOT EXISTS audio_segment_start DOUBLE PRECISION DEFAULT 0.0,
    ADD COLUMN IF NOT EXISTS audio_segment_end DOUBLE PRECISION,
    ADD COLUMN IF NOT EXISTS minimum_score_percentage INTEGER DEFAULT 60,
    ADD COLUMN IF NOT EXISTS rhythm_bpm INTEGER,
    ADD COLUMN IF NOT EXISTS rhythm_time_signature VARCHAR(10),
    ADD COLUMN IF NOT EXISTS audio_challenge_config JSONB;

-- 3. Add foreign key constraint for audio reference media
ALTER TABLE quiz_questions
    ADD CONSTRAINT fk_quiz_question_audio_reference
    FOREIGN KEY (audio_reference_media_id)
    REFERENCES media_files(id)
    ON DELETE SET NULL;

-- 4. Add indexes for audio challenge queries
CREATE INDEX IF NOT EXISTS idx_quiz_questions_audio_challenge_type
    ON quiz_questions(audio_challenge_type)
    WHERE audio_challenge_type IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_quiz_questions_audio_reference
    ON quiz_questions(audio_reference_media_id)
    WHERE audio_reference_media_id IS NOT NULL;

-- 5. Create audio_challenge_submissions table for tracking user submissions
CREATE TABLE IF NOT EXISTS audio_challenge_submissions (
    id BIGSERIAL PRIMARY KEY,
    question_id BIGINT NOT NULL REFERENCES quiz_questions(id) ON DELETE CASCADE,
    user_id BIGINT NOT NULL,
    submission_audio_path TEXT NOT NULL,
    processing_status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    processing_progress INTEGER DEFAULT 0,

    -- Scoring results
    overall_score DOUBLE PRECISION,
    pitch_score DOUBLE PRECISION,
    rhythm_score DOUBLE PRECISION,
    voice_score DOUBLE PRECISION,
    detailed_metrics JSONB,

    -- Timestamps
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    processed_at TIMESTAMP WITH TIME ZONE,

    CONSTRAINT check_processing_status CHECK (
        processing_status IN ('PENDING', 'PROCESSING', 'COMPLETED', 'FAILED')
    ),
    CONSTRAINT check_processing_progress CHECK (processing_progress BETWEEN 0 AND 100)
);

CREATE INDEX idx_audio_submissions_question ON audio_challenge_submissions(question_id);
CREATE INDEX idx_audio_submissions_user ON audio_challenge_submissions(user_id);
CREATE INDEX idx_audio_submissions_status ON audio_challenge_submissions(processing_status);

-- 6. Add constraint for audio question type consistency
ALTER TABLE quiz_questions DROP CONSTRAINT IF EXISTS chk_audio_challenge_consistency;
ALTER TABLE quiz_questions
    ADD CONSTRAINT chk_audio_challenge_consistency
    CHECK (
        (question_type != 'AUDIO' OR audio_challenge_type IS NOT NULL) AND
        (audio_challenge_type IS NULL OR question_type = 'AUDIO')
    );

-- 7. Add comments
COMMENT ON COLUMN quiz_questions.audio_challenge_type IS 'Type of audio challenge: RHYTHM_CREATION, RHYTHM_REPEAT, SOUND_MATCH, SINGING';
COMMENT ON COLUMN quiz_questions.audio_reference_media_id IS 'Reference audio file for the challenge';
COMMENT ON COLUMN quiz_questions.audio_segment_start IS 'Start time in seconds for audio segment';
COMMENT ON COLUMN quiz_questions.audio_segment_end IS 'End time in seconds for audio segment (null = full duration)';
COMMENT ON COLUMN quiz_questions.minimum_score_percentage IS 'Minimum score (0-100) required to pass the challenge';
COMMENT ON COLUMN quiz_questions.rhythm_bpm IS 'BPM for rhythm-based challenges';
COMMENT ON COLUMN quiz_questions.rhythm_time_signature IS 'Time signature for rhythm challenges (e.g., 4/4, 3/4)';
COMMENT ON COLUMN quiz_questions.audio_challenge_config IS 'Additional JSON configuration for audio challenges';
COMMENT ON TABLE audio_challenge_submissions IS 'User submissions for audio-based challenges with scoring results';

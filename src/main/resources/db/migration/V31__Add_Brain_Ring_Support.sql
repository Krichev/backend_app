-- ============================================================================
-- Flyway Migration V31: Add Brain Ring Support
-- ============================================================================
-- Description: Adds support for Brain Ring game mode, including new enums,
--              columns for quiz sessions, and state tracking for brain ring rounds.
-- ============================================================================

-- Add game_mode enum if not exists
DO $$ BEGIN
    CREATE TYPE game_mode AS ENUM ('STANDARD', 'BRAIN_RING', 'BLITZ');
EXCEPTION
    WHEN duplicate_object THEN null;
END $$;

-- Add brain ring round status enum
DO $$ BEGIN
    CREATE TYPE brain_ring_round_status AS ENUM (
        'WAITING_FOR_BUZZ', 
        'PLAYER_ANSWERING', 
        'CORRECT_ANSWER', 
        'ALL_LOCKED_OUT'
    );
EXCEPTION
    WHEN duplicate_object THEN null;
END $$;

-- Add columns to quiz_sessions
ALTER TABLE quiz_sessions ADD COLUMN IF NOT EXISTS game_mode game_mode DEFAULT 'STANDARD';
ALTER TABLE quiz_sessions ADD COLUMN IF NOT EXISTS answer_time_seconds INTEGER DEFAULT 20;

-- Create brain ring round state table
CREATE TABLE IF NOT EXISTS brain_ring_round_state (
    id BIGSERIAL PRIMARY KEY,
    quiz_round_id BIGINT NOT NULL UNIQUE REFERENCES quiz_rounds(id) ON DELETE CASCADE,
    current_buzzer_user_id BIGINT REFERENCES users(id),
    buzzer_timestamp TIMESTAMP WITH TIME ZONE,
    answer_deadline TIMESTAMP WITH TIME ZONE,
    locked_out_players JSONB DEFAULT '[]'::jsonb,
    buzz_order JSONB DEFAULT '[]'::jsonb,
    round_status brain_ring_round_status DEFAULT 'WAITING_FOR_BUZZ',
    winner_user_id BIGINT REFERENCES users(id),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_brain_ring_round_state_round ON brain_ring_round_state(quiz_round_id);
CREATE INDEX IF NOT EXISTS idx_brain_ring_round_state_status ON brain_ring_round_state(round_status);

COMMENT ON TABLE brain_ring_round_state IS 'Tracks Brain Ring game mode state per round';

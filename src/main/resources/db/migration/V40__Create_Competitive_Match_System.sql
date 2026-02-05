-- ============================================================================
-- Flyway Migration V40: Create Competitive Match System
-- ============================================================================
-- Description: Creates the core tables and enums for the 1v1 Competitive
--              Karaoke Match System.
--
-- Version: V40
-- Author: Gemini CLI
-- Date: 2026-02-05
-- ============================================================================

-- Create ENUM types using DO blocks to handle existence checks safely

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'competitive_match_status') THEN
        CREATE TYPE competitive_match_status AS ENUM (
            'WAITING_FOR_OPPONENT',
            'READY',
            'IN_PROGRESS',
            'ROUND_COMPLETE',
            'COMPLETED',
            'CANCELLED',
            'EXPIRED'
        );
        COMMENT ON TYPE competitive_match_status IS 'Lifecycle status of a competitive match';
    END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'competitive_match_type') THEN
        CREATE TYPE competitive_match_type AS ENUM (
            'FRIEND_CHALLENGE',
            'RANDOM_MATCHMAKING'
        );
        COMMENT ON TYPE competitive_match_type IS 'Type of competitive match (friend vs random)';
    END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'competitive_round_status') THEN
        CREATE TYPE competitive_round_status AS ENUM (
            'PENDING',
            'PLAYER1_PERFORMING',
            'PLAYER2_PERFORMING',
            'SCORING',
            'COMPLETED'
        );
        COMMENT ON TYPE competitive_round_status IS 'Status of a specific round within a match';
    END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'matchmaking_status') THEN
        CREATE TYPE matchmaking_status AS ENUM (
            'QUEUED',
            'MATCHED',
            'EXPIRED',
            'CANCELLED'
        );
        COMMENT ON TYPE matchmaking_status IS 'Status of a user in the matchmaking queue';
    END IF;
END $$;

-- ============================================================================
-- COMPETITIVE_MATCHES TABLE
-- ============================================================================

CREATE TABLE competitive_matches
(
    id                   BIGSERIAL PRIMARY KEY,
    match_type           competitive_match_type   NOT NULL,
    status               competitive_match_status NOT NULL DEFAULT 'WAITING_FOR_OPPONENT',
    player1_id           BIGINT                   NOT NULL,
    player2_id           BIGINT,
    winner_id            BIGINT,
    total_rounds         INTEGER                  NOT NULL DEFAULT 1,
    current_round        INTEGER                  NOT NULL DEFAULT 0,
    player1_total_score  DECIMAL(10, 2)           DEFAULT 0,
    player2_total_score  DECIMAL(10, 2)           DEFAULT 0,
    player1_rounds_won   INTEGER                  DEFAULT 0,
    player2_rounds_won   INTEGER                  DEFAULT 0,
    wager_id             BIGINT,
    audio_challenge_type VARCHAR(50)              DEFAULT 'SINGING',
    metadata             JSONB,
    expires_at           TIMESTAMP WITH TIME ZONE,
    started_at           TIMESTAMP WITH TIME ZONE,
    completed_at         TIMESTAMP WITH TIME ZONE,
    created_at           TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at           TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,

    -- Constraints
    CONSTRAINT fk_competitive_matches_player1 FOREIGN KEY (player1_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_competitive_matches_player2 FOREIGN KEY (player2_id) REFERENCES users (id) ON DELETE SET NULL,
    CONSTRAINT fk_competitive_matches_winner FOREIGN KEY (winner_id) REFERENCES users (id) ON DELETE SET NULL,
    CONSTRAINT fk_competitive_matches_wager FOREIGN KEY (wager_id) REFERENCES wagers (id) ON DELETE SET NULL
);

COMMENT ON TABLE competitive_matches IS 'Core match table for 1v1 karaoke competitions';
CREATE INDEX idx_competitive_matches_player1 ON competitive_matches (player1_id);
CREATE INDEX idx_competitive_matches_player2 ON competitive_matches (player2_id);
CREATE INDEX idx_competitive_matches_status ON competitive_matches (status);
CREATE INDEX idx_competitive_matches_type_status ON competitive_matches (match_type, status);

-- ============================================================================
-- COMPETITIVE_MATCH_ROUNDS TABLE
-- ============================================================================

CREATE TABLE competitive_match_rounds
(
    id                      BIGSERIAL PRIMARY KEY,
    match_id                BIGINT                   NOT NULL,
    round_number            INTEGER                  NOT NULL,
    status                  competitive_round_status NOT NULL DEFAULT 'PENDING',
    question_id             BIGINT,
    player1_score           DECIMAL(10, 2),
    player1_pitch_score     DECIMAL(5, 2),
    player1_rhythm_score    DECIMAL(5, 2),
    player1_voice_score     DECIMAL(5, 2),
    player1_submission_path VARCHAR(500),
    player1_submitted_at    TIMESTAMP WITH TIME ZONE,
    player2_score           DECIMAL(10, 2),
    player2_pitch_score     DECIMAL(5, 2),
    player2_rhythm_score    DECIMAL(5, 2),
    player2_voice_score     DECIMAL(5, 2),
    player2_submission_path VARCHAR(500),
    player2_submitted_at    TIMESTAMP WITH TIME ZONE,
    round_winner_id         BIGINT,
    started_at              TIMESTAMP WITH TIME ZONE,
    completed_at            TIMESTAMP WITH TIME ZONE,

    -- Constraints
    CONSTRAINT fk_match_rounds_match FOREIGN KEY (match_id) REFERENCES competitive_matches (id) ON DELETE CASCADE,
    CONSTRAINT fk_match_rounds_question FOREIGN KEY (question_id) REFERENCES quiz_questions (id) ON DELETE SET NULL,
    CONSTRAINT fk_match_rounds_winner FOREIGN KEY (round_winner_id) REFERENCES users (id) ON DELETE SET NULL,
    CONSTRAINT uk_match_round UNIQUE (match_id, round_number)
);

COMMENT ON TABLE competitive_match_rounds IS 'Individual round data including detailed scoring';
CREATE INDEX idx_match_rounds_match ON competitive_match_rounds (match_id);

-- ============================================================================
-- MATCHMAKING_QUEUE TABLE
-- ============================================================================

CREATE TABLE matchmaking_queue
(
    id                   BIGSERIAL PRIMARY KEY,
    user_id              BIGINT             NOT NULL,
    status               matchmaking_status NOT NULL DEFAULT 'QUEUED',
    audio_challenge_type VARCHAR(50)        NOT NULL,
    preferred_rounds     INTEGER            NOT NULL DEFAULT 1,
    skill_rating         INTEGER,
    matched_with_user_id BIGINT,
    matched_match_id     BIGINT,
    queued_at            TIMESTAMP WITH TIME ZONE        DEFAULT CURRENT_TIMESTAMP,
    matched_at           TIMESTAMP WITH TIME ZONE,
    expires_at           TIMESTAMP WITH TIME ZONE NOT NULL,

    -- Constraints
    CONSTRAINT fk_matchmaking_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_matchmaking_matched_user FOREIGN KEY (matched_with_user_id) REFERENCES users (id) ON DELETE SET NULL,
    CONSTRAINT fk_matchmaking_match FOREIGN KEY (matched_match_id) REFERENCES competitive_matches (id) ON DELETE SET NULL,
    CONSTRAINT uk_matchmaking_user UNIQUE (user_id)
);

COMMENT ON TABLE matchmaking_queue IS 'Queue for random matchmaking';
CREATE INDEX idx_matchmaking_queue_status ON matchmaking_queue (status, audio_challenge_type);

-- ============================================================================
-- COMPETITIVE_MATCH_INVITATIONS TABLE
-- ============================================================================

CREATE TABLE competitive_match_invitations
(
    id           BIGSERIAL PRIMARY KEY,
    match_id     BIGINT                   NOT NULL,
    inviter_id   BIGINT                   NOT NULL,
    invitee_id   BIGINT                   NOT NULL,
    status       VARCHAR(20)              DEFAULT 'PENDING',
    message      TEXT,
    expires_at   TIMESTAMP WITH TIME ZONE NOT NULL,
    responded_at TIMESTAMP WITH TIME ZONE,
    created_at   TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,

    -- Constraints
    CONSTRAINT fk_invitations_match FOREIGN KEY (match_id) REFERENCES competitive_matches (id) ON DELETE CASCADE,
    CONSTRAINT fk_invitations_inviter FOREIGN KEY (inviter_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_invitations_invitee FOREIGN KEY (invitee_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT uk_match_invitation UNIQUE (match_id, invitee_id)
);

COMMENT ON TABLE competitive_match_invitations IS 'Invitations for friend challenges';
CREATE INDEX idx_match_invitations_invitee ON competitive_match_invitations (invitee_id, status);

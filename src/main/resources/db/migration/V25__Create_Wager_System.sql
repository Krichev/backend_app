-- ============================================================================
-- Flyway Migration V25: Create Wager System
-- ============================================================================
-- Description: Creates the core tables and enums for the Wager-Based
--              Rewards & Punishments system.
--
-- Version: V25
-- Author: Gemini CLI
-- Date: 2026-02-01
-- ============================================================================

-- Create ENUM types for the wager system
CREATE TYPE stake_type AS ENUM (
    'POINTS',
    'SCREEN_TIME',
    'MONEY',
    'SOCIAL_QUEST'
);
COMMENT ON TYPE stake_type IS 'Type of stake being wagered';

CREATE TYPE wager_type AS ENUM (
    'HEAD_TO_HEAD',
    'GROUP_POT',
    'SIDE_BET'
);
COMMENT ON TYPE wager_type IS 'Structure of the wager (1v1, group pot, or side bet)';

CREATE TYPE wager_status AS ENUM (
    'PROPOSED',
    'ACCEPTED',
    'ACTIVE',
    'SETTLED',
    'CANCELLED',
    'EXPIRED'
);
COMMENT ON TYPE wager_status IS 'Lifecycle status of a wager';

CREATE TYPE participant_wager_status AS ENUM (
    'INVITED',
    'ACCEPTED',
    'DECLINED',
    'WON',
    'LOST',
    'DRAW'
);
COMMENT ON TYPE participant_wager_status IS 'Status of a participant in a wager';

CREATE TYPE settlement_type AS ENUM (
    'WINNER_TAKES_ALL',
    'PROPORTIONAL',
    'DRAW_REFUND'
);
COMMENT ON TYPE settlement_type IS 'How the wager is settled among winners';

-- ============================================================================
-- WAGERS TABLE
-- ============================================================================

CREATE TABLE wagers
(
    id                         BIGSERIAL PRIMARY KEY,
    challenge_id               BIGINT         NOT NULL,
    quiz_session_id            BIGINT,
    creator_id                 BIGINT         NOT NULL,
    wager_type                 wager_type     NOT NULL,
    stake_type                 stake_type     NOT NULL,
    stake_amount               DECIMAL(10, 2) NOT NULL,
    stake_currency             currency_type, -- NULL for POINTS/SCREEN_TIME
    status                     wager_status   NOT NULL DEFAULT 'PROPOSED',
    min_participants           INTEGER        NOT NULL DEFAULT 2,
    max_participants           INTEGER,
    social_penalty_description TEXT,
    screen_time_minutes        INTEGER,
    expires_at                 TIMESTAMP WITH TIME ZONE NOT NULL,
    settled_at                 TIMESTAMP WITH TIME ZONE,
    created_at                 TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at                 TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,

    -- Constraints
    CONSTRAINT chk_stake_amount_positive CHECK (stake_amount > 0),
    CONSTRAINT fk_wagers_challenge FOREIGN KEY (challenge_id) REFERENCES challenges (id) ON DELETE CASCADE,
    CONSTRAINT fk_wagers_quiz_session FOREIGN KEY (quiz_session_id) REFERENCES quiz_sessions (id) ON DELETE SET NULL,
    CONSTRAINT fk_wagers_creator FOREIGN KEY (creator_id) REFERENCES users (id) ON DELETE CASCADE
);

COMMENT ON TABLE wagers IS 'Core wager definitions between challenge participants';
CREATE INDEX idx_wagers_challenge_id ON wagers (challenge_id);
CREATE INDEX idx_wagers_creator_id ON wagers (creator_id);
CREATE INDEX idx_wagers_status ON wagers (status);

-- ============================================================================
-- WAGER_PARTICIPANTS TABLE
-- ============================================================================

CREATE TABLE wager_participants
(
    id              BIGSERIAL PRIMARY KEY,
    wager_id        BIGINT                   NOT NULL,
    user_id         BIGINT                   NOT NULL,
    status          participant_wager_status NOT NULL DEFAULT 'INVITED',
    stake_escrowed  BOOLEAN                  NOT NULL DEFAULT FALSE,
    amount_won      DECIMAL(10, 2),
    amount_lost     DECIMAL(10, 2),
    quiz_score      INTEGER,
    joined_at       TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    settled_at      TIMESTAMP WITH TIME ZONE,

    -- Constraints
    CONSTRAINT fk_wager_participants_wager FOREIGN KEY (wager_id) REFERENCES wagers (id) ON DELETE CASCADE,
    CONSTRAINT fk_wager_participants_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT uk_wager_participant UNIQUE (wager_id, user_id)
);

COMMENT ON TABLE wager_participants IS 'Links users to wagers with their participation status and outcomes';
CREATE INDEX idx_wager_participants_user_id ON wager_participants (user_id);

-- ============================================================================
-- WAGER_OUTCOMES TABLE
-- ============================================================================

CREATE TABLE wager_outcomes
(
    id                  BIGSERIAL PRIMARY KEY,
    wager_id            BIGINT          NOT NULL,
    winner_id           BIGINT, -- NULL for draws
    loser_id            BIGINT,
    settlement_type     settlement_type NOT NULL,
    amount_distributed  DECIMAL(10, 2)  NOT NULL,
    penalty_assigned    BOOLEAN         NOT NULL DEFAULT FALSE,
    notes               TEXT,
    settled_at          TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,

    -- Constraints
    CONSTRAINT fk_wager_outcomes_wager FOREIGN KEY (wager_id) REFERENCES wagers (id) ON DELETE CASCADE,
    CONSTRAINT fk_wager_outcomes_winner FOREIGN KEY (winner_id) REFERENCES users (id) ON DELETE SET NULL,
    CONSTRAINT fk_wager_outcomes_loser FOREIGN KEY (loser_id) REFERENCES users (id) ON DELETE SET NULL
);

COMMENT ON TABLE wager_outcomes IS 'Historical record of wager settlements';
CREATE INDEX idx_wager_outcomes_wager_id ON wager_outcomes (wager_id);

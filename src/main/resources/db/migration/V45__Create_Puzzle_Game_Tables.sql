-- ============================================================================
-- Flyway Migration V45: Create Puzzle Game Tables
-- ============================================================================
-- Description: Creates tables and enums for the Picture Puzzle Game feature.
--              Includes puzzle games, pieces, and participant tracking.
--
-- Version: V45
-- Author: Gemini CLI
-- Date: 2026-02-21
-- ============================================================================

-- Drop ENUM types if they exist
DROP TYPE IF EXISTS puzzle_game_mode CASCADE;
DROP TYPE IF EXISTS puzzle_edge_type CASCADE;
DROP TYPE IF EXISTS puzzle_session_status CASCADE;
DROP TYPE IF EXISTS puzzle_piece_status CASCADE;

-- Create ENUM types
CREATE TYPE puzzle_game_mode AS ENUM ('SHARED', 'INDIVIDUAL');
COMMENT ON TYPE puzzle_game_mode IS 'Puzzle game mode: SHARED (team collaboration) or INDIVIDUAL (solo competitive)';

CREATE TYPE puzzle_edge_type AS ENUM ('FLAT', 'TAB', 'BLANK');
COMMENT ON TYPE puzzle_edge_type IS 'Puzzle piece edge type for jigsaw generation';

CREATE TYPE puzzle_session_status AS ENUM (
    'CREATED', 'DISTRIBUTING', 'IN_PROGRESS', 
    'GUESSING', 'COMPLETED', 'ABANDONED'
);
COMMENT ON TYPE puzzle_session_status IS 'Status of a puzzle game session';

CREATE TYPE puzzle_piece_status AS ENUM (
    'UNASSIGNED', 'ASSIGNED', 'PLACED_CORRECT', 'PLACED_INCORRECT'
);
COMMENT ON TYPE puzzle_piece_status IS 'Status of an individual puzzle piece';

-- ============================================================================
-- PUZZLE_GAMES TABLE
-- ============================================================================

CREATE TABLE puzzle_games (
    id                           BIGSERIAL PRIMARY KEY,
    challenge_id                 BIGINT NOT NULL,
    quiz_session_id              BIGINT,
    room_code                    VARCHAR(10),
    source_image_media_id        BIGINT NOT NULL,
    game_mode                    puzzle_game_mode NOT NULL,
    grid_rows                    INTEGER NOT NULL DEFAULT 3,
    grid_cols                    INTEGER NOT NULL DEFAULT 3,
    total_pieces                 INTEGER NOT NULL,
    answer                       VARCHAR(500) NOT NULL,
    answer_aliases               TEXT, -- Store as JSON array string
    difficulty                   quiz_difficulty NOT NULL DEFAULT 'MEDIUM',
    time_limit_seconds           INTEGER,
    hint_text                    VARCHAR(500),
    hint_available_after_seconds INTEGER,
    enable_ai_validation         BOOLEAN DEFAULT FALSE,
    status                       puzzle_session_status NOT NULL DEFAULT 'CREATED',
    creator_id                   BIGINT NOT NULL,
    created_at                   TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    updated_at                   TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    started_at                   TIMESTAMPTZ,
    completed_at                 TIMESTAMPTZ,
    
    CONSTRAINT fk_puzzle_game_challenge FOREIGN KEY (challenge_id) REFERENCES challenges(id),
    CONSTRAINT fk_puzzle_game_quiz_session FOREIGN KEY (quiz_session_id) REFERENCES quiz_sessions(id),
    CONSTRAINT fk_puzzle_game_media FOREIGN KEY (source_image_media_id) REFERENCES media_files(id),
    CONSTRAINT fk_puzzle_game_creator FOREIGN KEY (creator_id) REFERENCES users(id)
);

COMMENT ON TABLE puzzle_games IS 'Core puzzle game definition and session state';

-- ============================================================================
-- PUZZLE_PIECES TABLE
-- ============================================================================

CREATE TABLE puzzle_pieces (
    id                    BIGSERIAL PRIMARY KEY,
    puzzle_game_id        BIGINT NOT NULL,
    piece_index           INTEGER NOT NULL,
    grid_row              INTEGER NOT NULL,
    grid_col              INTEGER NOT NULL,
    piece_image_media_id  BIGINT NOT NULL,
    edge_top              puzzle_edge_type NOT NULL,
    edge_right            puzzle_edge_type NOT NULL,
    edge_bottom           puzzle_edge_type NOT NULL,
    edge_left             puzzle_edge_type NOT NULL,
    svg_clip_path         TEXT NOT NULL,
    width_px              INTEGER NOT NULL,
    height_px             INTEGER NOT NULL,
    created_at            TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT fk_puzzle_piece_game FOREIGN KEY (puzzle_game_id) REFERENCES puzzle_games(id) ON DELETE CASCADE,
    CONSTRAINT fk_puzzle_piece_media FOREIGN KEY (piece_image_media_id) REFERENCES media_files(id)
);

CREATE INDEX idx_puzzle_pieces_game_id ON puzzle_pieces(puzzle_game_id);

COMMENT ON TABLE puzzle_pieces IS 'Metadata for individual jigsaw pieces';

-- ============================================================================
-- PUZZLE_PARTICIPANTS TABLE
-- ============================================================================

CREATE TABLE puzzle_participants (
    id                       BIGSERIAL PRIMARY KEY,
    puzzle_game_id           BIGINT NOT NULL,
    user_id                  BIGINT NOT NULL,
    assigned_piece_ids       TEXT, -- JSON array of piece IDs
    current_board_state      TEXT, -- JSON array of piece positions
    text_answer              VARCHAR(500),
    answer_correct           BOOLEAN DEFAULT FALSE,
    answer_submitted_at      TIMESTAMPTZ,
    pieces_placed_correctly  INTEGER DEFAULT 0,
    total_moves              INTEGER DEFAULT 0,
    score                    INTEGER DEFAULT 0,
    completion_time_ms       BIGINT,
    joined_at                TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT fk_puzzle_participant_game FOREIGN KEY (puzzle_game_id) REFERENCES puzzle_games(id),
    CONSTRAINT fk_puzzle_participant_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT unique_puzzle_game_user UNIQUE(puzzle_game_id, user_id)
);

CREATE INDEX idx_puzzle_participants_game_id ON puzzle_participants(puzzle_game_id);
CREATE INDEX idx_puzzle_participants_user_id ON puzzle_participants(user_id);

COMMENT ON TABLE puzzle_participants IS 'Per-player state tracking for puzzle games';

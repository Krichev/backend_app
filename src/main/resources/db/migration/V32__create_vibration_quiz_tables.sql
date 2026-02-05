-- VibrationSong entity
CREATE TABLE vibration_songs (
    id BIGSERIAL PRIMARY KEY,
    external_id UUID DEFAULT gen_random_uuid() UNIQUE,
    song_title VARCHAR(255) NOT NULL,
    artist VARCHAR(255) NOT NULL,
    category VARCHAR(100),
    release_year INTEGER,
    difficulty VARCHAR(20) NOT NULL, -- EASY, MEDIUM, HARD
    rhythm_pattern JSONB NOT NULL,   -- RhythmPatternDTO as JSON
    excerpt_duration_ms INTEGER,
    wrong_answers JSONB NOT NULL,    -- Array of 3 strings
    hint TEXT,
    status VARCHAR(20) DEFAULT 'PENDING', -- PENDING, APPROVED, REJECTED
    visibility VARCHAR(20) DEFAULT 'PUBLIC', -- PRIVATE, FRIENDS, PUBLIC
    creator_id VARCHAR(255),         -- User ID who created
    play_count INTEGER DEFAULT 0,
    correct_guesses INTEGER DEFAULT 0,
    total_attempts INTEGER DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_vibration_songs_difficulty ON vibration_songs(difficulty);
CREATE INDEX idx_vibration_songs_category ON vibration_songs(category);
CREATE INDEX idx_vibration_songs_status ON vibration_songs(status);
CREATE INDEX idx_vibration_songs_creator ON vibration_songs(creator_id);

-- VibrationGameSession entity
CREATE TABLE vibration_game_sessions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id VARCHAR(255) NOT NULL,
    difficulty VARCHAR(20) NOT NULL,
    question_count INTEGER NOT NULL,
    max_replays_per_question INTEGER DEFAULT 3,
    guess_time_limit_seconds INTEGER DEFAULT 30,
    status VARCHAR(20) DEFAULT 'ACTIVE', -- ACTIVE, COMPLETED, ABANDONED
    current_question_index INTEGER DEFAULT 0,
    total_score INTEGER DEFAULT 0,
    correct_answers INTEGER DEFAULT 0,
    started_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMP
);

CREATE INDEX idx_vibration_sessions_user ON vibration_game_sessions(user_id);
CREATE INDEX idx_vibration_sessions_status ON vibration_game_sessions(status);

-- Session questions (many-to-many with order)
CREATE TABLE vibration_session_questions (
    session_id UUID REFERENCES vibration_game_sessions(id),
    song_id BIGINT REFERENCES vibration_songs(id),
    question_order INTEGER NOT NULL,
    selected_answer VARCHAR(255),
    is_correct BOOLEAN,
    response_time_ms INTEGER,
    replays_used INTEGER DEFAULT 0,
    points_earned INTEGER DEFAULT 0,
    answered_at TIMESTAMP,
    PRIMARY KEY (session_id, song_id)
);

-- Leaderboard entries (denormalized for performance)
CREATE TABLE vibration_leaderboard (
    id BIGSERIAL PRIMARY KEY,
    user_id VARCHAR(255) NOT NULL,
    period VARCHAR(20) NOT NULL, -- DAILY, WEEKLY, MONTHLY, ALL_TIME
    period_start DATE NOT NULL,
    difficulty VARCHAR(20), -- NULL means all difficulties
    total_score INTEGER DEFAULT 0,
    games_played INTEGER DEFAULT 0,
    correct_answers INTEGER DEFAULT 0,
    total_questions INTEGER DEFAULT 0,
    best_streak INTEGER DEFAULT 0,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(user_id, period, period_start, difficulty)
);

CREATE INDEX idx_leaderboard_period ON vibration_leaderboard(period, period_start, difficulty);
CREATE INDEX idx_leaderboard_score ON vibration_leaderboard(total_score DESC);

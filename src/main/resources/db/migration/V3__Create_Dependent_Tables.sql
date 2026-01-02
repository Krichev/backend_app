-- ============================================================================
-- Flyway Migration V3: Create Dependent Tables
-- ============================================================================
-- Description: Creates all remaining tables that reference core tables.
--              Foreign key constraints are added in V5 to prevent circular
--              dependency issues during schema creation.
--
-- Version: V3
-- Author: Challenger System
-- Date: 2026-01-01
--
-- Tables Included (in logical dependency order):
--   - quests, challenges, stakes, photos
--   - tasks, rewards
--   - challenge_participants, challenge_progress, challenge_progress_completed_days
--   - challenge_quests, user_quests, group_users, quest_groups
--   - task_completions, reward_users
--   - verification_details, location_coordinates, photo_verification_details
--   - user_connections, user_activity_logs, user_relationships
--   - quiz_questions, quiz_sessions, quiz_rounds
--   - questions_old, questions_backup, tournament_questions
--   - refresh_tokens, challenge_access, payment_transactions, question_access_log
--
-- Note: Foreign key constraints intentionally omitted - added in V5.
-- ============================================================================

-- ============================================================================
-- QUEST AND CHALLENGE TABLES
-- ============================================================================

DROP TABLE IF EXISTS quests CASCADE;

CREATE TABLE quests
(
    id           BIGSERIAL PRIMARY KEY,
    title        VARCHAR(255) NOT NULL,
    description  TEXT,
    type         quest_type,
    visibility   quest_visibility         DEFAULT 'PUBLIC',
    status       quest_status             DEFAULT 'OPEN',
    creator_id   BIGINT       NOT NULL,
    challenge_id BIGINT,
    created_at   TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at   TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE quests IS 'Quest definitions for challenges and activities';
COMMENT ON COLUMN quests.id IS 'Auto-incrementing primary key';
COMMENT ON COLUMN quests.title IS 'Quest title/name';
COMMENT ON COLUMN quests.description IS 'Detailed quest description';
COMMENT ON COLUMN quests.type IS 'Quest type: CHALLENGE, ACTIVITY_PARTNER, LEARNING, CONTEST';
COMMENT ON COLUMN quests.visibility IS 'Who can see: PUBLIC, PRIVATE, GROUP_ONLY';
COMMENT ON COLUMN quests.status IS 'Current status: OPEN, IN_PROGRESS, COMPLETED, CANCELLED';
COMMENT ON COLUMN quests.creator_id IS 'User who created the quest (FK to users)';
COMMENT ON COLUMN quests.challenge_id IS 'Associated challenge (FK to challenges)';

-- ============================================================================

DROP TABLE IF EXISTS challenges CASCADE;

CREATE TABLE challenges
(
    id                  BIGSERIAL PRIMARY KEY,
    title               VARCHAR(255)   NOT NULL,
    description         TEXT,
    quiz_config         TEXT,
    type                challenge_type NOT NULL,
    creator_id          BIGINT         NOT NULL,
    group_id            BIGINT,
    is_public           BOOLEAN                  DEFAULT TRUE,
    start_date          TIMESTAMP WITH TIME ZONE,
    end_date            TIMESTAMP WITH TIME ZONE,
    frequency           challenge_frequency      DEFAULT 'ONE_TIME',
    verification_method verification_method      DEFAULT 'MANUAL',
    status              challenge_status         DEFAULT 'PENDING',
    difficulty          challenge_difficulty_type NOT NULL DEFAULT 'MEDIUM',
    payment_type        payment_type             DEFAULT 'FREE',
    has_entry_fee       BOOLEAN                  DEFAULT FALSE,
    entry_fee_amount    DECIMAL(10, 2),
    entry_fee_currency  currency_type,
    has_prize           BOOLEAN                  DEFAULT FALSE,
    prize_amount        DECIMAL(10, 2),
    prize_currency      currency_type,
    prize_pool          DECIMAL(10, 2)           DEFAULT 0.00,
    requires_approval   BOOLEAN                  DEFAULT FALSE,
    created_at          TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,

    -- Constraints
    CONSTRAINT chk_entry_fee_positive CHECK (entry_fee_amount IS NULL OR entry_fee_amount >= 0),
    CONSTRAINT chk_prize_positive CHECK (prize_amount IS NULL OR prize_amount >= 0),
    CONSTRAINT chk_prize_pool_positive CHECK (prize_pool >= 0)
);

COMMENT ON TABLE challenges IS 'Challenge definitions including accountability, quests, events, and quizzes';
COMMENT ON COLUMN challenges.id IS 'Auto-incrementing primary key';
COMMENT ON COLUMN challenges.title IS 'Challenge title';
COMMENT ON COLUMN challenges.description IS 'Detailed challenge description';
COMMENT ON COLUMN challenges.quiz_config IS 'JSON configuration for quiz challenges';
COMMENT ON COLUMN challenges.type IS 'Challenge type: ACCOUNTABILITY, QUEST, EVENT, QUIZ';
COMMENT ON COLUMN challenges.creator_id IS 'User who created the challenge (FK to users)';
COMMENT ON COLUMN challenges.group_id IS 'Associated group (FK to groups)';
COMMENT ON COLUMN challenges.is_public IS 'Whether challenge is publicly visible';
COMMENT ON COLUMN challenges.difficulty IS 'Challenge difficulty level: BEGINNER, EASY, MEDIUM, HARD, EXPERT, EXTREME';
COMMENT ON COLUMN challenges.payment_type IS 'Payment model: FREE, ENTRY_FEE, PRIZE_POOL, SUBSCRIPTION';

-- ============================================================================

DROP TABLE IF EXISTS stakes CASCADE;

CREATE TABLE stakes
(
    id              BIGSERIAL PRIMARY KEY,
    user_id         BIGINT           NOT NULL,
    challenge_id    BIGINT           NOT NULL,
    amount          DOUBLE PRECISION NOT NULL,
    currency        currency_type            DEFAULT 'USD',
    is_refunded     BOOLEAN                  DEFAULT FALSE,
    collective_pool BOOLEAN                  DEFAULT FALSE,
    created_at      TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE stakes IS 'Financial stakes placed by users on challenges';
COMMENT ON COLUMN stakes.collective_pool IS 'Whether this is part of a collective prize pool';

-- ============================================================================

DROP TABLE IF EXISTS photos CASCADE;

CREATE TABLE photos
(
    id                BIGSERIAL PRIMARY KEY,
    filename          VARCHAR(255)             NOT NULL,
    original_filename VARCHAR(255)             NOT NULL,
    file_path         VARCHAR(512)             NOT NULL,
    file_size         BIGINT                   NOT NULL,
    mime_type         VARCHAR(100)             NOT NULL,
    width             INTEGER,
    height            INTEGER,
    uploaded_by       BIGINT                   NOT NULL,
    photo_type        photo_type_enum          NOT NULL,
    entity_id         BIGINT,
    s3_key            VARCHAR(512),
    s3_url            VARCHAR(1024),
    processing_status processing_status_enum            DEFAULT 'PENDING',
    alt_text          TEXT,
    description       TEXT,
    created_at        TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE photos IS 'Table to store photo metadata and S3 information';
COMMENT ON COLUMN photos.file_path IS 'S3 key/path where the file is stored';
COMMENT ON COLUMN photos.s3_key IS 'S3 object key for direct S3 operations';
COMMENT ON COLUMN photos.s3_url IS 'Full S3 or CloudFront URL for the image';
COMMENT ON COLUMN photos.entity_id IS 'ID of the entity this photo belongs to (user, challenge, etc.)';
COMMENT ON COLUMN photos.photo_type IS 'Type of photo (AVATAR, QUIZ_QUESTION, etc.)';
COMMENT ON COLUMN photos.processing_status IS 'Current processing status of the photo';

-- ============================================================================
-- TASK AND REWARD TABLES
-- ============================================================================

DROP TABLE IF EXISTS tasks CASCADE;

CREATE TABLE tasks
(
    id                  BIGSERIAL PRIMARY KEY,
    title               VARCHAR(255) NOT NULL,
    description         TEXT,
    type                task_type                DEFAULT 'ONE_TIME',
    status              task_status              DEFAULT 'NOT_STARTED',
    verification_method verification_method      DEFAULT 'MANUAL',
    due_date            TIMESTAMP WITH TIME ZONE,
    start_date          TIMESTAMP WITH TIME ZONE,
    end_date            TIMESTAMP WITH TIME ZONE,
    quest_id            BIGINT,
    assigned_to         BIGINT,
    challenge_id        BIGINT,
    created_by          BIGINT,
    frequency           frequency_type,
    created_at          TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE tasks IS 'Individual tasks within quests and challenges';
COMMENT ON COLUMN tasks.created_by IS 'User who created the task (FK to users)';
COMMENT ON COLUMN tasks.frequency IS 'Task recurrence frequency';

-- ============================================================================

DROP TABLE IF EXISTS rewards CASCADE;

CREATE TABLE rewards
(
    id                   BIGSERIAL PRIMARY KEY,
    title                VARCHAR(255) NOT NULL,
    description          TEXT,
    type                 reward_type              DEFAULT 'POINTS',
    value                DOUBLE PRECISION,
    monetary_value       DOUBLE PRECISION,
    monetary_value_old   DOUBLE PRECISION,
    currency             currency_type,
    reward_source        reward_source            DEFAULT 'SYSTEM',
    quest_id             BIGINT,
    created_at           TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE rewards IS 'Reward definitions for quests and challenges';
COMMENT ON COLUMN rewards.monetary_value IS 'Current monetary value of reward';
COMMENT ON COLUMN rewards.monetary_value_old IS 'Legacy value field (deprecated)';

-- ============================================================================
-- CHALLENGE PARTICIPATION TABLES
-- ============================================================================

DROP TABLE IF EXISTS challenge_participants CASCADE;

CREATE TABLE challenge_participants
(
    challenge_id BIGINT,
    user_id      BIGINT,
    joined_at    TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (challenge_id, user_id)
);

COMMENT ON TABLE challenge_participants IS 'Junction table linking users to challenges they participate in';

-- ============================================================================

DROP TABLE IF EXISTS challenge_progress CASCADE;

CREATE TABLE challenge_progress
(
    id                    BIGSERIAL PRIMARY KEY,
    challenge_id          BIGINT             NOT NULL,
    user_id               BIGINT             NOT NULL,
    status                participant_status NOT NULL,
    completion_percentage DOUBLE PRECISION         DEFAULT 0,
    verification_data     TEXT,
    verification_status   completion_status        DEFAULT 'SUBMITTED',
    verified_by           BIGINT,
    verification_date     TIMESTAMP WITH TIME ZONE,
    streak                INTEGER,
    total_rewards_earned  DOUBLE PRECISION,
    created_at            TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at            TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE challenge_progress IS 'Tracks individual user progress within challenges';
COMMENT ON COLUMN challenge_progress.streak IS 'Current streak count for daily challenges';
COMMENT ON COLUMN challenge_progress.total_rewards_earned IS 'Total rewards earned in this challenge';

-- ============================================================================

DROP TABLE IF EXISTS challenge_progress_completed_days CASCADE;

CREATE TABLE challenge_progress_completed_days
(
    id                    BIGSERIAL PRIMARY KEY,
    challenge_progress_id BIGINT NOT NULL,
    completed_day         DATE   NOT NULL,
    created_at            TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (challenge_progress_id, completed_day)
);

COMMENT ON TABLE challenge_progress_completed_days IS 'Tracks individual days completed for recurring challenges';

-- ============================================================================
-- JUNCTION TABLES
-- ============================================================================

DROP TABLE IF EXISTS challenge_quests CASCADE;

CREATE TABLE challenge_quests
(
    challenge_id BIGINT,
    quest_id     BIGINT,
    PRIMARY KEY (challenge_id, quest_id)
);

COMMENT ON TABLE challenge_quests IS 'Junction table linking challenges to quests';

-- ============================================================================

DROP TABLE IF EXISTS user_quests CASCADE;

CREATE TABLE user_quests
(
    user_id   BIGINT,
    quest_id  BIGINT,
    joined_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    status    quiz_session_status      DEFAULT 'CREATED',
    PRIMARY KEY (user_id, quest_id)
);

COMMENT ON TABLE user_quests IS 'Junction table linking users to quests they have joined';

-- ============================================================================

DROP TABLE IF EXISTS group_users CASCADE;

CREATE TABLE group_users
(
    group_id  BIGINT,
    user_id   BIGINT,
    joined_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    role      user_role                DEFAULT 'MEMBER',
    PRIMARY KEY (group_id, user_id)
);

COMMENT ON TABLE group_users IS 'Junction table linking users to groups with roles';

-- ============================================================================

DROP TABLE IF EXISTS quest_groups CASCADE;

CREATE TABLE quest_groups
(
    quest_id BIGINT,
    group_id BIGINT,
    PRIMARY KEY (quest_id, group_id)
);

COMMENT ON TABLE quest_groups IS 'Junction table linking quests to groups';

-- ============================================================================

DROP TABLE IF EXISTS task_completions CASCADE;

CREATE TABLE task_completions
(
    id              BIGSERIAL PRIMARY KEY,
    task_id         BIGINT NOT NULL,
    user_id         BIGINT NOT NULL,
    status          completion_status        DEFAULT 'SUBMITTED',
    completion_data TEXT,
    notes           TEXT,
    verified_by     BIGINT,
    completed_at    TIMESTAMP WITH TIME ZONE,
    verified_at     TIMESTAMP WITH TIME ZONE,
    created_at      TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE task_completions IS 'Records of task completion submissions and verifications';

-- ============================================================================

DROP TABLE IF EXISTS reward_users CASCADE;

CREATE TABLE reward_users
(
    reward_id    BIGINT,
    user_id      BIGINT,
    awarded_date TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (reward_id, user_id)
);

COMMENT ON TABLE reward_users IS 'Junction table linking rewards to users who earned them';

-- ============================================================================
-- VERIFICATION TABLES
-- ============================================================================

DROP TABLE IF EXISTS location_coordinates CASCADE;

CREATE TABLE location_coordinates
(
    id         BIGSERIAL PRIMARY KEY,
    latitude   DOUBLE PRECISION,
    longitude  DOUBLE PRECISION,
    created_at TIMESTAMP                DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE location_coordinates IS 'Geographic coordinates for location-based verification';

-- ============================================================================

DROP TABLE IF EXISTS photo_verification_details CASCADE;

CREATE TABLE photo_verification_details
(
    id                         BIGSERIAL PRIMARY KEY,
    description                TEXT,
    requires_photo_comparison  BOOLEAN DEFAULT FALSE,
    verification_mode          VARCHAR(50) DEFAULT 'standard',
    created_at                 TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at                 TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE photo_verification_details IS 'Configuration for photo-based verification requirements';

-- ============================================================================

DROP TABLE IF EXISTS verification_details CASCADE;

CREATE TABLE verification_details
(
    id                      BIGSERIAL PRIMARY KEY,
    activity_type           VARCHAR(100),
    target_value            DOUBLE PRECISION,
    radius                  DOUBLE PRECISION,
    challenge_id            BIGINT NOT NULL,
    location_coordinates_id BIGINT,
    photo_details_id        BIGINT,
    created_at              TIMESTAMP                DEFAULT CURRENT_TIMESTAMP,
    updated_at              TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE verification_details IS 'Verification requirements and configuration for challenges';

-- ============================================================================
-- USER RELATIONSHIP TABLES
-- ============================================================================

DROP TABLE IF EXISTS user_connections CASCADE;

CREATE TABLE user_connections
(
    id                BIGSERIAL PRIMARY KEY,
    user_id           BIGINT NOT NULL,
    connected_user_id BIGINT NOT NULL,
    status            connection_status        DEFAULT 'PENDING',
    created_at        TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at        TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (user_id, connected_user_id)
);

COMMENT ON TABLE user_connections IS 'User-to-user connections for networking';

-- ============================================================================

DROP TABLE IF EXISTS user_relationships CASCADE;

CREATE TABLE user_relationships
(
    id                BIGSERIAL PRIMARY KEY,
    user_id           BIGINT      NOT NULL,
    related_user_id   BIGINT      NOT NULL,
    relationship_type VARCHAR(50) NOT NULL,
    status            VARCHAR(50) DEFAULT 'PENDING' NOT NULL,
    created_at        TIMESTAMP   DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at        TIMESTAMP   DEFAULT CURRENT_TIMESTAMP NOT NULL,

    CONSTRAINT uk_user_relationship UNIQUE (user_id, related_user_id),
    CONSTRAINT chk_different_users CHECK (user_id != related_user_id)
);

COMMENT ON TABLE user_relationships IS 'Manages friend and family relationships between users';
COMMENT ON COLUMN user_relationships.relationship_type IS 'Type: FRIEND, FAMILY, BLOCKED';
COMMENT ON COLUMN user_relationships.status IS 'Status: PENDING, ACCEPTED, REJECTED';

-- ============================================================================

DROP TABLE IF EXISTS user_activity_logs CASCADE;

CREATE TABLE user_activity_logs
(
    id            BIGSERIAL PRIMARY KEY,
    user_id       BIGINT        NOT NULL,
    activity_type activity_type NOT NULL,
    description   TEXT,
    metadata      JSONB,
    created_at    TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE user_activity_logs IS 'Audit log of user activities and events';

-- ============================================================================
-- QUIZ TABLES
-- ============================================================================

DROP TABLE IF EXISTS quiz_questions CASCADE;

CREATE TABLE quiz_questions
(
    id                      BIGSERIAL PRIMARY KEY,
    question                TEXT    NOT NULL,
    answer                  TEXT    NOT NULL,
    difficulty              quiz_difficulty          DEFAULT 'EASY',
    topic                   TEXT,
    topic_id                BIGINT,
    legacy_topic            VARCHAR(100),
    source                  TEXT,
    additional_info         TEXT,
    comments                TEXT,
    authors                 TEXT,
    pass_criteria           TEXT,
    is_user_created         BOOLEAN                  DEFAULT FALSE,
    creator_id              BIGINT,
    external_id             VARCHAR(255),
    usage_count             INTEGER                  DEFAULT 0,
    challenge_id            BIGINT,
    media_url               VARCHAR(500),
    has_media               BOOLEAN                  DEFAULT FALSE,
    media_type              media_type,
    question_type           VARCHAR(20)              DEFAULT 'TEXT' NOT NULL,
    question_media_url      VARCHAR(500),
    question_media_id       BIGINT,
    question_media_type     media_type,
    question_thumbnail_url  VARCHAR(500),
    visibility              question_visibility      DEFAULT 'PRIVATE' NOT NULL,
    original_quiz_id        BIGINT,
    is_active               BOOLEAN                  DEFAULT TRUE,
    legacy_question_id      INTEGER,
    created_at              TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at              TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    last_used               TIMESTAMP WITH TIME ZONE
);

COMMENT ON TABLE quiz_questions IS 'Quiz questions with multimedia support and visibility controls';
COMMENT ON COLUMN quiz_questions.question_type IS 'Type of question: TEXT, IMAGE, VIDEO, or AUDIO';
COMMENT ON COLUMN quiz_questions.question_media_url IS 'URL to access the question media file';
COMMENT ON COLUMN quiz_questions.question_media_id IS 'Foreign key to media_files.id';
COMMENT ON COLUMN quiz_questions.question_thumbnail_url IS 'URL to thumbnail for video/image questions';
COMMENT ON COLUMN quiz_questions.visibility IS 'Access policy for user-created questions';
COMMENT ON COLUMN quiz_questions.original_quiz_id IS 'Original quiz/challenge ID if visibility is QUIZ_ONLY';

-- ============================================================================

DROP TABLE IF EXISTS quiz_sessions CASCADE;

CREATE TABLE quiz_sessions
(
    id                     BIGSERIAL PRIMARY KEY,
    user_id                BIGINT  NOT NULL,
    challenge_id           BIGINT,
    difficulty             quiz_difficulty          DEFAULT 'EASY',
    question_source        question_source          DEFAULT 'app',
    total_questions        INTEGER                  DEFAULT 0,
    correct_answers        INTEGER                  DEFAULT 0,
    status                 quiz_session_status      DEFAULT 'CREATED',
    started_at             TIMESTAMP WITH TIME ZONE,
    completed_at           TIMESTAMP WITH TIME ZONE,
    host_user_id           BIGINT  NOT NULL,
    team_name              VARCHAR(200),
    team_members           VARCHAR(1000),
    round_time_seconds     INTEGER,
    total_rounds           INTEGER,
    completed_rounds       INTEGER                  DEFAULT 0,
    enable_ai_host         BOOLEAN                  DEFAULT FALSE,
    total_duration_seconds INTEGER,
    creator_id             BIGINT  NOT NULL,
    created_at             TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at             TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE quiz_sessions IS 'Quiz game sessions with multiplayer support';
COMMENT ON COLUMN quiz_sessions.team_members IS 'JSON array as string';

-- ============================================================================

DROP TABLE IF EXISTS quiz_rounds CASCADE;

CREATE TABLE quiz_rounds
(
    id                            BIGSERIAL PRIMARY KEY,
    quiz_session_id               BIGINT                   NOT NULL,
    question_id                   BIGINT                   NOT NULL,
    user_answer                   VARCHAR(500),
    team_answer                   VARCHAR(500),
    is_correct                    BOOLEAN,
    answered_at                   TIMESTAMP WITH TIME ZONE,
    time_taken                    INTEGER,
    round_number                  INTEGER                  NOT NULL,
    player_who_answered           VARCHAR(200),
    discussion_notes              VARCHAR(2000),
    round_started_at              TIMESTAMP WITH TIME ZONE,
    discussion_started_at         TIMESTAMP WITH TIME ZONE,
    answer_submitted_at           TIMESTAMP WITH TIME ZONE,
    discussion_duration_seconds   INTEGER,
    total_round_duration_seconds  INTEGER,
    hint_used                     BOOLEAN                           DEFAULT FALSE,
    voice_recording_used          BOOLEAN                           DEFAULT FALSE,
    ai_feedback                   VARCHAR(1000),
    media_interaction_count       INTEGER                           DEFAULT 0,
    media_play_duration           INTEGER,
    response_metadata             JSONB,
    created_at                    TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at                    TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE quiz_rounds IS 'Individual rounds within quiz sessions with detailed tracking';
COMMENT ON COLUMN quiz_rounds.media_play_duration IS 'Duration in seconds';

-- ============================================================================
-- LEGACY QUESTION TABLES
-- ============================================================================

DROP TABLE IF EXISTS questions_old CASCADE;

CREATE TABLE questions_old
(
    id               SERIAL PRIMARY KEY,
    tournament_id    INTEGER NOT NULL,
    tournament_title TEXT    NOT NULL,
    question_num     INTEGER,
    question         TEXT    NOT NULL,
    answer           TEXT    NOT NULL,
    authors          TEXT,
    sources          TEXT,
    comments         TEXT,
    pass_criteria    TEXT,
    notices          TEXT,
    images           TEXT,
    rating           INTEGER,
    tournament_type  TEXT,
    topic            TEXT,
    topic_num        INTEGER,
    entered_date     TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE questions_old IS 'Legacy questions table (renamed from questions)';

-- ============================================================================

DROP TABLE IF EXISTS questions_backup CASCADE;

CREATE TABLE questions_backup
(
    id               SERIAL PRIMARY KEY,
    tournament_id    INTEGER NOT NULL,
    tournament_title TEXT    NOT NULL,
    question_num     INTEGER,
    question         TEXT    NOT NULL,
    answer           TEXT    NOT NULL,
    authors          TEXT,
    sources          TEXT,
    comments         TEXT,
    pass_criteria    TEXT,
    notices          TEXT,
    images           TEXT,
    rating           INTEGER,
    tournament_type  TEXT,
    topic            TEXT,
    topic_num        INTEGER,
    entered_date     TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE questions_backup IS 'Backup of questions data before migration';

-- ============================================================================

DROP TABLE IF EXISTS tournament_questions CASCADE;

CREATE TABLE tournament_questions
(
    id                  SERIAL PRIMARY KEY,
    quiz_question_id    BIGINT    NOT NULL,
    tournament_id       INTEGER   NOT NULL,
    tournament_title    TEXT      NOT NULL,
    display_order       INTEGER   NOT NULL,
    legacy_question_num INTEGER,
    tournament_type     TEXT,
    topic_num           INTEGER,
    notices             TEXT,
    images              TEXT,
    rating              INTEGER,
    custom_question     TEXT,
    custom_answer       TEXT,
    custom_sources      TEXT,
    points              INTEGER            DEFAULT 10,
    time_limit_seconds  INTEGER,
    is_bonus_question   BOOLEAN            DEFAULT FALSE,
    is_mandatory        BOOLEAN            DEFAULT TRUE,
    is_active           BOOLEAN            DEFAULT TRUE,
    entered_date        TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP          DEFAULT CURRENT_TIMESTAMP,
    added_by            BIGINT,

    CONSTRAINT uk_tournament_display_order UNIQUE (tournament_id, display_order)
);

COMMENT ON TABLE tournament_questions IS 'Tournament-specific question configuration and ordering';

-- ============================================================================
-- AUTHENTICATION AND ACCESS TABLES
-- ============================================================================

DROP TABLE IF EXISTS refresh_tokens CASCADE;

CREATE TABLE refresh_tokens
(
    id          BIGSERIAL PRIMARY KEY,
    user_id     BIGINT       NOT NULL,
    token       VARCHAR(255) NOT NULL UNIQUE,
    expiry_date TIMESTAMP    NOT NULL,
    created_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE refresh_tokens IS 'JWT refresh tokens for authentication';

-- ============================================================================

DROP TABLE IF EXISTS challenge_access CASCADE;

CREATE TABLE challenge_access
(
    id                 BIGSERIAL PRIMARY KEY,
    challenge_id       BIGINT      NOT NULL,
    user_id            BIGINT      NOT NULL,
    granted_by_user_id BIGINT,
    granted_at         TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    status             VARCHAR(20)          DEFAULT 'ACTIVE',
    notes              TEXT,

    CONSTRAINT uq_challenge_user UNIQUE (challenge_id, user_id)
);

COMMENT ON TABLE challenge_access IS 'Access control for private challenges';

-- ============================================================================

DROP TABLE IF EXISTS payment_transactions CASCADE;

CREATE TABLE payment_transactions
(
    id                    BIGSERIAL PRIMARY KEY,
    user_id               BIGINT         NOT NULL,
    challenge_id          BIGINT,
    transaction_type      VARCHAR(50)    NOT NULL,
    amount                DECIMAL(10, 2) NOT NULL,
    currency              currency_type  NOT NULL,
    status                VARCHAR(20)             DEFAULT 'PENDING',
    payment_method        VARCHAR(50),
    transaction_reference VARCHAR(255),
    notes                 TEXT,
    created_at            TIMESTAMP               DEFAULT CURRENT_TIMESTAMP,
    updated_at            TIMESTAMP               DEFAULT CURRENT_TIMESTAMP,
    completed_at          TIMESTAMP
);

COMMENT ON TABLE payment_transactions IS 'Financial transaction audit trail';
COMMENT ON COLUMN payment_transactions.transaction_type IS 'Type: ENTRY_FEE, PRIZE, REFUND';
COMMENT ON COLUMN payment_transactions.status IS 'Status: PENDING, COMPLETED, FAILED, REFUNDED';

-- ============================================================================

DROP TABLE IF EXISTS question_access_log CASCADE;

CREATE TABLE question_access_log
(
    id                  BIGSERIAL PRIMARY KEY,
    question_id         BIGINT      NOT NULL,
    accessed_by_user_id BIGINT      NOT NULL,
    access_type         VARCHAR(50) NOT NULL,
    accessed_at         TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL
);

COMMENT ON TABLE question_access_log IS 'Audit log for question access and usage';
COMMENT ON COLUMN question_access_log.access_type IS 'Type: VIEW, USE_IN_QUIZ, EDIT, DELETE';

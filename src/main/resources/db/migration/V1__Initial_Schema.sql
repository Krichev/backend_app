-- ============================================================================
-- Flyway Migration V1: Initial Schema Baseline
-- ============================================================================
-- Description: Baseline migration containing the complete initial schema
--              for the Challenger Application. This migration represents
--              the starting point for version-controlled database changes.
--
-- Version: V1
-- Author: Challenger System
-- Date: 2025-12-06
--
-- IMPORTANT:
--   - For existing databases: Run `./mvnw flyway:baseline` first
--   - For new databases: This migration will create the complete schema
--   - baseline-on-migrate: true allows Flyway to handle existing databases
-- ============================================================================

-- PostgreSQL Schema for Challenger Application
-- ==============================================

-- Drop tables if they exist (for development/testing)
-- DROP TABLE IF EXISTS challenge_progress_completed_days CASCADE;
-- DROP TABLE IF EXISTS quiz_rounds CASCADE;
-- DROP TABLE IF EXISTS quiz_sessions CASCADE;
-- DROP TABLE IF EXISTS quiz_questions CASCADE;
-- DROP TABLE IF EXISTS user_activity_logs CASCADE;
-- DROP TABLE IF EXISTS user_connections CASCADE;
-- DROP TABLE IF EXISTS reward_users CASCADE;
-- DROP TABLE IF EXISTS task_completions CASCADE;
-- DROP TABLE IF EXISTS quest_groups CASCADE;
-- DROP TABLE IF EXISTS group_users CASCADE;
-- DROP TABLE IF EXISTS user_quests CASCADE;
-- DROP TABLE IF EXISTS rewards CASCADE;
-- DROP TABLE IF EXISTS tasks CASCADE;
-- DROP TABLE IF EXISTS photo_verification_details CASCADE;
-- DROP TABLE IF EXISTS location_coordinates CASCADE;
-- DROP TABLE IF EXISTS verification_details CASCADE;
-- DROP TABLE IF EXISTS challenge_progress CASCADE;
-- DROP TABLE IF EXISTS challenge_participants CASCADE;
-- DROP TABLE IF EXISTS challenge_quests CASCADE;
-- DROP TABLE IF EXISTS challenges CASCADE;
-- DROP TABLE IF EXISTS stakes CASCADE;
-- DROP TABLE IF EXISTS groups CASCADE;
-- DROP TABLE IF EXISTS quests CASCADE;
-- DROP TABLE IF EXISTS users CASCADE;
-- DROP TABLE IF EXISTS photos CASCADE;
-- DROP TABLE IF EXISTS media_files CASCADE;
-- DROP TABLE IF EXISTS tournament_questions CASCADE;
-- DROP TABLE IF EXISTS refresh_tokens CASCADE;

-- Create extension for UUID generation if not exists
CREATE
EXTENSION IF NOT EXISTS "uuid-ossp";

-- Create ENUM types for better type safety
CREATE TYPE user_role AS ENUM ('ADMIN', 'MEMBER', 'MODERATOR');
CREATE TYPE group_type AS ENUM ('CHALLENGE', 'SOCIAL', 'LEARNING', 'FITNESS', 'HOBBY');
CREATE TYPE privacy_setting AS ENUM ('PUBLIC', 'PRIVATE', 'INVITATION_ONLY');
CREATE TYPE quest_type AS ENUM ('CHALLENGE', 'ACTIVITY_PARTNER', 'LEARNING', 'CONTEST');
CREATE TYPE quest_visibility AS ENUM ('PUBLIC', 'PRIVATE', 'GROUP_ONLY');
CREATE TYPE quest_status AS ENUM ('OPEN', 'IN_PROGRESS', 'COMPLETED', 'CANCELLED');
CREATE TYPE task_type AS ENUM ('DAILY', 'ONE_TIME', 'RECURRING', 'WEEKLY', 'MONTHLY');
CREATE TYPE task_status AS ENUM ('NOT_STARTED', 'IN_PROGRESS', 'COMPLETED', 'VERIFIED', 'FAILED');
CREATE TYPE verification_method AS ENUM ('MANUAL', 'FITNESS_API', 'PHOTO', 'QUIZ', 'LOCATION', 'ACTIVITY');
CREATE TYPE completion_status AS ENUM ('SUBMITTED', 'VERIFIED', 'REJECTED', 'PENDING');
CREATE TYPE reward_type AS ENUM ('MONETARY', 'POINTS', 'BADGE', 'CUSTOM');
CREATE TYPE reward_source AS ENUM ('SYSTEM', 'USER', 'SPONSOR');
CREATE TYPE connection_status AS ENUM ('PENDING', 'ACCEPTED', 'REJECTED', 'BLOCKED');
CREATE TYPE challenge_type AS ENUM ('ACCOUNTABILITY', 'QUEST', 'EVENT');
CREATE TYPE challenge_frequency AS ENUM ('DAILY', 'WEEKLY', 'ONE_TIME');
CREATE TYPE challenge_status AS ENUM ('PENDING', 'ACTIVE', 'COMPLETED', 'CANCELLED');
CREATE TYPE participant_status AS ENUM ('IN_PROGRESS', 'COMPLETED', 'FAILED');
CREATE TYPE quiz_difficulty AS ENUM ('EASY', 'MEDIUM', 'HARD');
CREATE TYPE quiz_session_status AS ENUM ('CREATED', 'IN_PROGRESS', 'COMPLETED', 'ABANDONED', 'CANCELLED', 'ARCHIVED');
CREATE TYPE question_source AS ENUM ('app', 'user');
CREATE TYPE currency_type AS ENUM ('USD', 'EUR', 'GBP', 'CAD', 'AUD', 'POINTS');


-- Create enum type for photo_type
CREATE TYPE photo_type_enum AS ENUM (
    'AVATAR', 'QUIZ_QUESTION', 'CHALLENGE_COVER', 'TASK_VERIFICATION',
    'GENERAL', 'THUMBNAIL', 'BACKGROUND', 'BANNER', 'GALLERY', 'DOCUMENT'
);

-- Create enum type for processing_status
CREATE TYPE processing_status_enum AS ENUM (
    'PENDING', 'PROCESSING', 'COMPLETED', 'FAILED'
);


-- Create ENUM types matching your Java enums
CREATE TYPE media_type AS ENUM (
    'IMAGE',
    'VIDEO',
    'AUDIO',
    'DOCUMENT',
    'ARCHIVE'
);

CREATE TYPE media_category AS ENUM (
    'QUIZ_QUESTION', 'AVATAR', 'CHALLENGE_PROOF', 'SYSTEM'
);

CREATE TYPE processing_status AS ENUM (
    'PENDING',
    'PROCESSING',
    'COMPLETED',
    'FAILED',
    'CANCELLED'
);

-- Create the media_files table matching your Java entity exactly
CREATE TABLE media_files
(
    -- Primary key - matches @GeneratedValue(strategy = GenerationType.IDENTITY)
    id                BIGSERIAL PRIMARY KEY,

    -- File information
    original_filename VARCHAR(255)      NOT NULL,
    filename          VARCHAR(255)      NOT NULL UNIQUE,
    file_path         VARCHAR(500)      NOT NULL,
    processed_path    VARCHAR(500),
    thumbnail_path    VARCHAR(500),
    content_type      VARCHAR(100)      NOT NULL,
    file_size         BIGINT            NOT NULL CHECK (file_size > 0),

    -- Enum fields
    media_type        media_type        NOT NULL,
    media_category    media_category    NOT NULL,
    processing_status processing_status NOT NULL DEFAULT 'PENDING',

    -- Entity relationships
    entity_id         BIGINT,
    uploaded_by       BIGINT            NOT NULL,

    -- Timestamp fields - matching Java LocalDateTime
    uploaded_at       TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at        TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at        TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP,

    -- Media metadata fields
    width             INTEGER CHECK (width > 0),
    height            INTEGER CHECK (height > 0),
    duration_seconds  DOUBLE PRECISION CHECK (duration_seconds > 0),
    bitrate           BIGINT CHECK (bitrate > 0),
    frame_rate        DOUBLE PRECISION CHECK (frame_rate > 0),
    resolution        VARCHAR(50),

    -- S3 fields
    s3_key            VARCHAR(500),
    s3_url            TEXT
);

COMMENT
ON TABLE media_files IS 'Stores metadata and information about uploaded media files matching Java MediaFile entity';
COMMENT
ON COLUMN media_files.id IS 'Auto-incrementing primary key (matches @GeneratedValue IDENTITY)';
COMMENT
ON COLUMN media_files.original_filename IS 'Original filename as uploaded by user';
COMMENT
ON COLUMN media_files.filename IS 'Unique filename used for storage';
COMMENT
ON COLUMN media_files.file_path IS 'Full path to the file on storage system';
COMMENT
ON COLUMN media_files.processed_path IS 'Path to processed/optimized version';
COMMENT
ON COLUMN media_files.thumbnail_path IS 'Path to thumbnail version';
COMMENT
ON COLUMN media_files.content_type IS 'MIME type / content type of the file';
COMMENT
ON COLUMN media_files.file_size IS 'File size in bytes';
COMMENT
ON COLUMN media_files.media_type IS 'Type of media (IMAGE, VIDEO, AUDIO, etc.)';
COMMENT
ON COLUMN media_files.media_category IS 'Category of media usage (PROFILE_PICTURE, THUMBNAIL, etc.)';
COMMENT
ON COLUMN media_files.processing_status IS 'Current processing status (PENDING, COMPLETED, FAILED)';
COMMENT
ON COLUMN media_files.entity_id IS 'ID of related entity this media belongs to';
COMMENT
ON COLUMN media_files.uploaded_by IS 'ID of user who uploaded the file';
COMMENT
ON COLUMN media_files.uploaded_at IS 'Timestamp when file was uploaded (matches @CreationTimestamp)';
COMMENT
ON COLUMN media_files.created_at IS 'Record creation timestamp (matches @CreationTimestamp)';
COMMENT
ON COLUMN media_files.updated_at IS 'Last update timestamp (matches @UpdateTimestamp)';
COMMENT
ON COLUMN media_files.duration_seconds IS 'Duration in seconds for video/audio files';
COMMENT
ON COLUMN media_files.frame_rate IS 'Frame rate for video files (decimal with 2 places)';
COMMENT
ON COLUMN media_files.s3_key IS 'S3 object key for cloud storage';
COMMENT
ON COLUMN media_files.s3_url IS 'Full S3 URL for direct access';
-- Users table
CREATE TABLE users
(
    id                  BIGSERIAL PRIMARY KEY,
    username            VARCHAR(255) NOT NULL UNIQUE,
    email               VARCHAR(255) NOT NULL UNIQUE,
    password            VARCHAR(255) NOT NULL,
    profile_picture_url TEXT,
    bio                 TEXT,
    role                user_role                DEFAULT 'MEMBER',
    created_at          TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Groups table
CREATE TABLE groups
(
    id              BIGSERIAL PRIMARY KEY,
    name            VARCHAR(255) NOT NULL,
    description     TEXT,
    type            group_type,
    privacy_setting privacy_setting          DEFAULT 'PUBLIC',
    created_at      TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    creator_id      BIGINT       NOT NULL,
    FOREIGN KEY (creator_id) REFERENCES users (id) ON DELETE CASCADE
);

-- Quests table
CREATE TABLE quests
(
    id           BIGSERIAL PRIMARY KEY,
    title        VARCHAR(255) NOT NULL,
    description  TEXT,
    type         quest_type,
    visibility   quest_visibility         DEFAULT 'PUBLIC',
    status       quest_status             DEFAULT 'OPEN',
    created_at   TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at   TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    creator_id   BIGINT       NOT NULL,
    challenge_id BIGINT,
    FOREIGN KEY (creator_id) REFERENCES users (id) ON DELETE CASCADE
);

-- Tasks table
CREATE TABLE tasks
(
    id                  BIGSERIAL PRIMARY KEY,
    title               VARCHAR(255) NOT NULL,
    description         TEXT,
    type                task_type                DEFAULT 'ONE_TIME',
    status              task_status              DEFAULT 'NOT_STARTED',
    verification_method verification_method      DEFAULT 'MANUAL',
    due_date            TIMESTAMP WITH TIME ZONE,
    created_at          TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    quest_id            BIGINT,
    assigned_to         BIGINT,
    challenge_id        BIGINT,
    FOREIGN KEY (quest_id) REFERENCES quests (id) ON DELETE CASCADE,
    FOREIGN KEY (assigned_to) REFERENCES users (id) ON DELETE SET NULL
);

-- Rewards table
CREATE TABLE rewards
(
    id            BIGSERIAL PRIMARY KEY,
    title         VARCHAR(255) NOT NULL,
    description   TEXT,
    type          reward_type              DEFAULT 'POINTS',
    value         DOUBLE PRECISION,
    reward_source reward_source            DEFAULT 'SYSTEM',
    created_at    TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    quest_id      BIGINT,
    FOREIGN KEY (quest_id) REFERENCES quests (id) ON DELETE CASCADE
);


-- Challenges table
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
    created_at          TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (creator_id) REFERENCES users (id) ON DELETE CASCADE,
    FOREIGN KEY (group_id) REFERENCES groups (id) ON DELETE SET NULL
);

-- Stakes table
CREATE TABLE stakes
(
    id           BIGSERIAL PRIMARY KEY,
    user_id      BIGINT           NOT NULL,
    challenge_id BIGINT           NOT NULL,
    amount       DOUBLE PRECISION NOT NULL,
    currency     currency_type            DEFAULT 'USD',
    is_refunded  BOOLEAN                  DEFAULT FALSE,
    created_at   TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    FOREIGN KEY (challenge_id) REFERENCES challenges (id) ON DELETE CASCADE
);

-- Add foreign key constraint to quests table
ALTER TABLE quests
    ADD CONSTRAINT fk_quest_challenge
        FOREIGN KEY (challenge_id) REFERENCES challenges (id) ON DELETE CASCADE;

-- Add foreign key constraint to tasks table
ALTER TABLE tasks
    ADD CONSTRAINT fk_task_challenge
        FOREIGN KEY (challenge_id) REFERENCES challenges (id) ON DELETE CASCADE;

-- Challenge Participants table
CREATE TABLE challenge_participants
(
    challenge_id BIGINT,
    user_id      BIGINT,
    joined_at    TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (challenge_id, user_id),
    FOREIGN KEY (challenge_id) REFERENCES challenges (id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);

-- Challenge Progress table
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
    created_at            TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at            TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (challenge_id) REFERENCES challenges (id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    FOREIGN KEY (verified_by) REFERENCES users (id) ON DELETE SET NULL
);

-- Challenge Progress Completed Days table
CREATE TABLE challenge_progress_completed_days
(
    id                    BIGSERIAL PRIMARY KEY,
    challenge_progress_id BIGINT NOT NULL,
    completed_day         DATE   NOT NULL,
    created_at            TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (challenge_progress_id, completed_day),
    FOREIGN KEY (challenge_progress_id) REFERENCES challenge_progress (id) ON DELETE CASCADE
);

-- Challenge Quests mapping table
CREATE TABLE challenge_quests
(
    challenge_id BIGINT,
    quest_id     BIGINT,
    PRIMARY KEY (challenge_id, quest_id),
    FOREIGN KEY (challenge_id) REFERENCES challenges (id) ON DELETE CASCADE,
    FOREIGN KEY (quest_id) REFERENCES quests (id) ON DELETE CASCADE
);

-- Junction table for many-to-many relationship between users and quests
CREATE TABLE user_quests
(
    user_id   BIGINT,
    quest_id  BIGINT,
    joined_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    status    quiz_session_status      default 'CREATED',
    PRIMARY KEY (user_id, quest_id),
    FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    FOREIGN KEY (quest_id) REFERENCES quests (id) ON DELETE CASCADE
);

-- Junction table for many-to-many relationship between groups and users
CREATE TABLE group_users
(
    group_id  BIGINT,
    user_id   BIGINT,
    joined_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    role      user_role                DEFAULT 'MEMBER',
    PRIMARY KEY (group_id, user_id),
    FOREIGN KEY (group_id) REFERENCES groups (id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);

-- Junction table for many-to-many relationship between groups and quests
CREATE TABLE quest_groups
(
    quest_id BIGINT,
    group_id BIGINT,
    PRIMARY KEY (quest_id, group_id),
    FOREIGN KEY (quest_id) REFERENCES quests (id) ON DELETE CASCADE,
    FOREIGN KEY (group_id) REFERENCES groups (id) ON DELETE CASCADE
);

-- Task Completions table
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
    created_at      TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (task_id) REFERENCES tasks (id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    FOREIGN KEY (verified_by) REFERENCES users (id) ON DELETE SET NULL
);

CREATE TABLE IF NOT EXISTS location_coordinates
(
    id
    BIGSERIAL
    PRIMARY
    KEY,
    latitude
    DOUBLE
    PRECISION,
    longitude
    DOUBLE
    PRECISION,
    created_at
    TIMESTAMP
    DEFAULT
    CURRENT_TIMESTAMP,
    updated_at
    TIMESTAMP
    WITH
    TIME
    ZONE
    DEFAULT
    CURRENT_TIMESTAMP
);

-- Create photo_verification_details table first (referenced by verification_details)
CREATE TABLE IF NOT EXISTS photo_verification_details
(
    id
    BIGSERIAL
    PRIMARY
    KEY,
    description
    TEXT,
    requires_photo_comparison
    BOOLEAN
    DEFAULT
    FALSE,
    verification_mode
    VARCHAR
(
    50
) DEFAULT 'standard',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
                             );


-- Verification Details table
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
    updated_at              TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,

    -- Foreign key constraints
    CONSTRAINT fk_verification_details_challenge
        FOREIGN KEY (challenge_id) REFERENCES challenges (id)
            ON DELETE CASCADE,

    CONSTRAINT fk_verification_details_location
        FOREIGN KEY (location_coordinates_id) REFERENCES location_coordinates (id)
            ON DELETE SET NULL,

    CONSTRAINT fk_verification_details_photo
        FOREIGN KEY (photo_details_id) REFERENCES photo_verification_details (id)
            ON DELETE SET NULL
);

-- User Connections table
CREATE TABLE user_connections
(
    id                BIGSERIAL PRIMARY KEY,
    user_id           BIGINT NOT NULL,
    connected_user_id BIGINT NOT NULL,
    status            connection_status        DEFAULT 'PENDING',
    created_at        TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at        TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    FOREIGN KEY (connected_user_id) REFERENCES users (id) ON DELETE CASCADE,
    UNIQUE (user_id, connected_user_id)
);

-- User Activity Logs table
CREATE TABLE user_activity_logs
(
    id            BIGSERIAL PRIMARY KEY,
    user_id       BIGINT       NOT NULL,
    activity_type VARCHAR(100) NOT NULL,
    description   TEXT,
    metadata      JSONB,
    created_at    TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);

-- Quiz Questions table
CREATE TABLE quiz_questions
(
    id              BIGSERIAL PRIMARY KEY,
    question        TEXT NOT NULL,
    answer          TEXT NOT NULL,
    difficulty      quiz_difficulty          DEFAULT 'EASY',
    topic           TEXT,
    source          TEXT,
    additional_info TEXT,
    is_user_created BOOLEAN                  DEFAULT FALSE,
    creator_id      BIGINT,
    external_id     VARCHAR(255),
    usage_count     INTEGER                  DEFAULT 0,
    created_at      TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    last_used       TIMESTAMP WITH TIME ZONE,
    FOREIGN KEY (creator_id) REFERENCES users (id) ON DELETE SET NULL
);

-- Quiz Sessions table
CREATE TABLE quiz_sessions
(
    id              BIGSERIAL PRIMARY KEY,
    user_id         BIGINT NOT NULL,
    challenge_id    BIGINT,
    difficulty      quiz_difficulty          DEFAULT 'EASY',
    question_source question_source          DEFAULT 'app',
    total_questions INTEGER                  DEFAULT 0,
    correct_answers INTEGER                  DEFAULT 0,
    status          quiz_session_status      DEFAULT 'CREATED',
    started_at      TIMESTAMP WITH TIME ZONE,
    completed_at    TIMESTAMP WITH TIME ZONE,
    created_at      TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    FOREIGN KEY (challenge_id) REFERENCES challenges (id) ON DELETE SET NULL
);

-- Quiz Rounds table
CREATE TABLE quiz_rounds
(
    id              BIGSERIAL PRIMARY KEY,
    quiz_session_id BIGINT                   NOT NULL,
    question_id     BIGINT                   NOT NULL,
    user_answer     VARCHAR(500),
    is_correct      BOOLEAN,
    answered_at     TIMESTAMP WITH TIME ZONE,
    time_taken      INTEGER,
    round_number    INTEGER                  NOT NULL,
    created_at      TIMESTAMP WITH TIME ZONE          DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (quiz_session_id) REFERENCES quiz_sessions (id) ON DELETE CASCADE,
    FOREIGN KEY (question_id) REFERENCES quiz_questions (id) ON DELETE CASCADE
);
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

-- Add comments for documentation
COMMENT
ON TABLE photos IS 'Table to store photo metadata and S3 information';
COMMENT
ON COLUMN photos.file_path IS 'S3 key/path where the file is stored';
COMMENT
ON COLUMN photos.s3_key IS 'S3 object key for direct S3 operations';
COMMENT
ON COLUMN photos.s3_url IS 'Full S3 or CloudFront URL for the image';
COMMENT
ON COLUMN photos.entity_id IS 'ID of the entity this photo belongs to (user, challenge, etc.)';
COMMENT
ON COLUMN photos.photo_type IS 'Type of photo (AVATAR, QUIZ_QUESTION, etc.)';
COMMENT
ON COLUMN photos.processing_status IS 'Current processing status of the photo';


-- ================================
-- INDEXES FOR PERFORMANCE
-- ================================

-- Users table indexes
CREATE INDEX idx_users_email ON users (email);
CREATE INDEX idx_users_username ON users (username);

-- Groups table indexes
CREATE INDEX idx_groups_creator ON groups (creator_id);
CREATE INDEX idx_groups_type ON groups (type);

-- Quests table indexes
CREATE INDEX idx_quests_creator ON quests (creator_id);
CREATE INDEX idx_quests_status ON quests (status);
CREATE INDEX idx_quests_challenge ON quests (challenge_id);

CREATE INDEX idx_verification_details_challenge_id ON verification_details (challenge_id);
CREATE INDEX idx_verification_details_location_id ON verification_details (location_coordinates_id);
CREATE INDEX idx_verification_details_photo_id ON verification_details (photo_details_id);
CREATE INDEX idx_verification_details_activity_type ON verification_details (activity_type);

-- Tasks table indexes
CREATE INDEX idx_tasks_quest ON tasks (quest_id);
CREATE INDEX idx_tasks_assigned_to ON tasks (assigned_to);
CREATE INDEX idx_tasks_challenge ON tasks (challenge_id);
CREATE INDEX idx_tasks_status ON tasks (status);

-- Challenges table indexes
CREATE INDEX idx_challenges_creator ON challenges (creator_id);
CREATE INDEX idx_challenges_group ON challenges (group_id);
CREATE INDEX idx_challenges_status ON challenges (status);
CREATE INDEX idx_challenges_dates ON challenges (start_date, end_date);

-- Challenge Progress indexes
CREATE INDEX idx_challenge_progress_user ON challenge_progress (user_id);
CREATE INDEX idx_challenge_progress_challenge ON challenge_progress (challenge_id);
CREATE INDEX idx_challenge_progress_status ON challenge_progress (status);

-- Challenge Progress Completed Days index
CREATE INDEX idx_challenge_progress_completed_days
    ON challenge_progress_completed_days (challenge_progress_id);

-- Rewards table indexes
CREATE INDEX idx_rewards_quest ON rewards (quest_id);

-- User Connections indexes
CREATE INDEX idx_user_connections_user ON user_connections (user_id);
CREATE INDEX idx_user_connections_connected ON user_connections (connected_user_id);
CREATE INDEX idx_user_connections_status ON user_connections (status);

-- User Activity Logs indexes
CREATE INDEX idx_user_activity_logs_user ON user_activity_logs (user_id);
CREATE INDEX idx_user_activity_logs_type ON user_activity_logs (activity_type);
CREATE INDEX idx_user_activity_logs_created ON user_activity_logs (created_at);

-- Quiz Questions indexes
CREATE INDEX idx_quiz_questions_difficulty ON quiz_questions (difficulty);
CREATE INDEX idx_quiz_questions_topic ON quiz_questions (topic);
CREATE INDEX idx_quiz_questions_creator ON quiz_questions (creator_id);
CREATE INDEX idx_quiz_questions_external_id ON quiz_questions (external_id);

-- Quiz Sessions indexes
CREATE INDEX idx_quiz_sessions_user ON quiz_sessions (user_id);
CREATE INDEX idx_quiz_sessions_challenge ON quiz_sessions (challenge_id);
CREATE INDEX idx_quiz_sessions_status ON quiz_sessions (status);

-- Quiz Rounds indexes
CREATE INDEX idx_quiz_rounds_session ON quiz_rounds (quiz_session_id);
CREATE INDEX idx_quiz_rounds_question ON quiz_rounds (question_id);

-- Task Completions indexes
CREATE INDEX idx_task_completions_task ON task_completions (task_id);
CREATE INDEX idx_task_completions_user ON task_completions (user_id);
CREATE INDEX idx_task_completions_status ON task_completions (status);

-- Create indexes for better query performance
CREATE INDEX idx_photos_entity_id_photo_type ON photos (entity_id, photo_type);
CREATE INDEX idx_photos_uploaded_by ON photos (uploaded_by);
CREATE INDEX idx_photos_photo_type ON photos (photo_type);
CREATE INDEX idx_photos_s3_key ON photos (s3_key);
CREATE INDEX idx_photos_created_at ON photos (created_at);
CREATE INDEX idx_photos_processing_status ON photos (processing_status);

CREATE INDEX idx_media_files_uploaded_by ON media_files (uploaded_by);
CREATE INDEX idx_media_files_entity_id ON media_files (entity_id);
CREATE INDEX idx_media_files_media_type ON media_files (media_type);
CREATE INDEX idx_media_files_media_category ON media_files (media_category);
CREATE INDEX idx_media_files_processing_status ON media_files (processing_status);
CREATE INDEX idx_media_files_uploaded_at ON media_files (uploaded_at);
CREATE INDEX idx_media_files_created_at ON media_files (created_at);
CREATE INDEX idx_media_files_content_type ON media_files (content_type);
CREATE INDEX idx_media_files_s3_key ON media_files (s3_key);

-- Composite indexes for common queries
CREATE INDEX idx_media_files_uploaded_by_category ON media_files (uploaded_by, media_category);
CREATE INDEX idx_media_files_entity_category ON media_files (entity_id, media_category) WHERE entity_id IS NOT NULL;
CREATE INDEX idx_media_files_processing_media_type ON media_files (processing_status, media_type);
CREATE INDEX idx_media_files_s3_key_status ON media_files (s3_key, processing_status) WHERE s3_key IS NOT NULL;


-- Function to update updated_at timestamp
CREATE
OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at
= CURRENT_TIMESTAMP;
RETURN NEW;
END;
$$
LANGUAGE plpgsql;

CREATE
OR REPLACE FUNCTION is_image(media_t media_type)
RETURNS BOOLEAN AS $$
BEGIN
RETURN media_t = 'IMAGE';
END;
$$
LANGUAGE plpgsql IMMUTABLE;

-- Function to check if media is video (matches isVideo() method)
CREATE
OR REPLACE FUNCTION is_video(media_t media_type)
RETURNS BOOLEAN AS $$
BEGIN
RETURN media_t = 'VIDEO';
END;
$$
LANGUAGE plpgsql IMMUTABLE;

-- Function to check if media is audio (matches isAudio() method)
CREATE
OR REPLACE FUNCTION is_audio(media_t media_type)
RETURNS BOOLEAN AS $$
BEGIN
RETURN media_t = 'AUDIO';
END;
$$
LANGUAGE plpgsql IMMUTABLE;

-- Function to check if processing is completed (matches isProcessingCompleted() method)
CREATE
OR REPLACE FUNCTION is_processing_completed(proc_status processing_status)
RETURNS BOOLEAN AS $$
BEGIN
RETURN proc_status = 'COMPLETED';
END;
$$
LANGUAGE plpgsql IMMUTABLE;

-- Function to check if processing failed (matches isProcessingFailed() method)
CREATE
OR REPLACE FUNCTION is_processing_failed(proc_status processing_status)
RETURNS BOOLEAN AS $$
BEGIN
RETURN proc_status = 'FAILED';
END;
$$
LANGUAGE plpgsql IMMUTABLE;

-- Stored procedures for common operations

-- Get media files by user
CREATE
OR REPLACE FUNCTION get_media_files_by_user(
    user_id BIGINT,
    media_t media_type DEFAULT NULL,
    proc_status processing_status DEFAULT NULL,
    limit_count INTEGER DEFAULT 20,
    offset_count INTEGER DEFAULT 0
)
RETURNS TABLE (
    id BIGINT,
    original_filename VARCHAR(255),
    filename VARCHAR(255),
    s3_url TEXT,
    media_type media_type,
    media_category media_category,
    processing_status processing_status,
    file_size BIGINT,
    uploaded_at TIMESTAMP WITHOUT TIME ZONE
) AS $$
BEGIN
RETURN QUERY
SELECT mf.id,
       mf.original_filename,
       mf.filename,
       mf.s3_url,
       mf.media_type,
       mf.media_category,
       mf.processing_status,
       mf.file_size,
       mf.uploaded_at
FROM media_files mf
WHERE mf.uploaded_by = user_id
  AND (media_cat IS NULL OR mf.media_category = media_cat)
  AND (proc_status IS NULL OR mf.processing_status = proc_status)
ORDER BY mf.uploaded_at DESC LIMIT limit_count
OFFSET offset_count;
END;
$$
LANGUAGE plpgsql;

-- Get media files by entity
CREATE
OR REPLACE FUNCTION get_media_files_by_entity(
    ent_id BIGINT,
    media_t media_type DEFAULT NULL,
    limit_count INTEGER DEFAULT 50
)
RETURNS TABLE (
    id BIGINT,
    original_filename VARCHAR(255),
    filename VARCHAR(255),
    s3_url TEXT,
    media_t media_type,
    processing_status processing_status,
    uploaded_at TIMESTAMP WITHOUT TIME ZONE
) AS $$
BEGIN
RETURN QUERY
SELECT mf.id,
       mf.original_filename,
       mf.filename,
       mf.s3_url,
       mf.media_category,
       mf.processing_status,
       mf.uploaded_at
FROM media_files mf
WHERE mf.entity_id = ent_id
  AND (media_cat IS NULL OR mf.media_category = media_cat)
  AND mf.processing_status = 'COMPLETED'
ORDER BY mf.uploaded_at DESC LIMIT limit_count;
END;
$$
LANGUAGE plpgsql;

-- Update processing status
CREATE
OR REPLACE FUNCTION update_processing_status(
    file_id BIGINT,
    new_status processing_status,
    new_processed_path VARCHAR(500) DEFAULT NULL,
    new_thumbnail_path VARCHAR(500) DEFAULT NULL
)
RETURNS BOOLEAN AS $$
BEGIN
UPDATE media_files
SET processing_status = new_status,
    processed_path    = COALESCE(new_processed_path, processed_path),
    thumbnail_path    = COALESCE(new_thumbnail_path, thumbnail_path),
    updated_at        = CURRENT_TIMESTAMP
WHERE id = file_id;

RETURN
FOUND;
END;
$$
LANGUAGE plpgsql;

-- Get files pending processing
CREATE
OR REPLACE FUNCTION get_pending_processing_files(limit_count INTEGER DEFAULT 10)
RETURNS TABLE (
    id BIGINT,
    filename VARCHAR(255),
    file_path VARCHAR(500),
    s3_key VARCHAR(500),
    s3_url TEXT,
    media_type media_type,
    content_type VARCHAR(100),
    uploaded_at TIMESTAMP WITHOUT TIME ZONE
) AS $$
BEGIN
RETURN QUERY
SELECT mf.id,
       mf.filename,
       mf.file_path,
       mf.s3_key,
       mf.s3_url,
       mf.media_type,
       mf.content_type,
       mf.uploaded_at
FROM media_files mf
WHERE mf.processing_status = 'PENDING'
ORDER BY mf.uploaded_at ASC LIMIT limit_count;
END;
$$
LANGUAGE plpgsql;


-- Apply triggers to tables with updated_at column
CREATE TRIGGER update_users_updated_at
    BEFORE UPDATE
    ON users
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_groups_updated_at
    BEFORE UPDATE
    ON groups
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_quests_updated_at
    BEFORE UPDATE
    ON quests
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_tasks_updated_at
    BEFORE UPDATE
    ON tasks
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_challenges_updated_at
    BEFORE UPDATE
    ON challenges
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_challenge_progress_updated_at
    BEFORE UPDATE
    ON challenge_progress
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_user_connections_updated_at
    BEFORE UPDATE
    ON user_connections
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_quiz_questions_updated_at
    BEFORE UPDATE
    ON quiz_questions
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_quiz_sessions_updated_at
    BEFORE UPDATE
    ON quiz_sessions
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- Trigger to automatically update updated_at
CREATE TRIGGER update_media_files_updated_at
    BEFORE UPDATE
    ON media_files
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- -- Trigger to automatically extract file extension
-- CREATE TRIGGER extract_media_files_extension
--     BEFORE INSERT OR
-- UPDATE ON media_files
--     FOR EACH ROW
--     EXECUTE FUNCTION extract_file_extension();
--
-- create function extract_file_extension() returns trigger
--     language plpgsql
-- as
-- $$
-- BEGIN
--     IF
-- NEW.file_extension IS NULL AND NEW.original_filename IS NOT NULL THEN
--         NEW.file_extension = lower(substring(NEW.original_filename from '\.([^.]*)$'));
-- END IF;
-- RETURN NEW;
-- END;
-- $$;
--
-- alter function extract_file_extension() owner to challenger_user;
--



-- -- Trigger to automatically generate stored filename
-- CREATE TRIGGER generate_media_files_stored_filename
--     BEFORE INSERT
--     ON media_files
--     FOR EACH ROW
--     EXECUTE FUNCTION generate_stored_filename();
--
-- create function generate_stored_filename() returns trigger
--     language plpgsql
-- as
-- $$
-- BEGIN
--     IF
-- NEW.stored_filename IS NULL OR NEW.stored_filename = '' THEN
--         NEW.stored_filename = uuid_generate_v4()::text ||
--             CASE
--                 WHEN NEW.file_extension IS NOT NULL
--                 THEN '.' || NEW.file_extension
--                 ELSE ''
-- END;
-- END IF;
-- RETURN NEW;
-- END;
-- $$;
--
-- alter function generate_stored_filename() owner to challenger_user;
--


-- View for completed media files
CREATE VIEW completed_media_files AS
SELECT id,
       original_filename,
       filename,
       file_path,
       processed_path,
       thumbnail_path,
       content_type,
       file_size,
       media_type,
       media_category,
       entity_id,
       uploaded_by,
       uploaded_at,
       created_at,
       updated_at,
       width,
       height,
       duration_seconds,
       bitrate,
       frame_rate,
       resolution,
       s3_key,
       s3_url
FROM media_files
WHERE processing_status = 'COMPLETED';

-- View for media files with dimensions (images and videos)
CREATE VIEW media_files_with_dimensions AS
SELECT id,
       original_filename,
       filename,
       s3_url,
       media_type,
       media_category,
       width,
       height,
       resolution,
       file_size,
       uploaded_by,
       uploaded_at
FROM media_files
WHERE (width IS NOT NULL AND height IS NOT NULL)
  AND processing_status = 'COMPLETED';

-- View for user profile pictures
CREATE VIEW user_profile_pictures AS
SELECT DISTINCT
        ON (uploaded_by)
        id, original_filename, filename, s3_url, width, height, uploaded_by, uploaded_at
        FROM media_files
        WHERE media_category = 'PROFILE_PICTURE'
        AND processing_status = 'COMPLETED'
        ORDER BY uploaded_by, uploaded_at DESC;



ALTER TYPE challenge_type ADD VALUE IF NOT EXISTS 'QUIZ';
ALTER TYPE challenge_status ADD VALUE IF NOT EXISTS 'OPEN';
ALTER TYPE task_type ADD VALUE IF NOT EXISTS 'RECURRING';
ALTER TYPE reward_source RENAME VALUE 'SYSTEM' TO 'INDIVIDUAL';
ALTER TYPE reward_source ADD VALUE IF NOT EXISTS 'GROUP';
ALTER TYPE reward_source ADD VALUE IF NOT EXISTS 'SYSTEM';

-- 2. Fix tasks table - add missing date fields
ALTER TABLE tasks
    ADD COLUMN IF NOT EXISTS start_date TIMESTAMP WITH TIME ZONE;
ALTER TABLE tasks
    ADD COLUMN IF NOT EXISTS end_date TIMESTAMP WITH TIME ZONE;
-- Keep due_date for backward compatibility or remove if not needed
-- ALTER TABLE tasks DROP COLUMN due_date;

-- 3. Fix rewards table - add monetary fields
ALTER TABLE rewards
    ADD COLUMN IF NOT EXISTS monetary_value DOUBLE PRECISION;
ALTER TABLE rewards
    ADD COLUMN IF NOT EXISTS currency currency_type;
-- Remove or rename the existing value column
ALTER TABLE rewards RENAME COLUMN value TO monetary_value_old;


ALTER TABLE quiz_sessions
    ADD COLUMN IF NOT EXISTS host_user_id BIGINT NOT NULL;
ALTER TABLE quiz_sessions
    ADD COLUMN IF NOT EXISTS team_name VARCHAR (200);
ALTER TABLE quiz_sessions
    ADD COLUMN IF NOT EXISTS team_members VARCHAR (1000); -- JSON array as string
ALTER TABLE quiz_sessions
    ADD COLUMN IF NOT EXISTS round_time_seconds INTEGER;
ALTER TABLE quiz_sessions
    ADD COLUMN IF NOT EXISTS total_rounds INTEGER;
ALTER TABLE quiz_sessions
    ADD COLUMN IF NOT EXISTS completed_rounds INTEGER DEFAULT 0;
ALTER TABLE quiz_sessions
    ADD COLUMN IF NOT EXISTS enable_ai_host BOOLEAN DEFAULT FALSE;
ALTER TABLE quiz_sessions
    ADD COLUMN IF NOT EXISTS total_duration_seconds INTEGER;
ALTER TABLE quiz_sessions
    ADD COLUMN IF NOT EXISTS creator_id BIGINT NOT NULL;


ALTER TABLE quiz_rounds
    ADD COLUMN IF NOT EXISTS team_answer VARCHAR (500);
ALTER TABLE quiz_rounds
    ADD COLUMN IF NOT EXISTS player_who_answered VARCHAR (200);
ALTER TABLE quiz_rounds
    ADD COLUMN IF NOT EXISTS discussion_notes VARCHAR (2000);
ALTER TABLE quiz_rounds
    ADD COLUMN IF NOT EXISTS round_started_at TIMESTAMP WITH TIME ZONE;
ALTER TABLE quiz_rounds
    ADD COLUMN IF NOT EXISTS discussion_started_at TIMESTAMP WITH TIME ZONE;
ALTER TABLE quiz_rounds
    ADD COLUMN IF NOT EXISTS answer_submitted_at TIMESTAMP WITH TIME ZONE;
ALTER TABLE quiz_rounds
    ADD COLUMN IF NOT EXISTS discussion_duration_seconds INTEGER;
ALTER TABLE quiz_rounds
    ADD COLUMN IF NOT EXISTS total_round_duration_seconds INTEGER;
ALTER TABLE quiz_rounds
    ADD COLUMN IF NOT EXISTS hint_used BOOLEAN DEFAULT FALSE;
ALTER TABLE quiz_rounds
    ADD COLUMN IF NOT EXISTS voice_recording_used BOOLEAN DEFAULT FALSE;
ALTER TABLE quiz_rounds
    ADD COLUMN IF NOT EXISTS ai_feedback VARCHAR (1000);

-- Rename existing columns to match H2 if needed
-- ALTER TABLE quiz_rounds RENAME COLUMN answered_at TO answer_submitted_at;
-- ALTER TABLE quiz_rounds RENAME COLUMN time_taken TO total_round_duration_seconds;

-- 7. Update quiz_questions table with missing fields
ALTER TABLE quiz_questions
    ADD COLUMN IF NOT EXISTS challenge_id BIGINT;
ALTER TABLE quiz_questions
    ADD COLUMN IF NOT EXISTS media_url VARCHAR (500);
ALTER TABLE quiz_questions
    ADD COLUMN IF NOT EXISTS has_media BOOLEAN DEFAULT FALSE;
ALTER TABLE quiz_questions
    ADD COLUMN IF NOT EXISTS media_type media_type; -- IMAGE, VIDEO, AUDIO
ALTER TABLE quiz_questions
    ADD COLUMN IF NOT EXISTS is_active BOOLEAN DEFAULT TRUE;

-- Add foreign key constraint
ALTER TABLE quiz_questions
    ADD CONSTRAINT fk_quiz_questions_challenge
        FOREIGN KEY (challenge_id) REFERENCES challenges (id) ON DELETE CASCADE;

-- 8. Update stakes table structure to match H2
-- The H2 version doesn't have user_id and is_refunded fields
-- ALTER TABLE stakes DROP COLUMN IF EXISTS user_id;
-- ALTER TABLE stakes DROP COLUMN IF EXISTS is_refunded;
ALTER TABLE stakes
    ADD COLUMN IF NOT EXISTS collective_pool BOOLEAN DEFAULT FALSE;

-- 9. Update challenge_progress table (fields already added via ALTER in H2, but let's ensure)
ALTER TABLE challenge_progress
    ADD COLUMN IF NOT EXISTS streak INTEGER;
ALTER TABLE challenge_progress
    ADD COLUMN IF NOT EXISTS total_rewards_earned DOUBLE PRECISION;

-- 10. Ensure reward_users table exists (junction table)
CREATE TABLE IF NOT EXISTS reward_users
(
    reward_id
    BIGINT,
    user_id
    BIGINT,
    awarded_date
    TIMESTAMP
    WITH
    TIME
    ZONE
    DEFAULT
    CURRENT_TIMESTAMP,
    PRIMARY
    KEY
(
    reward_id,
    user_id
),
    FOREIGN KEY
(
    reward_id
) REFERENCES rewards
(
    id
) ON DELETE CASCADE,
    FOREIGN KEY
(
    user_id
) REFERENCES users
(
    id
)
  ON DELETE CASCADE
    );
-- Migration script to add difficulty column to challenges table
-- File: src/main/resources/db/migration/V{next_version_number}__add_difficulty_to_challenges.sql

-- Create custom enum type for challenge difficulty
CREATE TYPE challenge_difficulty_type AS ENUM (
    'BEGINNER',
    'EASY',
    'MEDIUM',
    'HARD',
    'EXPERT',
    'EXTREME'
);

-- Add difficulty column to challenges table using the custom enum type
ALTER TABLE challenges
    ADD COLUMN difficulty challenge_difficulty_type NOT NULL DEFAULT 'MEDIUM';

-- Create index on difficulty for better query performance
CREATE INDEX idx_challenges_difficulty ON challenges (difficulty);

-- Optional: Update existing records with default difficulty based on type or other criteria
-- Example: Set QUIZ challenges to EASY by default
UPDATE challenges
SET difficulty = 'EASY'
WHERE type = 'QUIZ'
  AND difficulty = 'MEDIUM';

-- Example: Set EVENT challenges to HARD by default
UPDATE challenges
SET difficulty = 'HARD'
WHERE type = 'EVENT'
  AND difficulty = 'MEDIUM';

-- Add comment to the column for documentation
COMMENT
ON COLUMN challenges.difficulty IS 'Challenge difficulty level: BEGINNER, EASY, MEDIUM, HARD, EXPERT, EXTREME';

-- Optional: Create view for difficulty statistics
CREATE
OR REPLACE VIEW challenge_difficulty_stats AS
SELECT difficulty,
       COUNT(*)                                                    as total_challenges,
       COUNT(CASE WHEN status = 'ACTIVE' THEN 1 END)               as active_challenges,
       COUNT(CASE WHEN status = 'COMPLETED' THEN 1 END)            as completed_challenges,
       AVG(CASE WHEN status = 'COMPLETED' THEN 100.0 ELSE 0.0 END) as completion_rate
FROM challenges
GROUP BY difficulty
ORDER BY CASE difficulty
             WHEN 'BEGINNER' THEN 1
             WHEN 'EASY' THEN 2
             WHEN 'MEDIUM' THEN 3
             WHEN 'HARD' THEN 4
             WHEN 'EXPERT' THEN 5
             WHEN 'EXTREME' THEN 6
             END;



-- V001__Add_Multimedia_Support_To_Quiz_Questions.sql
-- Migration to add multimedia support to quiz questions

-- Add new columns to quiz_questions table for multimedia support
ALTER TABLE quiz_questions
    ADD COLUMN IF NOT EXISTS question_type VARCHAR (20) DEFAULT 'TEXT';
ALTER TABLE quiz_questions
    ADD COLUMN IF NOT EXISTS question_media_url VARCHAR (500);
ALTER TABLE quiz_questions
    ADD COLUMN IF NOT EXISTS question_media_id VARCHAR (100);
ALTER TABLE quiz_questions
    ADD COLUMN IF NOT EXISTS question_media_type VARCHAR (50);
ALTER TABLE quiz_questions
    ADD COLUMN IF NOT EXISTS question_thumbnail_url VARCHAR (500);

-- Create index for question_type for faster queries
CREATE INDEX IF NOT EXISTS idx_quiz_questions_question_type ON quiz_questions(question_type);
CREATE INDEX IF NOT EXISTS idx_quiz_questions_media_id ON quiz_questions(question_media_id);

-- Enhanced media_files table with better support for quiz media
CREATE TABLE IF NOT EXISTS media_files
(
    id
    BIGSERIAL
    PRIMARY
    KEY,
    original_file_name
    VARCHAR
(
    255
) NOT NULL,
    stored_file_name VARCHAR
(
    255
) NOT NULL,
    s3_key VARCHAR
(
    500
) NOT NULL UNIQUE,
    file_type VARCHAR
(
    100
) NOT NULL,
    file_size BIGINT NOT NULL,
    media_category VARCHAR
(
    50
) NOT NULL DEFAULT 'QUIZ_QUESTION',
    media_type VARCHAR
(
    20
) NOT NULL DEFAULT 'IMAGE',
    processing_status VARCHAR
(
    20
) NOT NULL DEFAULT 'PENDING',

    -- Entity association
    entity_id BIGINT,
    entity_type VARCHAR
(
    50
),

    -- Media metadata
    width INTEGER,
    height INTEGER,
    duration_seconds INTEGER,
    thumbnail_url VARCHAR
(
    500
),
    metadata JSONB,

    -- User tracking
    uploaded_by BIGINT NOT NULL,

    -- Timestamps
    uploaded_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    processed_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_media_files_uploaded_by FOREIGN KEY
(
    uploaded_by
) REFERENCES users
(
    id
) ON DELETE CASCADE
    );

-- Create indexes for media_files
CREATE INDEX IF NOT EXISTS idx_media_files_entity ON media_files(entity_id, entity_type);
CREATE INDEX IF NOT EXISTS idx_media_files_uploaded_by ON media_files(uploaded_by);
CREATE INDEX IF NOT EXISTS idx_media_files_s3_key ON media_files(s3_key);
CREATE INDEX IF NOT EXISTS idx_media_files_processing_status ON media_files(processing_status);
CREATE INDEX IF NOT EXISTS idx_media_files_media_category ON media_files(media_category);
CREATE INDEX IF NOT EXISTS idx_media_files_created_at ON media_files(created_at);

-- Add foreign key constraint between quiz_questions and media_files
ALTER TABLE quiz_questions
    ADD CONSTRAINT fk_quiz_questions_media_id
        FOREIGN KEY (question_media_id) REFERENCES media_files (id) ON DELETE SET NULL;

-- Update existing records to have default question_type
UPDATE quiz_questions
SET question_type = 'TEXT'
WHERE question_type IS NULL;

-- Make question_type NOT NULL after setting defaults
ALTER TABLE quiz_questions
    ALTER COLUMN question_type SET NOT NULL;

-- Enhanced quiz_rounds table to support multimedia
ALTER TABLE quiz_rounds
    ADD COLUMN IF NOT EXISTS media_interaction_count INTEGER DEFAULT 0;
ALTER TABLE quiz_rounds
    ADD COLUMN IF NOT EXISTS media_play_duration INTEGER; -- in seconds
ALTER TABLE quiz_rounds
    ADD COLUMN IF NOT EXISTS response_metadata JSONB;

-- Create a view for easy multimedia question queries
CREATE
OR REPLACE VIEW multimedia_quiz_questions AS
SELECT qq.id,
       qq.question,
       qq.answer,
       qq.difficulty,
       qq.topic,
       qq.question_type,
       qq.question_media_url,
       qq.question_thumbnail_url,
       qq.is_user_created,
       qq.creator_id,
       u.username            as creator_name,
       qq.usage_count,
       qq.created_at,
       qq.updated_at,
       mf.id                 as media_id,
       mf.original_file_name as media_filename,
       mf.file_type          as media_mime_type,
       mf.file_size          as media_size,
       mf.duration_seconds   as media_duration,
       mf.width              as media_width,
       mf.height             as media_height,
       mf.processing_status  as media_processing_status,
       mf.thumbnail_url      as media_thumbnail_url
FROM quiz_questions qq
         LEFT JOIN users u ON qq.creator_id = u.id
         LEFT JOIN media_files mf ON qq.question_media_id = mf.id::varchar;

-- Function to clean up orphaned media files
CREATE
OR REPLACE FUNCTION cleanup_orphaned_media_files()
RETURNS INTEGER AS $$
DECLARE
deleted_count INTEGER;
BEGIN
    -- Delete media files that are not referenced by any quiz question
    -- and are older than 24 hours (in case of temporary uploads)
DELETE
FROM media_files
WHERE entity_type = 'quiz_question'
  AND id::varchar NOT IN (
            SELECT question_media_id
            FROM quiz_questions
            WHERE question_media_id IS NOT NULL
        )
        AND created_at < NOW() - INTERVAL '24 hours';

GET DIAGNOSTICS deleted_count = ROW_COUNT;

RETURN deleted_count;
END;
$$
LANGUAGE plpgsql;

-- Function to update media usage statistics
CREATE
OR REPLACE FUNCTION update_media_usage_stats()
RETURNS TRIGGER AS $$
BEGIN
    -- Update usage count when a question with media is used in a quiz round
    IF
NEW.question_id IS NOT NULL THEN
UPDATE quiz_questions
SET usage_count = usage_count + 1
WHERE id = NEW.question_id
  AND question_media_id IS NOT NULL;
END IF;

RETURN NEW;
END;
$$
LANGUAGE plpgsql;

-- Create trigger for media usage tracking
DROP TRIGGER IF EXISTS trigger_update_media_usage ON quiz_rounds;
CREATE TRIGGER trigger_update_media_usage
    AFTER INSERT
    ON quiz_rounds
    FOR EACH ROW
    EXECUTE FUNCTION update_media_usage_stats();


-- Create enum types for better type safety (PostgreSQL)
DO
$$
BEGIN
    IF
NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'question_type_enum') THEN
CREATE TYPE question_type_enum AS ENUM ('TEXT', 'IMAGE', 'VIDEO', 'AUDIO');
END IF;

--     IF
-- NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'media_category') THEN
-- CREATE TYPE media_category AS ENUM ('QUIZ_QUESTION', 'AVATAR', 'CHALLENGE_PROOF', 'SYSTEM');
-- END IF;

    IF
NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'processing_status_enum') THEN
CREATE TYPE processing_status_enum AS ENUM ('PENDING', 'PROCESSING', 'COMPLETED', 'FAILED');
END IF;
END
$$;

-- Update column types to use enums (optional, for better type safety)
-- ALTER TABLE quiz_questions ALTER COLUMN question_type TYPE question_type_enum USING question_type::question_type_enum;
-- ALTER TABLE media_files ALTER COLUMN media_category TYPE media_category_enum USING media_category::media_category_enum;
-- ALTER TABLE media_files ALTER COLUMN processing_status TYPE processing_status_enum USING processing_status::processing_status_enum;

-- Add useful stored procedures for media management

-- Procedure to get media statistics
CREATE
OR REPLACE FUNCTION get_media_statistics()
RETURNS TABLE (
    total_media_files BIGINT,
    total_size_mb NUMERIC,
    video_files BIGINT,
    audio_files BIGINT,
    image_files BIGINT,
    questions_with_media BIGINT,
    avg_media_file_size_mb NUMERIC
) AS $$
BEGIN
RETURN QUERY
SELECT COUNT(*)                                        as total_media_files,
       ROUND(SUM(file_size)::NUMERIC / 1024 / 1024, 2) as total_size_mb,
       COUNT(*)                                           FILTER (WHERE file_type LIKE 'video/%') as video_files, COUNT(*) FILTER (WHERE file_type LIKE 'audio/%') as audio_files, COUNT(*) FILTER (WHERE file_type LIKE 'image/%') as image_files, (SELECT COUNT(*)
                                                                                                                                                                                                                                                     FROM quiz_questions
                                                                                                                                                                                                                                                     WHERE question_media_id IS NOT NULL) as questions_with_media,
       ROUND(AVG(file_size)::NUMERIC / 1024 / 1024, 2) as avg_media_file_size_mb
FROM media_files
WHERE media_category = 'QUIZ_QUESTION';
END;
$$
LANGUAGE plpgsql;

-- Procedure to get user media quota usage
CREATE
OR REPLACE FUNCTION get_user_media_quota(user_id BIGINT)
RETURNS TABLE (
    total_files BIGINT,
    total_size_mb NUMERIC,
    video_files BIGINT,
    video_size_mb NUMERIC,
    audio_files BIGINT,
    audio_size_mb NUMERIC,
    image_files BIGINT,
    image_size_mb NUMERIC
) AS $$
BEGIN
RETURN QUERY
SELECT COUNT(*)                                        as total_files,
       ROUND(SUM(file_size)::NUMERIC / 1024 / 1024, 2) as total_size_mb,
       COUNT(*)                                           FILTER (WHERE file_type LIKE 'video/%') as video_files, ROUND(SUM(file_size) FILTER (WHERE file_type LIKE 'video/%')::NUMERIC / 1024 / 1024, 2) as video_size_mb,
       COUNT(*)                                           FILTER (WHERE file_type LIKE 'audio/%') as audio_files, ROUND(SUM(file_size) FILTER (WHERE file_type LIKE 'audio/%')::NUMERIC / 1024 / 1024, 2) as audio_size_mb,
       COUNT(*)                                           FILTER (WHERE file_type LIKE 'image/%') as image_files, ROUND(SUM(file_size) FILTER (WHERE file_type LIKE 'image/%')::NUMERIC / 1024 / 1024, 2) as image_size_mb
FROM media_files
WHERE uploaded_by = user_id
  AND media_category = 'QUIZ_QUESTION';
END;
$$
LANGUAGE plpgsql;

-- Add comments for documentation
COMMENT
ON TABLE media_files IS 'Stores metadata for all uploaded media files including quiz question media';
COMMENT
ON COLUMN media_files.s3_key IS 'Unique S3 object key for the stored file';
COMMENT
ON COLUMN media_files.entity_id IS 'ID of the related entity (e.g., quiz_question.id)';
COMMENT
ON COLUMN media_files.entity_type IS 'Type of the related entity (e.g., quiz_question)';
COMMENT
ON COLUMN media_files.duration_seconds IS 'Duration for video/audio files in seconds';
COMMENT
ON COLUMN media_files.metadata IS 'Additional metadata stored as JSON';

COMMENT
ON COLUMN quiz_questions.question_type IS 'Type of question: TEXT, IMAGE, VIDEO, or AUDIO';
COMMENT
ON COLUMN quiz_questions.question_media_url IS 'URL to access the question media file';
COMMENT
ON COLUMN quiz_questions.question_media_id IS 'Foreign key to media_files.id';
COMMENT
ON COLUMN quiz_questions.question_thumbnail_url IS 'URL to thumbnail for video/image questions';

-- Grant permissions (adjust as needed for your setup)
-- GRANT SELECT, INSERT, UPDATE, DELETE ON media_files TO quiz_app_user;
-- GRANT SELECT, UPDATE ON quiz_questions TO quiz_app_user;
-- GRANT USAGE, SELECT ON SEQUENCE media_files_id_seq TO quiz_app_user;


        -- Create question_type enum (if not exists)
DO
$$
BEGIN
    IF
NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'question_type_enum') THEN
CREATE TYPE question_type_enum AS ENUM (
            'TEXT',
            'IMAGE',
            'AUDIO',
            'VIDEO',
            'MULTIMEDIA'
        );
END IF;
END$$;

-- Create media_type enum (if not exists)
DO
$$
BEGIN
    IF
NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'media_type_enum') THEN
CREATE TYPE media_type_enum AS ENUM (
            'IMAGE',
            'VIDEO',
            'AUDIO',
            'DOCUMENT',
            'QUIZ_QUESTION',
            'AVATAR'
        );
END IF;
END$$;

-- 2. Migration script to convert question_media_type from text to enum
-- =====================================================

-- Step 1: Add new enum column
ALTER TABLE quiz_questions
    ADD COLUMN question_media_type_enum media_type_enum;

-- Step 2: Update existing data - map string values to enum values
UPDATE quiz_questions
SET question_media_type_enum =
        CASE
            WHEN LOWER(question_media_type) IN ('image', 'img', 'jpeg', 'jpg', 'png', 'gif', 'webp')
                THEN 'IMAGE'::media_type_enum
            WHEN LOWER(question_media_type) IN ('video', 'mp4', 'mov', 'avi', 'webm', 'mkv')
                THEN 'VIDEO'::media_type_enum
            WHEN LOWER(question_media_type) IN ('audio', 'mp3', 'wav', 'ogg', 'm4a', 'aac')
                THEN 'AUDIO'::media_type_enum
            WHEN LOWER(question_media_type) IN ('document', 'pdf', 'doc', 'docx', 'txt')
                THEN 'DOCUMENT'::media_type_enum
            WHEN LOWER(question_media_type) IN ('quiz_question', 'quiz', 'question')
                THEN 'QUIZ_QUESTION'::media_type_enum
            WHEN LOWER(question_media_type) IN ('avatar', 'profile')
                THEN 'AVATAR'::media_type_enum
            WHEN question_media_type IS NULL OR question_media_type = ''
                THEN NULL
            ELSE 'QUIZ_QUESTION'::media_type_enum -- Default fallback
            END
WHERE question_media_type_enum IS NULL;

-- Step 3: Drop old column and rename new column
ALTER TABLE quiz_questions DROP COLUMN IF EXISTS question_media_type;
ALTER TABLE quiz_questions RENAME COLUMN question_media_type_enum TO question_media_type;

-- Step 4: Ensure question_type is also using enum (if not already)
-- Check if question_type column exists and is not already an enum
DO
$$
BEGIN
    -- Add enum column if question_type is still text
    IF
EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'quiz_questions'
        AND column_name = 'question_type'
        AND data_type = 'character varying'
    ) THEN
        -- Add new enum column
ALTER TABLE quiz_questions
    ADD COLUMN question_type_enum question_type_enum;

-- Update existing data
UPDATE quiz_questions
SET question_type_enum =
        CASE
            WHEN UPPER(question_type) = 'TEXT' THEN 'TEXT'::question_type_enum
            WHEN UPPER(question_type) = 'IMAGE' THEN 'IMAGE'::question_type_enum
            WHEN UPPER(question_type) = 'AUDIO' THEN 'AUDIO'::question_type_enum
            WHEN UPPER(question_type) = 'VIDEO' THEN 'VIDEO'::question_type_enum
            WHEN UPPER(question_type) = 'MULTIMEDIA' THEN 'MULTIMEDIA'::question_type_enum
            ELSE 'TEXT'::question_type_enum -- Default fallback
            END
WHERE question_type_enum IS NULL;

-- Drop old column and rename
ALTER TABLE quiz_questions DROP COLUMN question_type;
ALTER TABLE quiz_questions RENAME COLUMN question_type_enum TO question_type;

-- Make question_type NOT NULL with default
ALTER TABLE quiz_questions
    ALTER COLUMN question_type SET NOT NULL;
ALTER TABLE quiz_questions
    ALTER COLUMN question_type SET DEFAULT 'TEXT'::question_type_enum;
END IF;
END$$;

-- 3. Add indexes for better performance
-- =====================================================
CREATE INDEX IF NOT EXISTS idx_quiz_questions_question_type ON quiz_questions(question_type);
CREATE INDEX IF NOT EXISTS idx_quiz_questions_question_media_type ON quiz_questions(question_media_type);
CREATE INDEX IF NOT EXISTS idx_quiz_questions_type_media_composite ON quiz_questions(question_type, question_media_type);

-- 4. Add constraints
-- =====================================================
-- Add check constraint to ensure media type aligns with question type
ALTER TABLE quiz_questions DROP CONSTRAINT IF EXISTS chk_question_media_consistency;
ALTER TABLE quiz_questions
    ADD CONSTRAINT chk_question_media_consistency
        CHECK (
            (question_type = 'TEXT' AND question_media_type IS NULL) OR
            (question_type = 'IMAGE' AND question_media_type IN ('IMAGE', 'QUIZ_QUESTION')) OR
            (question_type = 'AUDIO' AND question_media_type IN ('AUDIO', 'QUIZ_QUESTION')) OR
            (question_type = 'VIDEO' AND question_media_type IN ('VIDEO', 'QUIZ_QUESTION')) OR
            (question_type = 'MULTIMEDIA' AND question_media_type IS NOT NULL)
            );

-- 5. Verify the migration
-- =====================================================
-- Check enum types exist
SELECT typname, typcategory, typowner
FROM pg_type
WHERE typname IN ('question_type_enum', 'media_type_enum');

-- Check table structure
SELECT column_name, data_type, udt_name, is_nullable, column_default
FROM information_schema.columns
WHERE table_name = 'quiz_questions'
  AND column_name IN ('question_type', 'question_media_type')
ORDER BY ordinal_position;

-- Check data distribution
SELECT question_type,
       question_media_type,
       COUNT(*) as count
FROM quiz_questions
GROUP BY question_type, question_media_type
ORDER BY question_type, question_media_type;



-- ============================================================================
-- Add created_by and frequency columns to tasks table
-- ============================================================================

-- Create frequency_type ENUM if not exists
CREATE TYPE frequency_type AS ENUM ('DAILY', 'WEEKLY', 'MONTHLY', 'ONE_TIME', 'CUSTOM');

-- Add created_by column
ALTER TABLE tasks
    ADD COLUMN IF NOT EXISTS created_by BIGINT;

-- Add frequency column
ALTER TABLE tasks
    ADD COLUMN IF NOT EXISTS frequency frequency_type;

-- Add foreign key constraint for created_by
ALTER TABLE tasks
    ADD CONSTRAINT fk_tasks_created_by
        FOREIGN KEY (created_by)
            REFERENCES users (id)
            ON DELETE SET NULL;

-- Create indexes
CREATE INDEX IF NOT EXISTS idx_tasks_created_by ON tasks(created_by);
CREATE INDEX IF NOT EXISTS idx_tasks_frequency ON tasks(frequency);


-- Step 1: Create the custom ENUM type (if not already exists)
CREATE TYPE activity_type AS ENUM (
    'TASK_COMPLETION',
    'JOIN_GROUP',
    'COMPLETE_QUEST',
    'REWARD_EARNED',
    'CONNECTION_MADE',
    'CHALLENGE_JOINED',
    'CHALLENGE_COMPLETED'
);

-- Step 2: Alter the column to use the new ENUM type
ALTER TABLE user_activity_logs
ALTER
COLUMN activity_type TYPE activity_type
    USING activity_type::activity_type;


CREATE TABLE IF NOT EXISTS questions
(
    id
    SERIAL
    PRIMARY
    KEY,
    tournament_id
    INTEGER
    NOT
    NULL,
    tournament_title
    TEXT
    NOT
    NULL,
    question_num
    INTEGER,
    question
    TEXT
    NOT
    NULL,
    answer
    TEXT
    NOT
    NULL,
    authors
    TEXT,
    sources
    TEXT,
    comments
    TEXT,
    pass_criteria
    TEXT,
    notices
    TEXT,
    images
    TEXT,
    rating
    INTEGER,
    tournament_type
    TEXT,
    topic
    TEXT,
    topic_num
    INTEGER,
    entered_date
    TIMESTAMP
    DEFAULT
    CURRENT_TIMESTAMP
);

-- Optional index for faster queries
CREATE INDEX IF NOT EXISTS idx_tournament_id ON questions (tournament_id);



-- ============================================
-- STEP 1: Backup existing data
-- ============================================
CREATE TABLE questions_backup AS
SELECT *
FROM questions;

-- ============================================
-- STEP 2: Rename old questions table
-- ============================================
ALTER TABLE questions RENAME TO questions_old;

CREATE TABLE tournament_questions
(
    id                  SERIAL PRIMARY KEY,
    quiz_question_id    BIGINT    NOT NULL,
    tournament_id       INTEGER   NOT NULL,
    tournament_title    TEXT      NOT NULL,

    -- Auto-generated sequential order (replacing unreliable question_num)
    display_order       INTEGER   NOT NULL,

    -- Keep old question_num for reference only
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

    CONSTRAINT fk_tournament_question_quiz_question
        FOREIGN KEY (quiz_question_id)
            REFERENCES quiz_questions (id)
            ON DELETE RESTRICT,

    CONSTRAINT uk_tournament_display_order
        UNIQUE (tournament_id, display_order)
);

-- Create indexes
CREATE INDEX idx_tournament_id ON tournament_questions (tournament_id);
CREATE INDEX idx_quiz_question_id ON tournament_questions (quiz_question_id);
CREATE INDEX idx_tournament_order ON tournament_questions (tournament_id, display_order);
CREATE INDEX idx_is_active ON tournament_questions (is_active);


-- src/main/resources/db/migration/V3__Create_Refresh_Token_Table.sql

CREATE TABLE refresh_tokens
(
    id          BIGSERIAL PRIMARY KEY,
    user_id     BIGINT       NOT NULL,
    token       VARCHAR(255) NOT NULL UNIQUE,
    expiry_date TIMESTAMP    NOT NULL,
    created_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_refresh_token_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);

CREATE INDEX idx_refresh_token_token ON refresh_tokens (token);
CREATE INDEX idx_refresh_token_user_id ON refresh_tokens (user_id);

ALTER TABLE quiz_questions
    ADD COLUMN legacy_question_id integer;

ALTER TABLE quiz_questions
    ADD CONSTRAINT fk_quiz_questions_legacy_question_id
        FOREIGN KEY (legacy_question_id) REFERENCES questions_old (id);

ALTER TABLE quiz_questions
    ADD COLUMN comments text;
ALTER TABLE quiz_questions
    ADD COLUMN authors text;
ALTER TABLE quiz_questions
    ADD COLUMN pass_criteria text;

-- Create topics table
CREATE TABLE topics (
                        id BIGSERIAL PRIMARY KEY,
                        name VARCHAR(100) NOT NULL UNIQUE,
                        category VARCHAR(50),
                        description TEXT,
                        is_active BOOLEAN NOT NULL DEFAULT TRUE,
                        question_count INTEGER DEFAULT 0,
                        creator_id BIGINT REFERENCES users(id),
                        created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                        updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Create indexes
CREATE INDEX idx_topic_name ON topics(name);
CREATE INDEX idx_topic_category ON topics(category);
CREATE INDEX idx_is_active ON topics(is_active);

-- Migrate existing topics from quiz_questions table
INSERT INTO topics (name, is_active, question_count)
SELECT DISTINCT
    topic as name,
    TRUE as is_active,
    COUNT(*) as question_count
FROM quiz_questions
WHERE topic IS NOT NULL AND topic != ''
GROUP BY topic;

-- Add topic_id column to quiz_questions
ALTER TABLE quiz_questions ADD COLUMN topic_id BIGINT;
ALTER TABLE quiz_questions ADD COLUMN legacy_topic VARCHAR(100);

-- Update quiz_questions with topic_id
UPDATE quiz_questions q
SET topic_id = t.id,
    legacy_topic = q.topic
    FROM topics t
WHERE LOWER(q.topic) = LOWER(t.name);

-- Create foreign key
ALTER TABLE quiz_questions
    ADD CONSTRAINT fk_quiz_question_topic
        FOREIGN KEY (topic_id) REFERENCES topics(id);

-- Create index on topic_id
CREATE INDEX idx_topic_id ON quiz_questions(topic_id);

-- Optional: Drop old topic column after verification
-- ALTER TABLE quiz_questions DROP COLUMN topic;


-- Migration: Add Payment and Access Control Features
-- Author: Challenge System
-- Date: 2025-01-10

-- Step 1: Create PaymentType ENUM
CREATE TYPE payment_type AS ENUM ('FREE', 'ENTRY_FEE', 'PRIZE_POOL', 'SUBSCRIPTION');

-- Step 2: Add payment fields to challenges table
ALTER TABLE challenges
    ADD COLUMN IF NOT EXISTS payment_type payment_type DEFAULT 'FREE',
    ADD COLUMN IF NOT EXISTS has_entry_fee BOOLEAN DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS entry_fee_amount DECIMAL(10, 2),
    ADD COLUMN IF NOT EXISTS entry_fee_currency currency_type,
    ADD COLUMN IF NOT EXISTS has_prize BOOLEAN DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS prize_amount DECIMAL(10, 2),
    ADD COLUMN IF NOT EXISTS prize_currency currency_type,
    ADD COLUMN IF NOT EXISTS prize_pool DECIMAL(10, 2) DEFAULT 0.00,
    ADD COLUMN IF NOT EXISTS requires_approval BOOLEAN DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP;

-- Step 3: Create challenge_access table for private challenge permissions
CREATE TABLE IF NOT EXISTS challenge_access (
                                                id BIGSERIAL PRIMARY KEY,
                                                challenge_id BIGINT NOT NULL,
                                                user_id BIGINT NOT NULL,
                                                granted_by_user_id BIGINT,
                                                granted_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                                status VARCHAR(20) DEFAULT 'ACTIVE',
    notes TEXT,
    CONSTRAINT fk_challenge_access_challenge FOREIGN KEY (challenge_id)
    REFERENCES challenges(id) ON DELETE CASCADE,
    CONSTRAINT fk_challenge_access_user FOREIGN KEY (user_id)
    REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_challenge_access_granted_by FOREIGN KEY (granted_by_user_id)
    REFERENCES users(id) ON DELETE SET NULL,
    CONSTRAINT uq_challenge_user UNIQUE (challenge_id, user_id)
    );

-- Step 4: Add indexes for performance
CREATE INDEX IF NOT EXISTS idx_challenge_access_challenge_id
    ON challenge_access(challenge_id);

CREATE INDEX IF NOT EXISTS idx_challenge_access_user_id
    ON challenge_access(user_id);

CREATE INDEX IF NOT EXISTS idx_challenge_access_status
    ON challenge_access(status);

CREATE INDEX IF NOT EXISTS idx_challenges_payment_type
    ON challenges(payment_type);

CREATE INDEX IF NOT EXISTS idx_challenges_is_public
    ON challenges(is_public);

CREATE INDEX IF NOT EXISTS idx_challenges_created_at
    ON challenges(created_at);

-- Step 5: Add points field to users table if not exists
ALTER TABLE users
    ADD COLUMN IF NOT EXISTS points BIGINT DEFAULT 0;

ALTER TABLE users
    ADD COLUMN IF NOT EXISTS total_points_spent BIGINT DEFAULT 0;
ALTER TABLE users
    ADD COLUMN IF NOT EXISTS total_points_earned BIGINT DEFAULT 0;


-- Step 6: Create payment_transactions table for audit trail
CREATE TABLE IF NOT EXISTS payment_transactions (
                                                    id BIGSERIAL PRIMARY KEY,
                                                    user_id BIGINT NOT NULL,
                                                    challenge_id BIGINT,
                                                    transaction_type VARCHAR(50) NOT NULL, -- ENTRY_FEE, PRIZE, REFUND
    amount DECIMAL(10, 2) NOT NULL,
    currency currency_type NOT NULL,
    status VARCHAR(20) DEFAULT 'PENDING', -- PENDING, COMPLETED, FAILED, REFUNDED
    payment_method VARCHAR(50), -- POINTS, CREDIT_CARD, PAYPAL, etc.
    transaction_reference VARCHAR(255),
    notes TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMP ,
    CONSTRAINT fk_payment_user FOREIGN KEY (user_id)
    REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_payment_challenge FOREIGN KEY (challenge_id)
    REFERENCES challenges(id) ON DELETE SET NULL
    );

-- Step 7: Create indexes for payment_transactions
CREATE INDEX IF NOT EXISTS idx_payment_transactions_user_id
    ON payment_transactions(user_id);

CREATE INDEX IF NOT EXISTS idx_payment_transactions_challenge_id
    ON payment_transactions(challenge_id);

CREATE INDEX IF NOT EXISTS idx_payment_transactions_status
    ON payment_transactions(status);

CREATE INDEX IF NOT EXISTS idx_payment_transactions_created_at
    ON payment_transactions(created_at);

-- Step 8: Add constraints for payment validation
ALTER TABLE challenges
    ADD CONSTRAINT chk_entry_fee_positive
        CHECK (entry_fee_amount IS NULL OR entry_fee_amount >= 0),
    ADD CONSTRAINT chk_prize_positive
        CHECK (prize_amount IS NULL OR prize_amount >= 0),
    ADD CONSTRAINT chk_prize_pool_positive
        CHECK (prize_pool >= 0);

-- Step 9: Create function to update updated_at timestamp
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
RETURN NEW;
END;
$$ language 'plpgsql';

-- Step 10: Create trigger for challenges table
DROP TRIGGER IF EXISTS update_challenges_updated_at ON challenges;
CREATE TRIGGER update_challenges_updated_at
    BEFORE UPDATE ON challenges
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- Step 11: Create trigger for payment_transactions table
DROP TRIGGER IF EXISTS update_payment_transactions_updated_at ON payment_transactions;
CREATE TRIGGER update_payment_transactions_updated_at
    BEFORE UPDATE ON payment_transactions
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- Step 12: Insert sample data (optional)
-- Example: Update existing challenges to have payment_type
UPDATE challenges
SET payment_type = 'FREE'
WHERE payment_type IS NULL;

-- Step 13: Create view for challenge statistics with payment info
CREATE OR REPLACE VIEW challenge_payment_stats AS
SELECT
    c.id,
    c.title,
    c.payment_type,
    c.entry_fee_amount,
    c.entry_fee_currency,
    c.prize_pool,
    COUNT(DISTINCT cp.user_id) as participant_count,
    SUM(CASE WHEN pt.status = 'COMPLETED' THEN pt.amount ELSE 0 END) as total_collected
FROM challenges c
         LEFT JOIN challenge_progress cp ON c.id = cp.challenge_id
         LEFT JOIN payment_transactions pt ON c.id = pt.challenge_id
    AND pt.transaction_type = 'ENTRY_FEE'
GROUP BY c.id, c.title, c.payment_type, c.entry_fee_amount,
         c.entry_fee_currency, c.prize_pool;

-- Step 14: Grant permissions (adjust as needed for your roles)
-- GRANT SELECT, INSERT, UPDATE, DELETE ON challenge_access TO your_app_user;
-- GRANT SELECT, INSERT, UPDATE, DELETE ON payment_transactions TO your_app_user;

-- Rollback script (save separately as rollback.sql)
-- DROP VIEW IF EXISTS challenge_payment_stats;
-- DROP TRIGGER IF EXISTS update_payment_transactions_updated_at ON payment_transactions;
-- DROP TRIGGER IF EXISTS update_challenges_updated_at ON challenges;
-- DROP FUNCTION IF EXISTS update_updated_at_column();
-- DROP TABLE IF EXISTS payment_transactions;
-- DROP TABLE IF EXISTS challenge_access;
-- ALTER TABLE challenges DROP COLUMN IF EXISTS payment_type, DROP COLUMN IF EXISTS has_entry_fee, ...
-- ALTER TABLE users DROP COLUMN IF EXISTS points;
-- DROP TYPE IF EXISTS payment_type;


-- ============================================================================
-- Migration: Add Question Access Policy and Friend/Family Relationships
-- ============================================================================

-- Step 1: Create ENUM type for question visibility
CREATE TYPE question_visibility AS ENUM (
    'PRIVATE',           -- Only visible to creator
    'FRIENDS_FAMILY',    -- Visible to creator and their friends/family
    'QUIZ_ONLY',         -- Only in the specific quiz/challenge where it was added
    'PUBLIC'             -- Available to everyone in question search
);

-- Step 2: Add visibility column to quiz_questions table
ALTER TABLE quiz_questions
    ADD COLUMN visibility question_visibility DEFAULT 'PRIVATE' NOT NULL,
ADD COLUMN original_quiz_id BIGINT,  -- Track which quiz this question was created in (if QUIZ_ONLY)
ADD CONSTRAINT fk_question_original_quiz
    FOREIGN KEY (original_quiz_id)
    REFERENCES challenges(id)
    ON DELETE SET NULL;

-- Step 3: Create user_relationships table for friends/family connections
CREATE TABLE user_relationships (
                                    id BIGSERIAL PRIMARY KEY,
                                    user_id BIGINT NOT NULL,
                                    related_user_id BIGINT NOT NULL,
                                    relationship_type VARCHAR(50) NOT NULL, -- 'FRIEND', 'FAMILY', 'BLOCKED'
                                    status VARCHAR(50) DEFAULT 'PENDING' NOT NULL, -- 'PENDING', 'ACCEPTED', 'REJECTED'
                                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
                                    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,

                                    CONSTRAINT fk_user_relationships_user
                                        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
                                    CONSTRAINT fk_user_relationships_related_user
                                        FOREIGN KEY (related_user_id) REFERENCES users(id) ON DELETE CASCADE,
                                    CONSTRAINT uk_user_relationship UNIQUE (user_id, related_user_id),
                                    CONSTRAINT chk_different_users CHECK (user_id != related_user_id)
    );

-- Step 4: Create indexes for performance
CREATE INDEX idx_quiz_questions_visibility ON quiz_questions(visibility);
CREATE INDEX idx_quiz_questions_creator_visibility ON quiz_questions(creator_id, visibility);
CREATE INDEX idx_quiz_questions_original_quiz ON quiz_questions(original_quiz_id);
CREATE INDEX idx_user_relationships_user ON user_relationships(user_id, status);
CREATE INDEX idx_user_relationships_related_user ON user_relationships(related_user_id, status);

-- Step 5: Create question_access_log table for audit trail
CREATE TABLE question_access_log (
                                     id BIGSERIAL PRIMARY KEY,
                                     question_id BIGINT NOT NULL,
                                     accessed_by_user_id BIGINT NOT NULL,
                                     access_type VARCHAR(50) NOT NULL, -- 'VIEW', 'USE_IN_QUIZ', 'EDIT', 'DELETE'
                                     accessed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,

                                     CONSTRAINT fk_question_access_log_question
                                         FOREIGN KEY (question_id) REFERENCES quiz_questions(id) ON DELETE CASCADE,
                                     CONSTRAINT fk_question_access_log_user
                                         FOREIGN KEY (accessed_by_user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX idx_question_access_log_question ON question_access_log(question_id);
CREATE INDEX idx_question_access_log_user ON question_access_log(accessed_by_user_id);

-- Step 6: Update existing user-created questions to PRIVATE by default
UPDATE quiz_questions
SET visibility = 'PRIVATE'
WHERE is_user_created = true;

-- Step 7: Add comment documentation
COMMENT ON TYPE question_visibility IS 'Controls who can view and use a user-created question';
COMMENT ON COLUMN quiz_questions.visibility IS 'Access policy for user-created questions';
COMMENT ON COLUMN quiz_questions.original_quiz_id IS 'Original quiz/challenge ID if visibility is QUIZ_ONLY';
COMMENT ON TABLE user_relationships IS 'Manages friend and family relationships between users';
COMMENT ON TABLE question_access_log IS 'Audit log for question access and usage';

-- Step 8: Create function to check question access
CREATE OR REPLACE FUNCTION can_user_access_question(
    p_question_id BIGINT,
    p_user_id BIGINT
) RETURNS BOOLEAN AS $$
DECLARE
v_visibility question_visibility;
    v_creator_id BIGINT;
    v_original_quiz_id BIGINT;
    v_has_relationship BOOLEAN;
BEGIN
    -- Get question details
SELECT visibility, creator_id, original_quiz_id
INTO v_visibility, v_creator_id, v_original_quiz_id
FROM quiz_questions
WHERE id = p_question_id;

-- If question doesn't exist, return false
IF NOT FOUND THEN
        RETURN FALSE;
END IF;

    -- Creator always has access
    IF v_creator_id = p_user_id THEN
        RETURN TRUE;
END IF;

    -- Check based on visibility
CASE v_visibility
        WHEN 'PUBLIC' THEN
            RETURN TRUE;

WHEN 'PRIVATE' THEN
            RETURN FALSE;

WHEN 'FRIENDS_FAMILY' THEN
            -- Check if users are friends/family
SELECT EXISTS (
    SELECT 1 FROM user_relationships
    WHERE ((user_id = v_creator_id AND related_user_id = p_user_id)
        OR (user_id = p_user_id AND related_user_id = v_creator_id))
      AND status = 'ACCEPTED'
      AND relationship_type IN ('FRIEND', 'FAMILY')
) INTO v_has_relationship;
RETURN v_has_relationship;

WHEN 'QUIZ_ONLY' THEN
            -- Check if user has access to the original quiz
            -- This would need to check challenge_access or user_quests tables
            RETURN EXISTS (
                SELECT 1 FROM challenges c
                WHERE c.id = v_original_quiz_id
                  AND (c.creator_id = p_user_id
                       OR c.visibility = 'PUBLIC'
                       OR EXISTS (
                           SELECT 1 FROM user_quests uq
                           WHERE uq.quest_id = v_original_quiz_id
                             AND uq.user_id = p_user_id
                       ))
            );

ELSE
            RETURN FALSE;
END CASE;
END;
$$ LANGUAGE plpgsql STABLE;

-- Step 9: Grant permissions
GRANT EXECUTE ON FUNCTION can_user_access_question(BIGINT, BIGINT) TO challenger_app;

COMMENT ON FUNCTION can_user_access_question IS 'Determines if a user can access a specific question based on visibility and relationships';



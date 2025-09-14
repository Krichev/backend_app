-- PostgreSQL Schema for Challenger Application
-- ==============================================

-- Drop tables if they exist (for development/testing)
DROP TABLE IF EXISTS challenge_progress_completed_days CASCADE;
DROP TABLE IF EXISTS quiz_rounds CASCADE;
DROP TABLE IF EXISTS quiz_sessions CASCADE;
DROP TABLE IF EXISTS quiz_questions CASCADE;
DROP TABLE IF EXISTS user_activity_logs CASCADE;
DROP TABLE IF EXISTS user_connections CASCADE;
DROP TABLE IF EXISTS reward_users CASCADE;
DROP TABLE IF EXISTS task_completions CASCADE;
DROP TABLE IF EXISTS quest_groups CASCADE;
DROP TABLE IF EXISTS group_users CASCADE;
DROP TABLE IF EXISTS user_quests CASCADE;
DROP TABLE IF EXISTS rewards CASCADE;
DROP TABLE IF EXISTS tasks CASCADE;
DROP TABLE IF EXISTS photo_verification_details CASCADE;
DROP TABLE IF EXISTS location_coordinates CASCADE;
DROP TABLE IF EXISTS verification_details CASCADE;
DROP TABLE IF EXISTS challenge_progress CASCADE;
DROP TABLE IF EXISTS challenge_participants CASCADE;
DROP TABLE IF EXISTS challenge_quests CASCADE;
DROP TABLE IF EXISTS challenges CASCADE;
DROP TABLE IF EXISTS stakes CASCADE;
DROP TABLE IF EXISTS groups CASCADE;
DROP TABLE IF EXISTS quests CASCADE;
DROP TABLE IF EXISTS users CASCADE;
DROP TABLE IF EXISTS photos CASCADE;
DROP TABLE IF EXISTS media_files CASCADE;

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
CREATE TYPE completion_status AS ENUM ('SUBMITTED', 'VERIFIED', 'REJECTED');
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
    'IMAGE',
    'VIDEO',
    'AUDIO',
    'DOCUMENT',
    'PROFILE_PICTURE',
    'THUMBNAIL',
    'ATTACHMENT'
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
    frame_rate        DECIMAL(10, 2) CHECK (frame_rate > 0),
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
    value         DECIMAL(10, 2),
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
    user_id      BIGINT         NOT NULL,
    challenge_id BIGINT         NOT NULL,
    amount       DECIMAL(10, 2) NOT NULL,
    currency     VARCHAR(10)              DEFAULT 'USD',
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
    verified_by     BIGINT,
    completed_at    TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    verified_at     TIMESTAMP WITH TIME ZONE,
    FOREIGN KEY (task_id) REFERENCES tasks (id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    FOREIGN KEY (verified_by) REFERENCES users (id) ON DELETE SET NULL
);

-- Verification Details table
CREATE TABLE verification_details (
                                      id BIGINT PRIMARY KEY AUTO_INCREMENT,
                                      activity_type VARCHAR(100),
                                      target_value DOUBLE,
                                      radius DOUBLE,
                                      challenge_id BIGINT NOT NULL,
                                      location_coordinates_id BIGINT,
                                      photo_details_id BIGINT,
                                      created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                                      updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    -- Foreign key constraints
                                      CONSTRAINT fk_verification_details_challenge
                                          FOREIGN KEY (challenge_id) REFERENCES challenges(id)
                                              ON DELETE CASCADE,

                                      CONSTRAINT fk_verification_details_location
                                          FOREIGN KEY (location_coordinates_id) REFERENCES location_coordinates(id)
                                              ON DELETE SET NULL,

                                      CONSTRAINT fk_verification_details_photo
                                          FOREIGN KEY (photo_details_id) REFERENCES photo_verification_details(id)
                                              ON DELETE SET NULL
);

CREATE TABLE IF NOT EXISTS location_coordinates (
                                                    id BIGINT PRIMARY KEY AUTO_INCREMENT,
                                                    latitude DOUBLE,
                                                    longitude DOUBLE,
                                                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                                                    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-CREATE TABLE IF NOT EXISTS location_coordinates (
                                                     id BIGINT PRIMARY KEY AUTO_INCREMENT,
                                                     latitude DOUBLE NOT NULL,
                                                     longitude DOUBLE NOT NULL,
                                                     created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                                                     updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
 );

-- Create photo_verification_details table first (referenced by verification_details)
CREATE TABLE IF NOT EXISTS photo_verification_details (
                                                          id BIGINT PRIMARY KEY AUTO_INCREMENT,
                                                          description TEXT,
                                                          requires_photo_comparison BOOLEAN DEFAULT FALSE,
                                                          verification_mode VARCHAR(50) DEFAULT 'standard',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
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
    question        VARCHAR(1000) NOT NULL,
    answer          VARCHAR(500)  NOT NULL,
    difficulty      quiz_difficulty          DEFAULT 'EASY',
    topic           VARCHAR(200),
    source          VARCHAR(500),
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
    quiz_session_id BIGINT  NOT NULL,
    question_id     BIGINT  NOT NULL,
    user_answer     VARCHAR(500),
    is_correct      BOOLEAN,
    answered_at     TIMESTAMP WITH TIME ZONE,
    time_taken      INTEGER,
    round_number    INTEGER NOT NULL,
    created_at      TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
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
1,8s


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

CREATE INDEX idx_verification_details_challenge_id ON verification_details(challenge_id);
CREATE INDEX idx_verification_details_location_id ON verification_details(location_coordinates_id);
CREATE INDEX idx_verification_details_photo_id ON verification_details(photo_details_id);
CREATE INDEX idx_verification_details_activity_type ON verification_details(activity_type);

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
OR REPLACE FUNCTION is_image(media_cat media_category)
RETURNS BOOLEAN AS $$
BEGIN
RETURN media_cat = 'IMAGE';
END;
$$
LANGUAGE plpgsql IMMUTABLE;

-- Function to check if media is video (matches isVideo() method)
CREATE
OR REPLACE FUNCTION is_video(media_cat media_category)
RETURNS BOOLEAN AS $$
BEGIN
RETURN media_cat = 'VIDEO';
END;
$$
LANGUAGE plpgsql IMMUTABLE;

-- Function to check if media is audio (matches isAudio() method)
CREATE
OR REPLACE FUNCTION is_audio(media_cat media_category)
RETURNS BOOLEAN AS $$
BEGIN
RETURN media_cat = 'AUDIO';
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
    media_cat media_category DEFAULT NULL,
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
    media_cat media_category DEFAULT NULL,
    limit_count INTEGER DEFAULT 50
)
RETURNS TABLE (
    id BIGINT,
    original_filename VARCHAR(255),
    filename VARCHAR(255),
    s3_url TEXT,
    media_category media_category,
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

-- Trigger to automatically extract file extension
CREATE TRIGGER extract_media_files_extension
    BEFORE INSERT OR
UPDATE ON media_files
    FOR EACH ROW
    EXECUTE FUNCTION extract_file_extension();


-- Trigger to automatically generate stored filename
CREATE TRIGGER generate_media_files_stored_filename
    BEFORE INSERT
    ON media_files
    FOR EACH ROW
    EXECUTE FUNCTION generate_stored_filename();



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
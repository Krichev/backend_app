-- ============================================================================
-- Flyway Migration V2: Create Core Tables
-- ============================================================================
-- Description: Creates core tables with no inter-table dependencies.
--              These tables form the foundation of the Challenger schema.
--              Foreign key constraints are added in V5.
--
-- Version: V2
-- Author: Challenger System
-- Date: 2026-01-01
--
-- Tables Included:
--   - users: User accounts and profiles
--   - groups: Group/community definitions
--   - topics: Quiz question topics and categories
--   - media_files: Media file metadata and storage information
--
-- Note: Foreign key constraints are intentionally omitted and will be
--       added in V5__Create_Constraints.sql to prevent circular dependencies.
-- ============================================================================

-- Create extension for UUID generation if not exists
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE SCHEMA IF NOT EXISTS challenger;

-- ============================================================================
-- USERS TABLE
-- ============================================================================

DROP TABLE IF EXISTS users CASCADE;

CREATE TABLE users
(
    id                  BIGSERIAL PRIMARY KEY,
    username            VARCHAR(255) NOT NULL UNIQUE,
    email               VARCHAR(255) NOT NULL UNIQUE,
    password            VARCHAR(255) NOT NULL,
    profile_picture_url TEXT,
    bio                 TEXT,
    role                user_role DEFAULT 'MEMBER',
    points              BIGINT    DEFAULT 0,
    total_points_spent  BIGINT    DEFAULT 0,
    total_points_earned BIGINT    DEFAULT 0,
    created_at          TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Table comments
COMMENT ON TABLE users IS 'Stores user account information, profiles, and points balance';
COMMENT ON COLUMN users.id IS 'Auto-incrementing primary key for users';
COMMENT ON COLUMN users.username IS 'Unique username for login and display';
COMMENT ON COLUMN users.email IS 'Unique email address for login and notifications';
COMMENT ON COLUMN users.password IS 'Hashed password for authentication';
COMMENT ON COLUMN users.profile_picture_url IS 'URL to user profile picture';
COMMENT ON COLUMN users.bio IS 'User biography or description';
COMMENT ON COLUMN users.role IS 'User role: ADMIN, MEMBER, or MODERATOR';
COMMENT ON COLUMN users.points IS 'Current points balance available for spending';
COMMENT ON COLUMN users.total_points_spent IS 'Cumulative points spent by user';
COMMENT ON COLUMN users.total_points_earned IS 'Cumulative points earned by user';

-- ============================================================================
-- GROUPS TABLE
-- ============================================================================

DROP TABLE IF EXISTS groups CASCADE;

CREATE TABLE groups
(
    id              BIGSERIAL PRIMARY KEY,
    name            VARCHAR(255) NOT NULL,
    description     TEXT,
    type            group_type,
    privacy_setting privacy_setting          DEFAULT 'PUBLIC',
    creator_id      BIGINT       NOT NULL,
    created_at      TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Table comments
COMMENT ON TABLE groups IS 'Community groups for organizing users and challenges';
COMMENT ON COLUMN groups.id IS 'Auto-incrementing primary key for groups';
COMMENT ON COLUMN groups.name IS 'Display name of the group';
COMMENT ON COLUMN groups.description IS 'Detailed description of group purpose';
COMMENT ON COLUMN groups.type IS 'Group category: CHALLENGE, SOCIAL, LEARNING, FITNESS, HOBBY';
COMMENT ON COLUMN groups.privacy_setting IS 'Access level: PUBLIC, PRIVATE, INVITATION_ONLY';
COMMENT ON COLUMN groups.creator_id IS 'User who created the group (FK to users)';

-- ============================================================================
-- TOPICS TABLE
-- ============================================================================

DROP TABLE IF EXISTS topics CASCADE;

CREATE TABLE topics
(
    id             BIGSERIAL PRIMARY KEY,
    name           VARCHAR(100) NOT NULL UNIQUE,
    category       VARCHAR(50),
    description    TEXT,
    is_active      BOOLEAN   NOT NULL DEFAULT TRUE,
    question_count INTEGER            DEFAULT 0,
    creator_id     BIGINT,
    created_at     TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     TIMESTAMP          DEFAULT CURRENT_TIMESTAMP
);

-- Table comments
COMMENT ON TABLE topics IS 'Quiz question topics and categories for content organization';
COMMENT ON COLUMN topics.id IS 'Auto-incrementing primary key for topics';
COMMENT ON COLUMN topics.name IS 'Unique topic name';
COMMENT ON COLUMN topics.category IS 'Higher-level category grouping for topics';
COMMENT ON COLUMN topics.description IS 'Detailed description of topic scope';
COMMENT ON COLUMN topics.is_active IS 'Whether topic is currently active for use';
COMMENT ON COLUMN topics.question_count IS 'Number of questions tagged with this topic';
COMMENT ON COLUMN topics.creator_id IS 'User who created the topic (FK to users)';

-- ============================================================================
-- MEDIA_FILES TABLE
-- ============================================================================

DROP TABLE IF EXISTS media_files CASCADE;

CREATE TABLE media_files
(
    -- Primary key
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
    entity_type       VARCHAR(50),
    uploaded_by       BIGINT            NOT NULL,

    -- Timestamp fields
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

-- Table comments
COMMENT ON TABLE media_files IS 'Stores metadata and information about uploaded media files matching Java MediaFile entity';
COMMENT ON COLUMN media_files.id IS 'Auto-incrementing primary key (matches @GeneratedValue IDENTITY)';
COMMENT ON COLUMN media_files.original_filename IS 'Original filename as uploaded by user';
COMMENT ON COLUMN media_files.filename IS 'Unique filename used for storage';
COMMENT ON COLUMN media_files.file_path IS 'Full path to the file on storage system';
COMMENT ON COLUMN media_files.processed_path IS 'Path to processed/optimized version';
COMMENT ON COLUMN media_files.thumbnail_path IS 'Path to thumbnail version';
COMMENT ON COLUMN media_files.content_type IS 'MIME type / content type of the file';
COMMENT ON COLUMN media_files.file_size IS 'File size in bytes';
COMMENT ON COLUMN media_files.media_type IS 'Type of media (IMAGE, VIDEO, AUDIO, etc.)';
COMMENT ON COLUMN media_files.media_category IS 'Category of media usage (PROFILE_PICTURE, THUMBNAIL, etc.)';
COMMENT ON COLUMN media_files.processing_status IS 'Current processing status (PENDING, COMPLETED, FAILED)';
COMMENT ON COLUMN media_files.entity_id IS 'ID of related entity this media belongs to';
COMMENT ON COLUMN media_files.entity_type IS 'Type of the related entity (e.g., quiz_question)';
COMMENT ON COLUMN media_files.uploaded_by IS 'ID of user who uploaded the file';
COMMENT ON COLUMN media_files.uploaded_at IS 'Timestamp when file was uploaded (matches @CreationTimestamp)';
COMMENT ON COLUMN media_files.created_at IS 'Record creation timestamp (matches @CreationTimestamp)';
COMMENT ON COLUMN media_files.updated_at IS 'Last update timestamp (matches @UpdateTimestamp)';
COMMENT ON COLUMN media_files.duration_seconds IS 'Duration in seconds for video/audio files';
COMMENT ON COLUMN media_files.frame_rate IS 'Frame rate for video files (decimal with 2 places)';
COMMENT ON COLUMN media_files.s3_key IS 'S3 object key for cloud storage';
COMMENT ON COLUMN media_files.s3_url IS 'Full S3 URL for direct access';

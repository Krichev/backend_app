-- ============================================================================
-- Flyway Migration V1: Create Enum Types
-- ============================================================================
-- Description: Creates all enum types used throughout the Challenger schema.
--              These types must be created before any tables that reference them.
--
-- Version: V1
-- Author: Challenger System
-- Date: 2026-01-01
--
-- Purpose: PostgreSQL ENUM types provide type safety and ensure only valid
--          values can be inserted into enum-typed columns.
-- ============================================================================

-- Drop ENUM types if they exist (with CASCADE to handle dependencies)
DROP TYPE IF EXISTS activity_type CASCADE;
DROP TYPE IF EXISTS challenge_difficulty_type CASCADE;
DROP TYPE IF EXISTS challenge_frequency CASCADE;
DROP TYPE IF EXISTS challenge_status CASCADE;
DROP TYPE IF EXISTS challenge_type CASCADE;
DROP TYPE IF EXISTS completion_status CASCADE;
DROP TYPE IF EXISTS connection_status CASCADE;
DROP TYPE IF EXISTS currency_type CASCADE;
DROP TYPE IF EXISTS frequency_type CASCADE;
DROP TYPE IF EXISTS group_type CASCADE;
DROP TYPE IF EXISTS media_category CASCADE;
DROP TYPE IF EXISTS media_type CASCADE;
DROP TYPE IF EXISTS participant_status CASCADE;
DROP TYPE IF EXISTS payment_type CASCADE;
DROP TYPE IF EXISTS photo_type_enum CASCADE;
DROP TYPE IF EXISTS privacy_setting CASCADE;
DROP TYPE IF EXISTS processing_status CASCADE;
DROP TYPE IF EXISTS processing_status_enum CASCADE;
DROP TYPE IF EXISTS quest_status CASCADE;
DROP TYPE IF EXISTS quest_type CASCADE;
DROP TYPE IF EXISTS quest_visibility CASCADE;
DROP TYPE IF EXISTS question_source CASCADE;
DROP TYPE IF EXISTS question_visibility CASCADE;
DROP TYPE IF EXISTS quiz_difficulty CASCADE;
DROP TYPE IF EXISTS quiz_session_status CASCADE;
DROP TYPE IF EXISTS reward_source CASCADE;
DROP TYPE IF EXISTS reward_type CASCADE;
DROP TYPE IF EXISTS task_status CASCADE;
DROP TYPE IF EXISTS task_type CASCADE;
DROP TYPE IF EXISTS user_role CASCADE;
DROP TYPE IF EXISTS verification_method CASCADE;

-- Create ENUM types (alphabetically ordered for consistency)

-- Activity tracking types
CREATE TYPE activity_type AS ENUM (
    'TASK_COMPLETION',
    'JOIN_GROUP',
    'COMPLETE_QUEST',
    'REWARD_EARNED',
    'CONNECTION_MADE',
    'CHALLENGE_JOINED',
    'CHALLENGE_COMPLETED'
);
COMMENT ON TYPE activity_type IS 'Types of user activities tracked in activity logs';

-- Challenge difficulty levels
CREATE TYPE challenge_difficulty_type AS ENUM (
    'BEGINNER',
    'EASY',
    'MEDIUM',
    'HARD',
    'EXPERT',
    'EXTREME'
);
COMMENT ON TYPE challenge_difficulty_type IS 'Difficulty levels for challenges';

-- Challenge frequency options
CREATE TYPE challenge_frequency AS ENUM (
    'DAILY',
    'WEEKLY',
    'ONE_TIME'
);
COMMENT ON TYPE challenge_frequency IS 'How often a challenge occurs';

-- Challenge lifecycle states
CREATE TYPE challenge_status AS ENUM (
    'PENDING',
    'ACTIVE',
    'COMPLETED',
    'CANCELLED',
    'OPEN'
);
COMMENT ON TYPE challenge_status IS 'Current state of a challenge';

-- Challenge categories
CREATE TYPE challenge_type AS ENUM (
    'ACCOUNTABILITY',
    'QUEST',
    'EVENT',
    'QUIZ'
);
COMMENT ON TYPE challenge_type IS 'Type of challenge';

-- Completion/submission status
CREATE TYPE completion_status AS ENUM (
    'SUBMITTED',
    'VERIFIED',
    'REJECTED',
    'PENDING'
);
COMMENT ON TYPE completion_status IS 'Status of task completion submissions';

-- User connection states
CREATE TYPE connection_status AS ENUM (
    'PENDING',
    'ACCEPTED',
    'REJECTED',
    'BLOCKED'
);
COMMENT ON TYPE connection_status IS 'Status of connections between users';

-- Currency types
CREATE TYPE currency_type AS ENUM (
    'USD',
    'EUR',
    'GBP',
    'CAD',
    'AUD',
    'POINTS'
);
COMMENT ON TYPE currency_type IS 'Supported currency types for rewards';

-- Frequency options
CREATE TYPE frequency_type AS ENUM (
    'DAILY',
    'WEEKLY',
    'MONTHLY',
    'ONE_TIME',
    'CUSTOM'
);
COMMENT ON TYPE frequency_type IS 'Frequency options for recurring tasks';

-- Group categories
CREATE TYPE group_type AS ENUM (
    'CHALLENGE',
    'SOCIAL',
    'LEARNING',
    'FITNESS',
    'HOBBY'
);
COMMENT ON TYPE group_type IS 'Categories for user groups';

-- Media file categories
CREATE TYPE media_category AS ENUM (
    'QUIZ_QUESTION',
    'AVATAR',
    'CHALLENGE_PROOF',
    'SYSTEM'
);
COMMENT ON TYPE media_category IS 'Category of media file usage';

-- Media file types
CREATE TYPE media_type AS ENUM (
    'IMAGE',
    'VIDEO',
    'AUDIO',
    'DOCUMENT',
    'ARCHIVE'
);
COMMENT ON TYPE media_type IS 'Type of media file';

-- Participant progress status
CREATE TYPE participant_status AS ENUM (
    'IN_PROGRESS',
    'COMPLETED',
    'FAILED'
);
COMMENT ON TYPE participant_status IS 'Status of challenge participant';

-- Payment/pricing models
CREATE TYPE payment_type AS ENUM (
    'FREE',
    'ENTRY_FEE',
    'PRIZE_POOL',
    'SUBSCRIPTION'
);
COMMENT ON TYPE payment_type IS 'Payment model for challenges';

-- Photo types (legacy - consider consolidating with media_category)
CREATE TYPE photo_type_enum AS ENUM (
    'AVATAR',
    'QUIZ_QUESTION',
    'CHALLENGE_COVER',
    'TASK_VERIFICATION',
    'GENERAL',
    'THUMBNAIL',
    'BACKGROUND',
    'BANNER',
    'GALLERY',
    'DOCUMENT'
);
COMMENT ON TYPE photo_type_enum IS 'Types of photos (legacy enum)';

-- Privacy levels
CREATE TYPE privacy_setting AS ENUM (
    'PUBLIC',
    'PRIVATE',
    'INVITATION_ONLY'
);
COMMENT ON TYPE privacy_setting IS 'Privacy settings for groups and content';

-- Processing status for media files
CREATE TYPE processing_status AS ENUM (
    'PENDING',
    'PROCESSING',
    'COMPLETED',
    'FAILED',
    'CANCELLED'
);
COMMENT ON TYPE processing_status IS 'Status of media file processing';

-- Processing status enum (legacy - consider consolidating with processing_status)
CREATE TYPE processing_status_enum AS ENUM (
    'PENDING',
    'PROCESSING',
    'COMPLETED',
    'FAILED'
);
COMMENT ON TYPE processing_status_enum IS 'Processing status enum (legacy)';

-- Quest lifecycle states
CREATE TYPE quest_status AS ENUM (
    'OPEN',
    'IN_PROGRESS',
    'COMPLETED',
    'CANCELLED'
);
COMMENT ON TYPE quest_status IS 'Current state of a quest';

-- Quest categories
CREATE TYPE quest_type AS ENUM (
    'CHALLENGE',
    'ACTIVITY_PARTNER',
    'LEARNING',
    'CONTEST'
);
COMMENT ON TYPE quest_type IS 'Type of quest';

-- Quest visibility levels
CREATE TYPE quest_visibility AS ENUM (
    'PUBLIC',
    'PRIVATE',
    'GROUP_ONLY'
);
COMMENT ON TYPE quest_visibility IS 'Who can see and join a quest';

-- Question source tracking
CREATE TYPE question_source AS ENUM (
    'app',
    'user'
);
COMMENT ON TYPE question_source IS 'Source of quiz questions (system or user-generated)';

-- Question visibility levels
CREATE TYPE question_visibility AS ENUM (
    'PRIVATE',           -- Only visible to creator
    'FRIENDS_FAMILY',    -- Visible to creator and their friends/family
    'QUIZ_ONLY',         -- Only in the specific quiz/challenge where it was added
    'PUBLIC'             -- Available to everyone in question search
);
COMMENT ON TYPE question_visibility IS 'Who can see and use quiz questions';

-- Quiz difficulty levels
CREATE TYPE quiz_difficulty AS ENUM (
    'EASY',
    'MEDIUM',
    'HARD'
);
COMMENT ON TYPE quiz_difficulty IS 'Difficulty level of quiz questions';

-- Quiz session states
CREATE TYPE quiz_session_status AS ENUM (
    'CREATED',
    'IN_PROGRESS',
    'COMPLETED',
    'ABANDONED',
    'CANCELLED',
    'ARCHIVED'
);
COMMENT ON TYPE quiz_session_status IS 'Status of quiz session';

-- Reward sources
CREATE TYPE reward_source AS ENUM (
    'INDIVIDUAL',
    'USER',
    'SPONSOR',
    'GROUP',
    'SYSTEM'
);
COMMENT ON TYPE reward_source IS 'Source of rewards';

-- Reward types
CREATE TYPE reward_type AS ENUM (
    'MONETARY',
    'POINTS',
    'BADGE',
    'CUSTOM'
);
COMMENT ON TYPE reward_type IS 'Type of reward';

-- Task completion states
CREATE TYPE task_status AS ENUM (
    'NOT_STARTED',
    'IN_PROGRESS',
    'COMPLETED',
    'VERIFIED',
    'FAILED'
);
COMMENT ON TYPE task_status IS 'Status of task completion';

-- Task frequency/recurrence
CREATE TYPE task_type AS ENUM (
    'DAILY',
    'ONE_TIME',
    'RECURRING',
    'WEEKLY',
    'MONTHLY'
);
COMMENT ON TYPE task_type IS 'Type and frequency of task';

-- User roles
CREATE TYPE user_role AS ENUM (
    'ADMIN',
    'MEMBER',
    'MODERATOR'
);
COMMENT ON TYPE user_role IS 'User role for permissions';

-- Verification methods
CREATE TYPE verification_method AS ENUM (
    'MANUAL',
    'FITNESS_API',
    'PHOTO',
    'QUIZ',
    'LOCATION',
    'ACTIVITY'
);
COMMENT ON TYPE verification_method IS 'How task completion is verified';

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

-- Users table
CREATE TABLE users (
                       id BIGSERIAL PRIMARY KEY,
                       username VARCHAR(255) NOT NULL UNIQUE,
                       email VARCHAR(255) NOT NULL UNIQUE,
                       password VARCHAR(255) NOT NULL,
                       profile_picture_url TEXT,
                       bio TEXT,
                       role user_role DEFAULT 'MEMBER',
                       created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
                       updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Groups table
CREATE TABLE groups (
                        id BIGSERIAL PRIMARY KEY,
                        name VARCHAR(255) NOT NULL,
                        description TEXT,
                        type group_type,
                        privacy_setting privacy_setting DEFAULT 'PUBLIC',
                        created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
                        updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
                        creator_id BIGINT NOT NULL,
                        FOREIGN KEY (creator_id) REFERENCES users (id) ON DELETE CASCADE
);

-- Quests table
CREATE TABLE quests (
                        id BIGSERIAL PRIMARY KEY,
                        title VARCHAR(255) NOT NULL,
                        description TEXT,
                        type quest_type,
                        visibility quest_visibility DEFAULT 'PUBLIC',
                        status quest_status DEFAULT 'OPEN',
                        created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
                        updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
                        creator_id BIGINT NOT NULL,
                        challenge_id BIGINT,
                        FOREIGN KEY (creator_id) REFERENCES users (id) ON DELETE CASCADE
);

-- Tasks table
CREATE TABLE tasks (
                       id BIGSERIAL PRIMARY KEY,
                       title VARCHAR(255) NOT NULL,
                       description TEXT,
                       type task_type DEFAULT 'ONE_TIME',
                       status task_status DEFAULT 'NOT_STARTED',
                       verification_method verification_method DEFAULT 'MANUAL',
                       due_date TIMESTAMP WITH TIME ZONE,
                       created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
                       updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
                       quest_id BIGINT,
                       assigned_to BIGINT,
                       challenge_id BIGINT,
                       FOREIGN KEY (quest_id) REFERENCES quests (id) ON DELETE CASCADE,
                       FOREIGN KEY (assigned_to) REFERENCES users (id) ON DELETE SET NULL
);

-- Rewards table
CREATE TABLE rewards (
                         id BIGSERIAL PRIMARY KEY,
                         title VARCHAR(255) NOT NULL,
                         description TEXT,
                         type reward_type DEFAULT 'POINTS',
                         value DECIMAL(10,2),
                         reward_source reward_source DEFAULT 'SYSTEM',
                         created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
                         quest_id BIGINT,
                         FOREIGN KEY (quest_id) REFERENCES quests (id) ON DELETE CASCADE
);


-- Challenges table
CREATE TABLE challenges (
                            id BIGSERIAL PRIMARY KEY,
                            title VARCHAR(255) NOT NULL,
                            description TEXT,
                            type challenge_type NOT NULL,
                            creator_id BIGINT NOT NULL,
                            group_id BIGINT,
                            is_public BOOLEAN DEFAULT TRUE,
                            start_date TIMESTAMP WITH TIME ZONE,
                            end_date TIMESTAMP WITH TIME ZONE,
                            frequency challenge_frequency DEFAULT 'ONE_TIME',
                            verification_method verification_method DEFAULT 'MANUAL',
                            status challenge_status DEFAULT 'PENDING',
                            created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
                            updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
                            FOREIGN KEY (creator_id) REFERENCES users (id) ON DELETE CASCADE,
                            FOREIGN KEY (group_id) REFERENCES groups (id) ON DELETE SET NULL
);

-- Stakes table
CREATE TABLE stakes (
                        id BIGSERIAL PRIMARY KEY,
                        user_id BIGINT NOT NULL,
                        challenge_id BIGINT NOT NULL,
                        amount DECIMAL(10,2) NOT NULL,
                        currency VARCHAR(10) DEFAULT 'USD',
                        is_refunded BOOLEAN DEFAULT FALSE,
                        created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
                        FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
                        FOREIGN KEY (challenge_id) REFERENCES challenges (id) ON DELETE CASCADE
);

-- Add foreign key constraint to quests table
ALTER TABLE quests ADD CONSTRAINT fk_quest_challenge
    FOREIGN KEY (challenge_id) REFERENCES challenges (id) ON DELETE CASCADE;

-- Add foreign key constraint to tasks table
ALTER TABLE tasks ADD CONSTRAINT fk_task_challenge
    FOREIGN KEY (challenge_id) REFERENCES challenges (id) ON DELETE CASCADE;

-- Challenge Participants table
CREATE TABLE challenge_participants (
                                        challenge_id BIGINT,
                                        user_id BIGINT,
                                        joined_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
                                        PRIMARY KEY (challenge_id, user_id),
                                        FOREIGN KEY (challenge_id) REFERENCES challenges (id) ON DELETE CASCADE,
                                        FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);

-- Challenge Progress table
CREATE TABLE challenge_progress (
                                    id BIGSERIAL PRIMARY KEY,
                                    challenge_id BIGINT NOT NULL,
                                    user_id BIGINT NOT NULL,
                                    status participant_status NOT NULL,
                                    completion_percentage DOUBLE PRECISION DEFAULT 0,
                                    verification_data TEXT,
                                    verification_status completion_status DEFAULT 'SUBMITTED',
                                    verified_by BIGINT,
                                    verification_date TIMESTAMP WITH TIME ZONE,
                                    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
                                    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
                                    FOREIGN KEY (challenge_id) REFERENCES challenges (id) ON DELETE CASCADE,
                                    FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
                                    FOREIGN KEY (verified_by) REFERENCES users (id) ON DELETE SET NULL
);

-- Challenge Progress Completed Days table
CREATE TABLE challenge_progress_completed_days (
                                                   id BIGSERIAL PRIMARY KEY,
                                                   challenge_progress_id BIGINT NOT NULL,
                                                   completed_day DATE NOT NULL,
                                                   created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
                                                   UNIQUE (challenge_progress_id, completed_day),
                                                   FOREIGN KEY (challenge_progress_id) REFERENCES challenge_progress (id) ON DELETE CASCADE
);

-- Challenge Quests mapping table
CREATE TABLE challenge_quests (
                                  challenge_id BIGINT,
                                  quest_id BIGINT,
                                  PRIMARY KEY (challenge_id, quest_id),
                                  FOREIGN KEY (challenge_id) REFERENCES challenges (id) ON DELETE CASCADE,
                                  FOREIGN KEY (quest_id) REFERENCES quests (id) ON DELETE CASCADE
);

-- Junction table for many-to-many relationship between users and quests
CREATE TABLE user_quests (
                             user_id BIGINT,
                             quest_id BIGINT,
                             joined_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
                             PRIMARY KEY (user_id, quest_id),
                             FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
                             FOREIGN KEY (quest_id) REFERENCES quests (id) ON DELETE CASCADE
);

-- Junction table for many-to-many relationship between groups and users
CREATE TABLE group_users (
                             group_id BIGINT,
                             user_id BIGINT,
                             joined_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
                             PRIMARY KEY (group_id, user_id),
                             FOREIGN KEY (group_id) REFERENCES groups (id) ON DELETE CASCADE,
                             FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);

-- Junction table for many-to-many relationship between groups and quests
CREATE TABLE quest_groups (
                              quest_id BIGINT,
                              group_id BIGINT,
                              PRIMARY KEY (quest_id, group_id),
                              FOREIGN KEY (quest_id) REFERENCES quests (id) ON DELETE CASCADE,
                              FOREIGN KEY (group_id) REFERENCES groups (id) ON DELETE CASCADE
);

-- Task Completions table
CREATE TABLE task_completions (
                                  id BIGSERIAL PRIMARY KEY,
                                  task_id BIGINT NOT NULL,
                                  user_id BIGINT NOT NULL,
                                  status completion_status DEFAULT 'SUBMITTED',
                                  completion_data TEXT,
                                  verified_by BIGINT,
                                  completed_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
                                  verified_at TIMESTAMP WITH TIME ZONE,
                                  FOREIGN KEY (task_id) REFERENCES tasks (id) ON DELETE CASCADE,
                                  FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
                                  FOREIGN KEY (verified_by) REFERENCES users (id) ON DELETE SET NULL
);

-- Verification Details table
CREATE TABLE verification_details (
                                      id BIGSERIAL PRIMARY KEY,
                                      task_completion_id BIGINT,
                                      verification_type VARCHAR(50),
                                      verification_data JSONB,
                                      created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
                                      FOREIGN KEY (task_completion_id) REFERENCES task_completions (id) ON DELETE CASCADE
);

-- Location Coordinates table
CREATE TABLE location_coordinates (
                                      id BIGSERIAL PRIMARY KEY,
                                      verification_detail_id BIGINT,
                                      latitude DOUBLE PRECISION,
                                      longitude DOUBLE PRECISION,
                                      address TEXT,
                                      created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
                                      FOREIGN KEY (verification_detail_id) REFERENCES verification_details (id) ON DELETE CASCADE
);

-- Photo Verification Details table
CREATE TABLE photo_verification_details (
                                            id BIGSERIAL PRIMARY KEY,
                                            verification_detail_id BIGINT,
                                            photo_url TEXT NOT NULL,
                                            description TEXT,
                                            uploaded_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
                                            FOREIGN KEY (verification_detail_id) REFERENCES verification_details (id) ON DELETE CASCADE
);

-- Junction table for many-to-many relationship between rewards and users
CREATE TABLE reward_users (
                              reward_id BIGINT,
                              user_id BIGINT,
                              awarded_date TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
                              PRIMARY KEY (reward_id, user_id),
                              FOREIGN KEY (reward_id) REFERENCES rewards (id) ON DELETE CASCADE,
                              FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);

-- User Connections table
CREATE TABLE user_connections (
                                  id BIGSERIAL PRIMARY KEY,
                                  user_id BIGINT NOT NULL,
                                  connected_user_id BIGINT NOT NULL,
                                  status connection_status DEFAULT 'PENDING',
                                  created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
                                  updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
                                  FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
                                  FOREIGN KEY (connected_user_id) REFERENCES users (id) ON DELETE CASCADE,
                                  UNIQUE (user_id, connected_user_id)
);

-- User Activity Logs table
CREATE TABLE user_activity_logs (
                                    id BIGSERIAL PRIMARY KEY,
                                    user_id BIGINT NOT NULL,
                                    activity_type VARCHAR(100) NOT NULL,
                                    description TEXT,
                                    metadata JSONB,
                                    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
                                    FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);

-- Quiz Questions table
CREATE TABLE quiz_questions (
                                id BIGSERIAL PRIMARY KEY,
                                question VARCHAR(1000) NOT NULL,
                                answer VARCHAR(500) NOT NULL,
                                difficulty quiz_difficulty DEFAULT 'EASY',
                                topic VARCHAR(200),
                                source VARCHAR(500),
                                additional_info TEXT,
                                is_user_created BOOLEAN DEFAULT FALSE,
                                creator_id BIGINT,
                                external_id VARCHAR(255),
                                usage_count INTEGER DEFAULT 0,
                                created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
                                updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
                                last_used TIMESTAMP WITH TIME ZONE,
                                FOREIGN KEY (creator_id) REFERENCES users (id) ON DELETE SET NULL
);

-- Quiz Sessions table
CREATE TABLE quiz_sessions (
                               id BIGSERIAL PRIMARY KEY,
                               user_id BIGINT NOT NULL,
                               challenge_id BIGINT,
                               difficulty quiz_difficulty DEFAULT 'EASY',
                               question_source question_source DEFAULT 'app',
                               total_questions INTEGER DEFAULT 0,
                               correct_answers INTEGER DEFAULT 0,
                               status quiz_session_status DEFAULT 'CREATED',
                               started_at TIMESTAMP WITH TIME ZONE,
                               completed_at TIMESTAMP WITH TIME ZONE,
                               created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
                               updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
                               FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
                               FOREIGN KEY (challenge_id) REFERENCES challenges (id) ON DELETE SET NULL
);

-- Quiz Rounds table
CREATE TABLE quiz_rounds (
                             id BIGSERIAL PRIMARY KEY,
                             quiz_session_id BIGINT NOT NULL,
                             question_id BIGINT NOT NULL,
                             user_answer VARCHAR(500),
                             is_correct BOOLEAN,
                             answered_at TIMESTAMP WITH TIME ZONE,
                             time_taken INTEGER,
                             round_number INTEGER NOT NULL,
                             created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
                             FOREIGN KEY (quiz_session_id) REFERENCES quiz_sessions (id) ON DELETE CASCADE,
                             FOREIGN KEY (question_id) REFERENCES quiz_questions (id) ON DELETE CASCADE
);

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

-- ================================
-- TRIGGERS FOR UPDATED_AT
-- ================================

-- Function to update updated_at timestamp
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Apply triggers to tables with updated_at column
CREATE TRIGGER update_users_updated_at
    BEFORE UPDATE ON users
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_groups_updated_at
    BEFORE UPDATE ON groups
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_quests_updated_at
    BEFORE UPDATE ON quests
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_tasks_updated_at
    BEFORE UPDATE ON tasks
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_challenges_updated_at
    BEFORE UPDATE ON challenges
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_challenge_progress_updated_at
    BEFORE UPDATE ON challenge_progress
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_user_connections_updated_at
    BEFORE UPDATE ON user_connections
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_quiz_questions_updated_at
    BEFORE UPDATE ON quiz_questions
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_quiz_sessions_updated_at
    BEFORE UPDATE ON quiz_sessions
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
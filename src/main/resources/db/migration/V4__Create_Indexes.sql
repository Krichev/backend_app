-- ============================================================================
-- Flyway Migration V4: Create Indexes
-- ============================================================================
-- Description: Creates all indexes for performance optimization.
--              Indexes are organized by table for easy maintenance.
--
-- Version: V4
-- Author: Challenger System
-- Date: 2026-01-01
--
-- Index Categories:
--   - Primary lookup indexes (username, email, etc.)
--   - Foreign key indexes for join performance
--   - Status/type filtering indexes
--   - Timestamp indexes for sorting/filtering
--   - Composite indexes for common queries
-- ============================================================================

-- ============================================================================
-- USERS TABLE INDEXES
-- ============================================================================

-- Lookup indexes for user authentication and search
DROP INDEX IF EXISTS idx_users_email;
CREATE INDEX idx_users_email ON users (email);
COMMENT ON INDEX idx_users_email IS 'Fast lookup for user authentication by email';

DROP INDEX IF EXISTS idx_users_username;
CREATE INDEX idx_users_username ON users (username);
COMMENT ON INDEX idx_users_username IS 'Fast lookup for user search by username';

-- ============================================================================
-- GROUPS TABLE INDEXES
-- ============================================================================

-- Foreign key and filtering indexes
DROP INDEX IF EXISTS idx_groups_creator;
CREATE INDEX idx_groups_creator ON groups (creator_id);
COMMENT ON INDEX idx_groups_creator IS 'Find groups created by a specific user';

DROP INDEX IF EXISTS idx_groups_type;
CREATE INDEX idx_groups_type ON groups (type);
COMMENT ON INDEX idx_groups_type IS 'Filter groups by category (CHALLENGE, SOCIAL, etc.)';

-- ============================================================================
-- TOPICS TABLE INDEXES
-- ============================================================================

DROP INDEX IF EXISTS idx_topic_name;
CREATE INDEX idx_topic_name ON topics (name);
COMMENT ON INDEX idx_topic_name IS 'Fast topic lookup by name';

DROP INDEX IF EXISTS idx_topic_category;
CREATE INDEX idx_topic_category ON topics (category);
COMMENT ON INDEX idx_topic_category IS 'Filter topics by category';

DROP INDEX IF EXISTS idx_topics_is_active;
CREATE INDEX idx_topics_is_active ON topics (is_active);
COMMENT ON INDEX idx_topics_is_active IS 'Filter active/inactive topics';

-- ============================================================================
-- MEDIA_FILES TABLE INDEXES
-- ============================================================================

-- Single column indexes
DROP INDEX IF EXISTS idx_media_files_uploaded_by;
CREATE INDEX idx_media_files_uploaded_by ON media_files (uploaded_by);
COMMENT ON INDEX idx_media_files_uploaded_by IS 'Find all media uploaded by a user';

DROP INDEX IF EXISTS idx_media_files_entity_id;
CREATE INDEX idx_media_files_entity_id ON media_files (entity_id);
COMMENT ON INDEX idx_media_files_entity_id IS 'Find media for a specific entity';

DROP INDEX IF EXISTS idx_media_files_media_type;
CREATE INDEX idx_media_files_media_type ON media_files (media_type);
COMMENT ON INDEX idx_media_files_media_type IS 'Filter by media type (IMAGE, VIDEO, AUDIO, etc.)';

DROP INDEX IF EXISTS idx_media_files_media_category;
CREATE INDEX idx_media_files_media_category ON media_files (media_category);
COMMENT ON INDEX idx_media_files_media_category IS 'Filter by category (AVATAR, QUIZ_QUESTION, etc.)';

DROP INDEX IF EXISTS idx_media_files_processing_status;
CREATE INDEX idx_media_files_processing_status ON media_files (processing_status);
COMMENT ON INDEX idx_media_files_processing_status IS 'Find files by processing status';

DROP INDEX IF EXISTS idx_media_files_uploaded_at;
CREATE INDEX idx_media_files_uploaded_at ON media_files (uploaded_at);
COMMENT ON INDEX idx_media_files_uploaded_at IS 'Sort files by upload time';

DROP INDEX IF EXISTS idx_media_files_created_at;
CREATE INDEX idx_media_files_created_at ON media_files (created_at);
COMMENT ON INDEX idx_media_files_created_at IS 'Sort files by creation time';

DROP INDEX IF EXISTS idx_media_files_content_type;
CREATE INDEX idx_media_files_content_type ON media_files (content_type);
COMMENT ON INDEX idx_media_files_content_type IS 'Filter by MIME type';

DROP INDEX IF EXISTS idx_media_files_s3_key;
CREATE INDEX idx_media_files_s3_key ON media_files (s3_key);
COMMENT ON INDEX idx_media_files_s3_key IS 'Fast lookup by S3 key';

-- Composite indexes for common query patterns
DROP INDEX IF EXISTS idx_media_files_uploaded_by_category;
CREATE INDEX idx_media_files_uploaded_by_category ON media_files (uploaded_by, media_category);
COMMENT ON INDEX idx_media_files_uploaded_by_category IS 'Find user media by category';

DROP INDEX IF EXISTS idx_media_files_entity_category;
CREATE INDEX idx_media_files_entity_category ON media_files (entity_id, media_category) WHERE entity_id IS NOT NULL;
COMMENT ON INDEX idx_media_files_entity_category IS 'Find entity media by category (partial index)';

DROP INDEX IF EXISTS idx_media_files_processing_media_type;
CREATE INDEX idx_media_files_processing_media_type ON media_files (processing_status, media_type);
COMMENT ON INDEX idx_media_files_processing_media_type IS 'Find files by processing status and type';

DROP INDEX IF EXISTS idx_media_files_s3_key_status;
CREATE INDEX idx_media_files_s3_key_status ON media_files (s3_key, processing_status) WHERE s3_key IS NOT NULL;
COMMENT ON INDEX idx_media_files_s3_key_status IS 'S3 files by processing status (partial index)';

-- ============================================================================
-- QUESTS TABLE INDEXES
-- ============================================================================

DROP INDEX IF EXISTS idx_quests_creator;
CREATE INDEX idx_quests_creator ON quests (creator_id);
COMMENT ON INDEX idx_quests_creator IS 'Find quests created by a user';

DROP INDEX IF EXISTS idx_quests_status;
CREATE INDEX idx_quests_status ON quests (status);
COMMENT ON INDEX idx_quests_status IS 'Filter quests by status';

DROP INDEX IF EXISTS idx_quests_challenge;
CREATE INDEX idx_quests_challenge ON quests (challenge_id);
COMMENT ON INDEX idx_quests_challenge IS 'Find quests for a challenge';

-- ============================================================================
-- CHALLENGES TABLE INDEXES
-- ============================================================================

DROP INDEX IF EXISTS idx_challenges_creator;
CREATE INDEX idx_challenges_creator ON challenges (creator_id);
COMMENT ON INDEX idx_challenges_creator IS 'Find challenges created by a user';

DROP INDEX IF EXISTS idx_challenges_group;
CREATE INDEX idx_challenges_group ON challenges (group_id);
COMMENT ON INDEX idx_challenges_group IS 'Find challenges in a group';

DROP INDEX IF EXISTS idx_challenges_status;
CREATE INDEX idx_challenges_status ON challenges (status);
COMMENT ON INDEX idx_challenges_status IS 'Filter challenges by status';

DROP INDEX IF EXISTS idx_challenges_dates;
CREATE INDEX idx_challenges_dates ON challenges (start_date, end_date);
COMMENT ON INDEX idx_challenges_dates IS 'Query challenges by date range';

DROP INDEX IF EXISTS idx_challenges_difficulty;
CREATE INDEX idx_challenges_difficulty ON challenges (difficulty);
COMMENT ON INDEX idx_challenges_difficulty IS 'Filter challenges by difficulty level';

DROP INDEX IF EXISTS idx_challenges_payment_type;
CREATE INDEX idx_challenges_payment_type ON challenges (payment_type);
COMMENT ON INDEX idx_challenges_payment_type IS 'Filter challenges by payment model';

DROP INDEX IF EXISTS idx_challenges_is_public;
CREATE INDEX idx_challenges_is_public ON challenges (is_public);
COMMENT ON INDEX idx_challenges_is_public IS 'Separate public from private challenges';

DROP INDEX IF EXISTS idx_challenges_created_at;
CREATE INDEX idx_challenges_created_at ON challenges (created_at);
COMMENT ON INDEX idx_challenges_created_at IS 'Sort challenges by creation date';

-- ============================================================================
-- TASKS TABLE INDEXES
-- ============================================================================

DROP INDEX IF EXISTS idx_tasks_quest;
CREATE INDEX idx_tasks_quest ON tasks (quest_id);
COMMENT ON INDEX idx_tasks_quest IS 'Find tasks for a quest';

DROP INDEX IF EXISTS idx_tasks_assigned_to;
CREATE INDEX idx_tasks_assigned_to ON tasks (assigned_to);
COMMENT ON INDEX idx_tasks_assigned_to IS 'Find tasks assigned to a user';

DROP INDEX IF EXISTS idx_tasks_challenge;
CREATE INDEX idx_tasks_challenge ON tasks (challenge_id);
COMMENT ON INDEX idx_tasks_challenge IS 'Find tasks for a challenge';

DROP INDEX IF EXISTS idx_tasks_status;
CREATE INDEX idx_tasks_status ON tasks (status);
COMMENT ON INDEX idx_tasks_status IS 'Filter tasks by status';

DROP INDEX IF EXISTS idx_tasks_created_by;
CREATE INDEX idx_tasks_created_by ON tasks (created_by);
COMMENT ON INDEX idx_tasks_created_by IS 'Find tasks created by a user';

DROP INDEX IF EXISTS idx_tasks_frequency;
CREATE INDEX idx_tasks_frequency ON tasks (frequency);
COMMENT ON INDEX idx_tasks_frequency IS 'Filter tasks by recurrence frequency';

-- ============================================================================
-- REWARDS TABLE INDEXES
-- ============================================================================

DROP INDEX IF EXISTS idx_rewards_quest;
CREATE INDEX idx_rewards_quest ON rewards (quest_id);
COMMENT ON INDEX idx_rewards_quest IS 'Find rewards for a quest';

-- ============================================================================
-- CHALLENGE PROGRESS INDEXES
-- ============================================================================

DROP INDEX IF EXISTS idx_challenge_progress_user;
CREATE INDEX idx_challenge_progress_user ON challenge_progress (user_id);
COMMENT ON INDEX idx_challenge_progress_user IS 'Find all progress records for a user';

DROP INDEX IF EXISTS idx_challenge_progress_challenge;
CREATE INDEX idx_challenge_progress_challenge ON challenge_progress (challenge_id);
COMMENT ON INDEX idx_challenge_progress_challenge IS 'Find all progress for a challenge';

DROP INDEX IF EXISTS idx_challenge_progress_status;
CREATE INDEX idx_challenge_progress_status ON challenge_progress (status);
COMMENT ON INDEX idx_challenge_progress_status IS 'Filter progress by participant status';

DROP INDEX IF EXISTS idx_challenge_progress_completed_days;
CREATE INDEX idx_challenge_progress_completed_days ON challenge_progress_completed_days (challenge_progress_id);
COMMENT ON INDEX idx_challenge_progress_completed_days IS 'Find completed days for a progress record';

-- ============================================================================
-- TASK COMPLETIONS INDEXES
-- ============================================================================

DROP INDEX IF EXISTS idx_task_completions_task;
CREATE INDEX idx_task_completions_task ON task_completions (task_id);
COMMENT ON INDEX idx_task_completions_task IS 'Find completions for a task';

DROP INDEX IF EXISTS idx_task_completions_user;
CREATE INDEX idx_task_completions_user ON task_completions (user_id);
COMMENT ON INDEX idx_task_completions_user IS 'Find completions by a user';

DROP INDEX IF EXISTS idx_task_completions_status;
CREATE INDEX idx_task_completions_status ON task_completions (status);
COMMENT ON INDEX idx_task_completions_status IS 'Filter completions by verification status';

-- ============================================================================
-- VERIFICATION DETAILS INDEXES
-- ============================================================================

DROP INDEX IF EXISTS idx_verification_details_challenge_id;
CREATE INDEX idx_verification_details_challenge_id ON verification_details (challenge_id);
COMMENT ON INDEX idx_verification_details_challenge_id IS 'Find verification config for a challenge';

DROP INDEX IF EXISTS idx_verification_details_location_id;
CREATE INDEX idx_verification_details_location_id ON verification_details (location_coordinates_id);
COMMENT ON INDEX idx_verification_details_location_id IS 'Link to location coordinates';

DROP INDEX IF EXISTS idx_verification_details_photo_id;
CREATE INDEX idx_verification_details_photo_id ON verification_details (photo_details_id);
COMMENT ON INDEX idx_verification_details_photo_id IS 'Link to photo verification config';

DROP INDEX IF EXISTS idx_verification_details_activity_type;
CREATE INDEX idx_verification_details_activity_type ON verification_details (activity_type);
COMMENT ON INDEX idx_verification_details_activity_type IS 'Filter by activity type';

-- ============================================================================
-- USER CONNECTIONS INDEXES
-- ============================================================================

DROP INDEX IF EXISTS idx_user_connections_user;
CREATE INDEX idx_user_connections_user ON user_connections (user_id);
COMMENT ON INDEX idx_user_connections_user IS 'Find connections initiated by a user';

DROP INDEX IF EXISTS idx_user_connections_connected;
CREATE INDEX idx_user_connections_connected ON user_connections (connected_user_id);
COMMENT ON INDEX idx_user_connections_connected IS 'Find connections to a user';

DROP INDEX IF EXISTS idx_user_connections_status;
CREATE INDEX idx_user_connections_status ON user_connections (status);
COMMENT ON INDEX idx_user_connections_status IS 'Filter connections by status';

-- ============================================================================
-- USER RELATIONSHIPS INDEXES
-- ============================================================================

DROP INDEX IF EXISTS idx_user_relationships_user;
CREATE INDEX idx_user_relationships_user ON user_relationships (user_id, status);
COMMENT ON INDEX idx_user_relationships_user IS 'Find user relationships by status';

DROP INDEX IF EXISTS idx_user_relationships_related_user;
CREATE INDEX idx_user_relationships_related_user ON user_relationships (related_user_id, status);
COMMENT ON INDEX idx_user_relationships_related_user IS 'Find related user relationships by status';

-- ============================================================================
-- USER ACTIVITY LOGS INDEXES
-- ============================================================================

DROP INDEX IF EXISTS idx_user_activity_logs_user;
CREATE INDEX idx_user_activity_logs_user ON user_activity_logs (user_id);
COMMENT ON INDEX idx_user_activity_logs_user IS 'Find activities for a user';

DROP INDEX IF EXISTS idx_user_activity_logs_type;
CREATE INDEX idx_user_activity_logs_type ON user_activity_logs (activity_type);
COMMENT ON INDEX idx_user_activity_logs_type IS 'Filter activities by type';

DROP INDEX IF EXISTS idx_user_activity_logs_created;
CREATE INDEX idx_user_activity_logs_created ON user_activity_logs (created_at);
COMMENT ON INDEX idx_user_activity_logs_created IS 'Sort activities chronologically';

-- ============================================================================
-- QUIZ QUESTIONS INDEXES
-- ============================================================================

DROP INDEX IF EXISTS idx_quiz_questions_difficulty;
CREATE INDEX idx_quiz_questions_difficulty ON quiz_questions (difficulty);
COMMENT ON INDEX idx_quiz_questions_difficulty IS 'Filter questions by difficulty';

DROP INDEX IF EXISTS idx_quiz_questions_topic;
CREATE INDEX idx_quiz_questions_topic ON quiz_questions (topic);
COMMENT ON INDEX idx_quiz_questions_topic IS 'Filter questions by topic (legacy)';

DROP INDEX IF EXISTS idx_quiz_questions_topic_id;
CREATE INDEX idx_quiz_questions_topic_id ON quiz_questions (topic_id);
COMMENT ON INDEX idx_quiz_questions_topic_id IS 'Filter questions by topic ID';

DROP INDEX IF EXISTS idx_quiz_questions_creator;
CREATE INDEX idx_quiz_questions_creator ON quiz_questions (creator_id);
COMMENT ON INDEX idx_quiz_questions_creator IS 'Find questions created by a user';

DROP INDEX IF EXISTS idx_quiz_questions_external_id;
CREATE INDEX idx_quiz_questions_external_id ON quiz_questions (external_id);
COMMENT ON INDEX idx_quiz_questions_external_id IS 'Lookup by external system ID';

DROP INDEX IF EXISTS idx_quiz_questions_question_type;
CREATE INDEX idx_quiz_questions_question_type ON quiz_questions (question_type);
COMMENT ON INDEX idx_quiz_questions_question_type IS 'Filter by question type (TEXT, IMAGE, etc.)';

DROP INDEX IF EXISTS idx_quiz_questions_media_id;
CREATE INDEX idx_quiz_questions_media_id ON quiz_questions (question_media_id);
COMMENT ON INDEX idx_quiz_questions_media_id IS 'Find questions with specific media';

DROP INDEX IF EXISTS idx_quiz_questions_question_media_type;
CREATE INDEX idx_quiz_questions_question_media_type ON quiz_questions (question_media_type);
COMMENT ON INDEX idx_quiz_questions_question_media_type IS 'Filter by media type';

DROP INDEX IF EXISTS idx_quiz_questions_type_media_composite;
CREATE INDEX idx_quiz_questions_type_media_composite ON quiz_questions (question_type, question_media_type);
COMMENT ON INDEX idx_quiz_questions_type_media_composite IS 'Combined filter for question and media type';

DROP INDEX IF EXISTS idx_quiz_questions_visibility;
CREATE INDEX idx_quiz_questions_visibility ON quiz_questions (visibility);
COMMENT ON INDEX idx_quiz_questions_visibility IS 'Filter by visibility setting';

DROP INDEX IF EXISTS idx_quiz_questions_creator_visibility;
CREATE INDEX idx_quiz_questions_creator_visibility ON quiz_questions (creator_id, visibility);
COMMENT ON INDEX idx_quiz_questions_creator_visibility IS 'Find user questions by visibility';

DROP INDEX IF EXISTS idx_quiz_questions_original_quiz;
CREATE INDEX idx_quiz_questions_original_quiz ON quiz_questions (original_quiz_id);
COMMENT ON INDEX idx_quiz_questions_original_quiz IS 'Find questions from a specific quiz';

-- ============================================================================
-- QUIZ SESSIONS INDEXES
-- ============================================================================

DROP INDEX IF EXISTS idx_quiz_sessions_user;
CREATE INDEX idx_quiz_sessions_user ON quiz_sessions (user_id);
COMMENT ON INDEX idx_quiz_sessions_user IS 'Find sessions for a user';

DROP INDEX IF EXISTS idx_quiz_sessions_challenge;
CREATE INDEX idx_quiz_sessions_challenge ON quiz_sessions (challenge_id);
COMMENT ON INDEX idx_quiz_sessions_challenge IS 'Find sessions for a challenge';

DROP INDEX IF EXISTS idx_quiz_sessions_status;
CREATE INDEX idx_quiz_sessions_status ON quiz_sessions (status);
COMMENT ON INDEX idx_quiz_sessions_status IS 'Filter sessions by status';

-- ============================================================================
-- QUIZ ROUNDS INDEXES
-- ============================================================================

DROP INDEX IF EXISTS idx_quiz_rounds_session;
CREATE INDEX idx_quiz_rounds_session ON quiz_rounds (quiz_session_id);
COMMENT ON INDEX idx_quiz_rounds_session IS 'Find rounds for a quiz session';

DROP INDEX IF EXISTS idx_quiz_rounds_question;
CREATE INDEX idx_quiz_rounds_question ON quiz_rounds (question_id);
COMMENT ON INDEX idx_quiz_rounds_question IS 'Find usage of a specific question';

-- ============================================================================
-- PHOTOS TABLE INDEXES
-- ============================================================================

DROP INDEX IF EXISTS idx_photos_entity_id_photo_type;
CREATE INDEX idx_photos_entity_id_photo_type ON photos (entity_id, photo_type);
COMMENT ON INDEX idx_photos_entity_id_photo_type IS 'Find photos for an entity by type';

DROP INDEX IF EXISTS idx_photos_uploaded_by;
CREATE INDEX idx_photos_uploaded_by ON photos (uploaded_by);
COMMENT ON INDEX idx_photos_uploaded_by IS 'Find photos uploaded by a user';

DROP INDEX IF EXISTS idx_photos_photo_type;
CREATE INDEX idx_photos_photo_type ON photos (photo_type);
COMMENT ON INDEX idx_photos_photo_type IS 'Filter photos by type';

DROP INDEX IF EXISTS idx_photos_s3_key;
CREATE INDEX idx_photos_s3_key ON photos (s3_key);
COMMENT ON INDEX idx_photos_s3_key IS 'Lookup by S3 key';

DROP INDEX IF EXISTS idx_photos_created_at;
CREATE INDEX idx_photos_created_at ON photos (created_at);
COMMENT ON INDEX idx_photos_created_at IS 'Sort photos chronologically';

DROP INDEX IF EXISTS idx_photos_processing_status;
CREATE INDEX idx_photos_processing_status ON photos (processing_status);
COMMENT ON INDEX idx_photos_processing_status IS 'Filter by processing status';

-- ============================================================================
-- TOURNAMENT QUESTIONS INDEXES
-- ============================================================================

DROP INDEX IF EXISTS idx_tournament_questions_tournament_id;
CREATE INDEX idx_tournament_questions_tournament_id ON tournament_questions (tournament_id);
COMMENT ON INDEX idx_tournament_questions_tournament_id IS 'Find questions for a tournament';

DROP INDEX IF EXISTS idx_tournament_questions_quiz_question_id;
CREATE INDEX idx_tournament_questions_quiz_question_id ON tournament_questions (quiz_question_id);
COMMENT ON INDEX idx_tournament_questions_quiz_question_id IS 'Find tournaments using a question';

DROP INDEX IF EXISTS idx_tournament_questions_order;
CREATE INDEX idx_tournament_questions_order ON tournament_questions (tournament_id, display_order);
COMMENT ON INDEX idx_tournament_questions_order IS 'Efficient ordering of tournament questions';

DROP INDEX IF EXISTS idx_tournament_questions_is_active;
CREATE INDEX idx_tournament_questions_is_active ON tournament_questions (is_active);
COMMENT ON INDEX idx_tournament_questions_is_active IS 'Filter active/inactive questions';

-- ============================================================================
-- LEGACY QUESTIONS INDEXES
-- ============================================================================

DROP INDEX IF EXISTS idx_questions_old_tournament_id;
CREATE INDEX idx_questions_old_tournament_id ON questions_old (tournament_id);
COMMENT ON INDEX idx_questions_old_tournament_id IS 'Legacy table tournament lookup';

-- ============================================================================
-- REFRESH TOKENS INDEXES
-- ============================================================================

DROP INDEX IF EXISTS idx_refresh_token_token;
CREATE INDEX idx_refresh_token_token ON refresh_tokens (token);
COMMENT ON INDEX idx_refresh_token_token IS 'Fast token validation lookup';

DROP INDEX IF EXISTS idx_refresh_token_user_id;
CREATE INDEX idx_refresh_token_user_id ON refresh_tokens (user_id);
COMMENT ON INDEX idx_refresh_token_user_id IS 'Find all tokens for a user';

-- ============================================================================
-- CHALLENGE ACCESS INDEXES
-- ============================================================================

DROP INDEX IF EXISTS idx_challenge_access_challenge_id;
CREATE INDEX idx_challenge_access_challenge_id ON challenge_access (challenge_id);
COMMENT ON INDEX idx_challenge_access_challenge_id IS 'Find users with access to a challenge';

DROP INDEX IF EXISTS idx_challenge_access_user_id;
CREATE INDEX idx_challenge_access_user_id ON challenge_access (user_id);
COMMENT ON INDEX idx_challenge_access_user_id IS 'Find challenges a user can access';

DROP INDEX IF EXISTS idx_challenge_access_status;
CREATE INDEX idx_challenge_access_status ON challenge_access (status);
COMMENT ON INDEX idx_challenge_access_status IS 'Filter by access status';

-- ============================================================================
-- PAYMENT TRANSACTIONS INDEXES
-- ============================================================================

DROP INDEX IF EXISTS idx_payment_transactions_user_id;
CREATE INDEX idx_payment_transactions_user_id ON payment_transactions (user_id);
COMMENT ON INDEX idx_payment_transactions_user_id IS 'Find transactions for a user';

DROP INDEX IF EXISTS idx_payment_transactions_challenge_id;
CREATE INDEX idx_payment_transactions_challenge_id ON payment_transactions (challenge_id);
COMMENT ON INDEX idx_payment_transactions_challenge_id IS 'Find transactions for a challenge';

DROP INDEX IF EXISTS idx_payment_transactions_status;
CREATE INDEX idx_payment_transactions_status ON payment_transactions (status);
COMMENT ON INDEX idx_payment_transactions_status IS 'Filter transactions by status';

DROP INDEX IF EXISTS idx_payment_transactions_created_at;
CREATE INDEX idx_payment_transactions_created_at ON payment_transactions (created_at);
COMMENT ON INDEX idx_payment_transactions_created_at IS 'Sort transactions chronologically';

-- ============================================================================
-- QUESTION ACCESS LOG INDEXES
-- ============================================================================

DROP INDEX IF EXISTS idx_question_access_log_question;
CREATE INDEX idx_question_access_log_question ON question_access_log (question_id);
COMMENT ON INDEX idx_question_access_log_question IS 'Find access logs for a question';

DROP INDEX IF EXISTS idx_question_access_log_user;
CREATE INDEX idx_question_access_log_user ON question_access_log (accessed_by_user_id);
COMMENT ON INDEX idx_question_access_log_user IS 'Find access logs by user';

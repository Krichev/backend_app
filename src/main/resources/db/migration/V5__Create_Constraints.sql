-- ============================================================================
-- Flyway Migration V5: Create Foreign Key Constraints
-- ============================================================================
-- Description: Adds all foreign key constraints that were omitted from table
--              creation to prevent circular dependency issues.
--
--              Foreign keys are grouped by referenced table for clarity.
--
-- Version: V5
-- Author: Challenger System
-- Date: 2026-01-01
--
-- Organization:
--   - Constraints referencing users table
--   - Constraints referencing groups table
--   - Constraints referencing topics table
--   - Constraints referencing media_files table
--   - Constraints referencing challenges table
--   - Constraints referencing quests table
--   - Constraints referencing tasks table
--   - Constraints referencing rewards table
--   - Constraints referencing quiz_questions table
--   - Constraints referencing quiz_sessions table
--   - Constraints referencing challenge_progress table
--   - Constraints referencing location_coordinates table
--   - Constraints referencing photo_verification_details table
--   - Constraints referencing questions_old table
-- ============================================================================

-- ============================================================================
-- FOREIGN KEYS REFERENCING: users
-- ============================================================================

-- groups.creator_id -> users.id
ALTER TABLE groups
    ADD CONSTRAINT fk_groups_creator
        FOREIGN KEY (creator_id)
            REFERENCES users (id)
            ON DELETE CASCADE;
COMMENT ON CONSTRAINT fk_groups_creator ON groups IS 'Group creator must be a valid user';

-- topics.creator_id -> users.id
ALTER TABLE topics
    ADD CONSTRAINT fk_topics_creator
        FOREIGN KEY (creator_id)
            REFERENCES users (id)
            ON DELETE SET NULL;
COMMENT ON CONSTRAINT fk_topics_creator ON topics IS 'Topic creator reference (optional)';

-- media_files.uploaded_by -> users.id
ALTER TABLE media_files
    ADD CONSTRAINT fk_media_files_uploaded_by
        FOREIGN KEY (uploaded_by)
            REFERENCES users (id)
            ON DELETE CASCADE;
COMMENT ON CONSTRAINT fk_media_files_uploaded_by ON media_files IS 'User who uploaded the file';

-- quests.creator_id -> users.id
ALTER TABLE quests
    ADD CONSTRAINT fk_quests_creator
        FOREIGN KEY (creator_id)
            REFERENCES users (id)
            ON DELETE CASCADE;
COMMENT ON CONSTRAINT fk_quests_creator ON quests IS 'Quest creator must be a valid user';

-- challenges.creator_id -> users.id
ALTER TABLE challenges
    ADD CONSTRAINT fk_challenges_creator
        FOREIGN KEY (creator_id)
            REFERENCES users (id)
            ON DELETE CASCADE;
COMMENT ON CONSTRAINT fk_challenges_creator ON challenges IS 'Challenge creator must be a valid user';

-- stakes.user_id -> users.id
ALTER TABLE stakes
    ADD CONSTRAINT fk_stakes_user
        FOREIGN KEY (user_id)
            REFERENCES users (id)
            ON DELETE CASCADE;
COMMENT ON CONSTRAINT fk_stakes_user ON stakes IS 'User who placed the stake';

-- photos.uploaded_by -> users.id
ALTER TABLE photos
    ADD CONSTRAINT fk_photos_uploaded_by
        FOREIGN KEY (uploaded_by)
            REFERENCES users (id)
            ON DELETE CASCADE;
COMMENT ON CONSTRAINT fk_photos_uploaded_by ON photos IS 'User who uploaded the photo';

-- tasks.assigned_to -> users.id
ALTER TABLE tasks
    ADD CONSTRAINT fk_tasks_assigned_to
        FOREIGN KEY (assigned_to)
            REFERENCES users (id)
            ON DELETE SET NULL;
COMMENT ON CONSTRAINT fk_tasks_assigned_to ON tasks IS 'User assigned to the task (optional)';

-- tasks.created_by -> users.id
ALTER TABLE tasks
    ADD CONSTRAINT fk_tasks_created_by
        FOREIGN KEY (created_by)
            REFERENCES users (id)
            ON DELETE SET NULL;
COMMENT ON CONSTRAINT fk_tasks_created_by ON tasks IS 'User who created the task';

-- challenge_participants.user_id -> users.id
ALTER TABLE challenge_participants
    ADD CONSTRAINT fk_challenge_participants_user
        FOREIGN KEY (user_id)
            REFERENCES users (id)
            ON DELETE CASCADE;
COMMENT ON CONSTRAINT fk_challenge_participants_user ON challenge_participants IS 'Participating user';

-- challenge_progress.user_id -> users.id
ALTER TABLE challenge_progress
    ADD CONSTRAINT fk_challenge_progress_user
        FOREIGN KEY (user_id)
            REFERENCES users (id)
            ON DELETE CASCADE;
COMMENT ON CONSTRAINT fk_challenge_progress_user ON challenge_progress IS 'User whose progress is tracked';

-- challenge_progress.verified_by -> users.id
ALTER TABLE challenge_progress
    ADD CONSTRAINT fk_challenge_progress_verified_by
        FOREIGN KEY (verified_by)
            REFERENCES users (id)
            ON DELETE SET NULL;
COMMENT ON CONSTRAINT fk_challenge_progress_verified_by ON challenge_progress IS 'User who verified the progress';

-- user_quests.user_id -> users.id
ALTER TABLE user_quests
    ADD CONSTRAINT fk_user_quests_user
        FOREIGN KEY (user_id)
            REFERENCES users (id)
            ON DELETE CASCADE;
COMMENT ON CONSTRAINT fk_user_quests_user ON user_quests IS 'User participating in quest';

-- group_users.user_id -> users.id
ALTER TABLE group_users
    ADD CONSTRAINT fk_group_users_user
        FOREIGN KEY (user_id)
            REFERENCES users (id)
            ON DELETE CASCADE;
COMMENT ON CONSTRAINT fk_group_users_user ON group_users IS 'User who is group member';

-- task_completions.user_id -> users.id
ALTER TABLE task_completions
    ADD CONSTRAINT fk_task_completions_user
        FOREIGN KEY (user_id)
            REFERENCES users (id)
            ON DELETE CASCADE;
COMMENT ON CONSTRAINT fk_task_completions_user ON task_completions IS 'User who completed the task';

-- task_completions.verified_by -> users.id
ALTER TABLE task_completions
    ADD CONSTRAINT fk_task_completions_verified_by
        FOREIGN KEY (verified_by)
            REFERENCES users (id)
            ON DELETE SET NULL;
COMMENT ON CONSTRAINT fk_task_completions_verified_by ON task_completions IS 'User who verified completion';

-- reward_users.user_id -> users.id
ALTER TABLE reward_users
    ADD CONSTRAINT fk_reward_users_user
        FOREIGN KEY (user_id)
            REFERENCES users (id)
            ON DELETE CASCADE;
COMMENT ON CONSTRAINT fk_reward_users_user ON reward_users IS 'User who earned the reward';

-- user_connections.user_id -> users.id
ALTER TABLE user_connections
    ADD CONSTRAINT fk_user_connections_user
        FOREIGN KEY (user_id)
            REFERENCES users (id)
            ON DELETE CASCADE;
COMMENT ON CONSTRAINT fk_user_connections_user ON user_connections IS 'User initiating connection';

-- user_connections.connected_user_id -> users.id
ALTER TABLE user_connections
    ADD CONSTRAINT fk_user_connections_connected_user
        FOREIGN KEY (connected_user_id)
            REFERENCES users (id)
            ON DELETE CASCADE;
COMMENT ON CONSTRAINT fk_user_connections_connected_user ON user_connections IS 'User receiving connection request';

-- user_relationships.user_id -> users.id
ALTER TABLE user_relationships
    ADD CONSTRAINT fk_user_relationships_user
        FOREIGN KEY (user_id)
            REFERENCES users (id)
            ON DELETE CASCADE;
COMMENT ON CONSTRAINT fk_user_relationships_user ON user_relationships IS 'User in relationship';

-- user_relationships.related_user_id -> users.id
ALTER TABLE user_relationships
    ADD CONSTRAINT fk_user_relationships_related_user
        FOREIGN KEY (related_user_id)
            REFERENCES users (id)
            ON DELETE CASCADE;
COMMENT ON CONSTRAINT fk_user_relationships_related_user ON user_relationships IS 'Related user in relationship';

-- user_activity_logs.user_id -> users.id
ALTER TABLE user_activity_logs
    ADD CONSTRAINT fk_user_activity_logs_user
        FOREIGN KEY (user_id)
            REFERENCES users (id)
            ON DELETE CASCADE;
COMMENT ON CONSTRAINT fk_user_activity_logs_user ON user_activity_logs IS 'User performing the activity';

-- quiz_questions.creator_id -> users.id
ALTER TABLE quiz_questions
    ADD CONSTRAINT fk_quiz_questions_creator
        FOREIGN KEY (creator_id)
            REFERENCES users (id)
            ON DELETE SET NULL;
COMMENT ON CONSTRAINT fk_quiz_questions_creator ON quiz_questions IS 'User who created the question';

-- quiz_sessions.user_id -> users.id
ALTER TABLE quiz_sessions
    ADD CONSTRAINT fk_quiz_sessions_user
        FOREIGN KEY (user_id)
            REFERENCES users (id)
            ON DELETE CASCADE;
COMMENT ON CONSTRAINT fk_quiz_sessions_user ON quiz_sessions IS 'Primary user in quiz session';

-- quiz_sessions.host_user_id -> users.id
ALTER TABLE quiz_sessions
    ADD CONSTRAINT fk_quiz_sessions_host_user
        FOREIGN KEY (host_user_id)
            REFERENCES users (id)
            ON DELETE CASCADE;
COMMENT ON CONSTRAINT fk_quiz_sessions_host_user ON quiz_sessions IS 'Host user for quiz session';

-- quiz_sessions.creator_id -> users.id
ALTER TABLE quiz_sessions
    ADD CONSTRAINT fk_quiz_sessions_creator
        FOREIGN KEY (creator_id)
            REFERENCES users (id)
            ON DELETE CASCADE;
COMMENT ON CONSTRAINT fk_quiz_sessions_creator ON quiz_sessions IS 'User who created the session';

-- tournament_questions.added_by -> users.id
ALTER TABLE tournament_questions
    ADD CONSTRAINT fk_tournament_questions_added_by
        FOREIGN KEY (added_by)
            REFERENCES users (id)
            ON DELETE SET NULL;
COMMENT ON CONSTRAINT fk_tournament_questions_added_by ON tournament_questions IS 'User who added question to tournament';

-- refresh_tokens.user_id -> users.id
ALTER TABLE refresh_tokens
    ADD CONSTRAINT fk_refresh_token_user
        FOREIGN KEY (user_id)
            REFERENCES users (id)
            ON DELETE CASCADE;
COMMENT ON CONSTRAINT fk_refresh_token_user ON refresh_tokens IS 'User owning the refresh token';

-- challenge_access.user_id -> users.id
ALTER TABLE challenge_access
    ADD CONSTRAINT fk_challenge_access_user
        FOREIGN KEY (user_id)
            REFERENCES users (id)
            ON DELETE CASCADE;
COMMENT ON CONSTRAINT fk_challenge_access_user ON challenge_access IS 'User with access';

-- challenge_access.granted_by_user_id -> users.id
ALTER TABLE challenge_access
    ADD CONSTRAINT fk_challenge_access_granted_by
        FOREIGN KEY (granted_by_user_id)
            REFERENCES users (id)
            ON DELETE SET NULL;
COMMENT ON CONSTRAINT fk_challenge_access_granted_by ON challenge_access IS 'User who granted access';

-- payment_transactions.user_id -> users.id
ALTER TABLE payment_transactions
    ADD CONSTRAINT fk_payment_user
        FOREIGN KEY (user_id)
            REFERENCES users (id)
            ON DELETE CASCADE;
COMMENT ON CONSTRAINT fk_payment_user ON payment_transactions IS 'User involved in transaction';

-- question_access_log.accessed_by_user_id -> users.id
ALTER TABLE question_access_log
    ADD CONSTRAINT fk_question_access_log_user
        FOREIGN KEY (accessed_by_user_id)
            REFERENCES users (id)
            ON DELETE CASCADE;
COMMENT ON CONSTRAINT fk_question_access_log_user ON question_access_log IS 'User who accessed the question';

-- ============================================================================
-- FOREIGN KEYS REFERENCING: groups
-- ============================================================================

-- challenges.group_id -> groups.id
ALTER TABLE challenges
    ADD CONSTRAINT fk_challenges_group
        FOREIGN KEY (group_id)
            REFERENCES groups (id)
            ON DELETE SET NULL;
COMMENT ON CONSTRAINT fk_challenges_group ON challenges IS 'Associated group (optional)';

-- group_users.group_id -> groups.id
ALTER TABLE group_users
    ADD CONSTRAINT fk_group_users_group
        FOREIGN KEY (group_id)
            REFERENCES groups (id)
            ON DELETE CASCADE;
COMMENT ON CONSTRAINT fk_group_users_group ON group_users IS 'Group that user belongs to';

-- quest_groups.group_id -> groups.id
ALTER TABLE quest_groups
    ADD CONSTRAINT fk_quest_groups_group
        FOREIGN KEY (group_id)
            REFERENCES groups (id)
            ON DELETE CASCADE;
COMMENT ON CONSTRAINT fk_quest_groups_group ON quest_groups IS 'Group associated with quest';

-- ============================================================================
-- FOREIGN KEYS REFERENCING: topics
-- ============================================================================

-- quiz_questions.topic_id -> topics.id
ALTER TABLE quiz_questions
    ADD CONSTRAINT fk_quiz_question_topic
        FOREIGN KEY (topic_id)
            REFERENCES topics (id)
            ON DELETE SET NULL;
COMMENT ON CONSTRAINT fk_quiz_question_topic ON quiz_questions IS 'Topic categorization for question';

-- ============================================================================
-- FOREIGN KEYS REFERENCING: media_files
-- ============================================================================

-- quiz_questions.question_media_id -> media_files.id
ALTER TABLE quiz_questions
    ADD CONSTRAINT fk_quiz_questions_media_id
        FOREIGN KEY (question_media_id)
            REFERENCES media_files (id)
            ON DELETE SET NULL;
COMMENT ON CONSTRAINT fk_quiz_questions_media_id ON quiz_questions IS 'Media file for multimedia questions';

-- ============================================================================
-- FOREIGN KEYS REFERENCING: challenges
-- ============================================================================

-- quests.challenge_id -> challenges.id
ALTER TABLE quests
    ADD CONSTRAINT fk_quest_challenge
        FOREIGN KEY (challenge_id)
            REFERENCES challenges (id)
            ON DELETE CASCADE;
COMMENT ON CONSTRAINT fk_quest_challenge ON quests IS 'Associated challenge';

-- stakes.challenge_id -> challenges.id
ALTER TABLE stakes
    ADD CONSTRAINT fk_stakes_challenge
        FOREIGN KEY (challenge_id)
            REFERENCES challenges (id)
            ON DELETE CASCADE;
COMMENT ON CONSTRAINT fk_stakes_challenge ON stakes IS 'Challenge being staked on';

-- tasks.challenge_id -> challenges.id
ALTER TABLE tasks
    ADD CONSTRAINT fk_task_challenge
        FOREIGN KEY (challenge_id)
            REFERENCES challenges (id)
            ON DELETE CASCADE;
COMMENT ON CONSTRAINT fk_task_challenge ON tasks IS 'Challenge containing this task';

-- challenge_participants.challenge_id -> challenges.id
ALTER TABLE challenge_participants
    ADD CONSTRAINT fk_challenge_participants_challenge
        FOREIGN KEY (challenge_id)
            REFERENCES challenges (id)
            ON DELETE CASCADE;
COMMENT ON CONSTRAINT fk_challenge_participants_challenge ON challenge_participants IS 'Challenge being participated in';

-- challenge_progress.challenge_id -> challenges.id
ALTER TABLE challenge_progress
    ADD CONSTRAINT fk_challenge_progress_challenge
        FOREIGN KEY (challenge_id)
            REFERENCES challenges (id)
            ON DELETE CASCADE;
COMMENT ON CONSTRAINT fk_challenge_progress_challenge ON challenge_progress IS 'Challenge being tracked';

-- challenge_quests.challenge_id -> challenges.id
ALTER TABLE challenge_quests
    ADD CONSTRAINT fk_challenge_quests_challenge
        FOREIGN KEY (challenge_id)
            REFERENCES challenges (id)
            ON DELETE CASCADE;
COMMENT ON CONSTRAINT fk_challenge_quests_challenge ON challenge_quests IS 'Challenge in the relationship';

-- verification_details.challenge_id -> challenges.id
ALTER TABLE verification_details
    ADD CONSTRAINT fk_verification_details_challenge
        FOREIGN KEY (challenge_id)
            REFERENCES challenges (id)
            ON DELETE CASCADE;
COMMENT ON CONSTRAINT fk_verification_details_challenge ON verification_details IS 'Challenge requiring verification';

-- quiz_questions.challenge_id -> challenges.id
ALTER TABLE quiz_questions
    ADD CONSTRAINT fk_quiz_questions_challenge
        FOREIGN KEY (challenge_id)
            REFERENCES challenges (id)
            ON DELETE CASCADE;
COMMENT ON CONSTRAINT fk_quiz_questions_challenge ON quiz_questions IS 'Challenge this question belongs to';

-- quiz_questions.original_quiz_id -> challenges.id
ALTER TABLE quiz_questions
    ADD CONSTRAINT fk_question_original_quiz
        FOREIGN KEY (original_quiz_id)
            REFERENCES challenges (id)
            ON DELETE SET NULL;
COMMENT ON CONSTRAINT fk_question_original_quiz ON quiz_questions IS 'Original quiz where question was created (for QUIZ_ONLY visibility)';

-- quiz_sessions.challenge_id -> challenges.id
ALTER TABLE quiz_sessions
    ADD CONSTRAINT fk_quiz_sessions_challenge
        FOREIGN KEY (challenge_id)
            REFERENCES challenges (id)
            ON DELETE SET NULL;
COMMENT ON CONSTRAINT fk_quiz_sessions_challenge ON quiz_sessions IS 'Challenge this session belongs to';

-- challenge_access.challenge_id -> challenges.id
ALTER TABLE challenge_access
    ADD CONSTRAINT fk_challenge_access_challenge
        FOREIGN KEY (challenge_id)
            REFERENCES challenges (id)
            ON DELETE CASCADE;
COMMENT ON CONSTRAINT fk_challenge_access_challenge ON challenge_access IS 'Challenge being accessed';

-- payment_transactions.challenge_id -> challenges.id
ALTER TABLE payment_transactions
    ADD CONSTRAINT fk_payment_challenge
        FOREIGN KEY (challenge_id)
            REFERENCES challenges (id)
            ON DELETE SET NULL;
COMMENT ON CONSTRAINT fk_payment_challenge ON payment_transactions IS 'Challenge related to transaction';

-- ============================================================================
-- FOREIGN KEYS REFERENCING: quests
-- ============================================================================

-- tasks.quest_id -> quests.id
ALTER TABLE tasks
    ADD CONSTRAINT fk_tasks_quest
        FOREIGN KEY (quest_id)
            REFERENCES quests (id)
            ON DELETE CASCADE;
COMMENT ON CONSTRAINT fk_tasks_quest ON tasks IS 'Quest containing this task';

-- rewards.quest_id -> quests.id
ALTER TABLE rewards
    ADD CONSTRAINT fk_rewards_quest
        FOREIGN KEY (quest_id)
            REFERENCES quests (id)
            ON DELETE CASCADE;
COMMENT ON CONSTRAINT fk_rewards_quest ON rewards IS 'Quest offering this reward';

-- challenge_quests.quest_id -> quests.id
ALTER TABLE challenge_quests
    ADD CONSTRAINT fk_challenge_quests_quest
        FOREIGN KEY (quest_id)
            REFERENCES quests (id)
            ON DELETE CASCADE;
COMMENT ON CONSTRAINT fk_challenge_quests_quest ON challenge_quests IS 'Quest in the relationship';

-- user_quests.quest_id -> quests.id
ALTER TABLE user_quests
    ADD CONSTRAINT fk_user_quests_quest
        FOREIGN KEY (quest_id)
            REFERENCES quests (id)
            ON DELETE CASCADE;
COMMENT ON CONSTRAINT fk_user_quests_quest ON user_quests IS 'Quest being participated in';

-- quest_groups.quest_id -> quests.id
ALTER TABLE quest_groups
    ADD CONSTRAINT fk_quest_groups_quest
        FOREIGN KEY (quest_id)
            REFERENCES quests (id)
            ON DELETE CASCADE;
COMMENT ON CONSTRAINT fk_quest_groups_quest ON quest_groups IS 'Quest associated with group';

-- ============================================================================
-- FOREIGN KEYS REFERENCING: tasks
-- ============================================================================

-- task_completions.task_id -> tasks.id
ALTER TABLE task_completions
    ADD CONSTRAINT fk_task_completions_task
        FOREIGN KEY (task_id)
            REFERENCES tasks (id)
            ON DELETE CASCADE;
COMMENT ON CONSTRAINT fk_task_completions_task ON task_completions IS 'Task being completed';

-- ============================================================================
-- FOREIGN KEYS REFERENCING: rewards
-- ============================================================================

-- reward_users.reward_id -> rewards.id
ALTER TABLE reward_users
    ADD CONSTRAINT fk_reward_users_reward
        FOREIGN KEY (reward_id)
            REFERENCES rewards (id)
            ON DELETE CASCADE;
COMMENT ON CONSTRAINT fk_reward_users_reward ON reward_users IS 'Reward being awarded';

-- ============================================================================
-- FOREIGN KEYS REFERENCING: quiz_questions
-- ============================================================================

-- quiz_rounds.question_id -> quiz_questions.id
ALTER TABLE quiz_rounds
    ADD CONSTRAINT fk_quiz_rounds_question
        FOREIGN KEY (question_id)
            REFERENCES quiz_questions (id)
            ON DELETE CASCADE;
COMMENT ON CONSTRAINT fk_quiz_rounds_question ON quiz_rounds IS 'Question being answered in this round';

-- tournament_questions.quiz_question_id -> quiz_questions.id
ALTER TABLE tournament_questions
    ADD CONSTRAINT fk_tournament_question_quiz_question
        FOREIGN KEY (quiz_question_id)
            REFERENCES quiz_questions (id)
            ON DELETE RESTRICT;
COMMENT ON CONSTRAINT fk_tournament_question_quiz_question ON tournament_questions IS 'Quiz question used in tournament';

-- question_access_log.question_id -> quiz_questions.id
ALTER TABLE question_access_log
    ADD CONSTRAINT fk_question_access_log_question
        FOREIGN KEY (question_id)
            REFERENCES quiz_questions (id)
            ON DELETE CASCADE;
COMMENT ON CONSTRAINT fk_question_access_log_question ON question_access_log IS 'Question being accessed';

-- ============================================================================
-- FOREIGN KEYS REFERENCING: quiz_sessions
-- ============================================================================

-- quiz_rounds.quiz_session_id -> quiz_sessions.id
ALTER TABLE quiz_rounds
    ADD CONSTRAINT fk_quiz_rounds_session
        FOREIGN KEY (quiz_session_id)
            REFERENCES quiz_sessions (id)
            ON DELETE CASCADE;
COMMENT ON CONSTRAINT fk_quiz_rounds_session ON quiz_rounds IS 'Quiz session containing this round';

-- ============================================================================
-- FOREIGN KEYS REFERENCING: challenge_progress
-- ============================================================================

-- challenge_progress_completed_days.challenge_progress_id -> challenge_progress.id
ALTER TABLE challenge_progress_completed_days
    ADD CONSTRAINT fk_challenge_progress_completed_days
        FOREIGN KEY (challenge_progress_id)
            REFERENCES challenge_progress (id)
            ON DELETE CASCADE;
COMMENT ON CONSTRAINT fk_challenge_progress_completed_days ON challenge_progress_completed_days IS 'Progress record for completed days';

-- ============================================================================
-- FOREIGN KEYS REFERENCING: location_coordinates
-- ============================================================================

-- verification_details.location_coordinates_id -> location_coordinates.id
ALTER TABLE verification_details
    ADD CONSTRAINT fk_verification_details_location
        FOREIGN KEY (location_coordinates_id)
            REFERENCES location_coordinates (id)
            ON DELETE SET NULL;
COMMENT ON CONSTRAINT fk_verification_details_location ON verification_details IS 'Geographic coordinates for verification';

-- ============================================================================
-- FOREIGN KEYS REFERENCING: photo_verification_details
-- ============================================================================

-- verification_details.photo_details_id -> photo_verification_details.id
ALTER TABLE verification_details
    ADD CONSTRAINT fk_verification_details_photo
        FOREIGN KEY (photo_details_id)
            REFERENCES photo_verification_details (id)
            ON DELETE SET NULL;
COMMENT ON CONSTRAINT fk_verification_details_photo ON verification_details IS 'Photo verification configuration';

-- ============================================================================
-- FOREIGN KEYS REFERENCING: questions_old (legacy)
-- ============================================================================

-- quiz_questions.legacy_question_id -> questions_old.id
ALTER TABLE quiz_questions
    ADD CONSTRAINT fk_quiz_questions_legacy_question_id
        FOREIGN KEY (legacy_question_id)
            REFERENCES questions_old (id)
            ON DELETE SET NULL;
COMMENT ON CONSTRAINT fk_quiz_questions_legacy_question_id ON quiz_questions IS 'Link to legacy question data';

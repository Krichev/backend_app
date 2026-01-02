-- ============================================================================
-- Flyway Migration V6: Create Functions, Triggers, and Views
-- ============================================================================
-- Description: Creates all database functions, triggers, and views for the
--              Challenger application.
--
-- Version: V6
-- Author: Challenger System
-- Date: 2026-01-01
--
-- Contents:
--   - Utility functions for media type checking
--   - Functions for data retrieval and management
--   - Triggers for automatic timestamp updates
--   - Views for common query patterns
--   - Access control functions
-- ============================================================================

-- ============================================================================
-- UTILITY FUNCTIONS
-- ============================================================================

-- Function to update updated_at timestamp (used by many triggers)
CREATE OR REPLACE FUNCTION update_updated_at_column()
    RETURNS TRIGGER AS
$$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$
    LANGUAGE plpgsql;

COMMENT ON FUNCTION update_updated_at_column() IS 'Automatically updates the updated_at column to current timestamp';

-- Function to check if media is image
CREATE OR REPLACE FUNCTION is_image(media_t media_type)
    RETURNS BOOLEAN AS
$$
BEGIN
    RETURN media_t = 'IMAGE';
END;
$$
    LANGUAGE plpgsql IMMUTABLE;

COMMENT ON FUNCTION is_image(media_type) IS 'Returns true if media type is IMAGE';

-- Function to check if media is video
CREATE OR REPLACE FUNCTION is_video(media_t media_type)
    RETURNS BOOLEAN AS
$$
BEGIN
    RETURN media_t = 'VIDEO';
END;
$$
    LANGUAGE plpgsql IMMUTABLE;

COMMENT ON FUNCTION is_video(media_type) IS 'Returns true if media type is VIDEO';

-- Function to check if media is audio
CREATE OR REPLACE FUNCTION is_audio(media_t media_type)
    RETURNS BOOLEAN AS
$$
BEGIN
    RETURN media_t = 'AUDIO';
END;
$$
    LANGUAGE plpgsql IMMUTABLE;

COMMENT ON FUNCTION is_audio(media_type) IS 'Returns true if media type is AUDIO';

-- Function to check if processing is completed
CREATE OR REPLACE FUNCTION is_processing_completed(proc_status processing_status)
    RETURNS BOOLEAN AS
$$
BEGIN
    RETURN proc_status = 'COMPLETED';
END;
$$
    LANGUAGE plpgsql IMMUTABLE;

COMMENT ON FUNCTION is_processing_completed(processing_status) IS 'Returns true if processing status is COMPLETED';

-- Function to check if processing failed
CREATE OR REPLACE FUNCTION is_processing_failed(proc_status processing_status)
    RETURNS BOOLEAN AS
$$
BEGIN
    RETURN proc_status = 'FAILED';
END;
$$
    LANGUAGE plpgsql IMMUTABLE;

COMMENT ON FUNCTION is_processing_failed(processing_status) IS 'Returns true if processing status is FAILED';

-- ============================================================================
-- MEDIA MANAGEMENT FUNCTIONS
-- ============================================================================

-- Get media files by user
CREATE OR REPLACE FUNCTION get_media_files_by_user(
    p_user_id BIGINT,
    p_media_type media_type DEFAULT NULL,
    p_proc_status processing_status DEFAULT NULL,
    p_limit_count INTEGER DEFAULT 20,
    p_offset_count INTEGER DEFAULT 0
)
    RETURNS TABLE
            (
                id                BIGINT,
                original_filename VARCHAR(255),
                filename          VARCHAR(255),
                s3_url            TEXT,
                media_type        media_type,
                media_category    media_category,
                processing_status processing_status,
                file_size         BIGINT,
                uploaded_at       TIMESTAMP WITHOUT TIME ZONE
            )
AS
$$
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
        WHERE mf.uploaded_by = p_user_id
          AND (p_media_type IS NULL OR mf.media_type = p_media_type)
          AND (p_proc_status IS NULL OR mf.processing_status = p_proc_status)
        ORDER BY mf.uploaded_at DESC
        LIMIT p_limit_count OFFSET p_offset_count;
END;
$$
    LANGUAGE plpgsql;

COMMENT ON FUNCTION get_media_files_by_user(BIGINT, media_type, processing_status, INTEGER, INTEGER) IS 'Retrieves paginated media files for a user with optional filtering';

-- Get media files by entity
CREATE OR REPLACE FUNCTION get_media_files_by_entity(
    p_entity_id BIGINT,
    p_media_category media_category DEFAULT NULL,
    p_limit_count INTEGER DEFAULT 50
)
    RETURNS TABLE
            (
                id                BIGINT,
                original_filename VARCHAR(255),
                filename          VARCHAR(255),
                s3_url            TEXT,
                media_category    media_category,
                processing_status processing_status,
                uploaded_at       TIMESTAMP WITHOUT TIME ZONE
            )
AS
$$
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
        WHERE mf.entity_id = p_entity_id
          AND (p_media_category IS NULL OR mf.media_category = p_media_category)
          AND mf.processing_status = 'COMPLETED'
        ORDER BY mf.uploaded_at DESC
        LIMIT p_limit_count;
END;
$$
    LANGUAGE plpgsql;

COMMENT ON FUNCTION get_media_files_by_entity(BIGINT, media_category, INTEGER) IS 'Retrieves completed media files for an entity';

-- Update processing status
CREATE OR REPLACE FUNCTION update_processing_status(
    p_file_id BIGINT,
    p_new_status processing_status,
    p_new_processed_path VARCHAR(500) DEFAULT NULL,
    p_new_thumbnail_path VARCHAR(500) DEFAULT NULL
)
    RETURNS BOOLEAN AS
$$
BEGIN
    UPDATE media_files
    SET processing_status = p_new_status,
        processed_path    = COALESCE(p_new_processed_path, processed_path),
        thumbnail_path    = COALESCE(p_new_thumbnail_path, thumbnail_path),
        updated_at        = CURRENT_TIMESTAMP
    WHERE id = p_file_id;

    RETURN FOUND;
END;
$$
    LANGUAGE plpgsql;

COMMENT ON FUNCTION update_processing_status(BIGINT, processing_status, VARCHAR, VARCHAR) IS 'Updates media file processing status and optional paths';

-- Get files pending processing
CREATE OR REPLACE FUNCTION get_pending_processing_files(p_limit_count INTEGER DEFAULT 10)
    RETURNS TABLE
            (
                id           BIGINT,
                filename     VARCHAR(255),
                file_path    VARCHAR(500),
                s3_key       VARCHAR(500),
                s3_url       TEXT,
                media_type   media_type,
                content_type VARCHAR(100),
                uploaded_at  TIMESTAMP WITHOUT TIME ZONE
            )
AS
$$
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
        ORDER BY mf.uploaded_at ASC
        LIMIT p_limit_count;
END;
$$
    LANGUAGE plpgsql;

COMMENT ON FUNCTION get_pending_processing_files(INTEGER) IS 'Retrieves media files awaiting processing';

-- Clean up orphaned media files
CREATE OR REPLACE FUNCTION cleanup_orphaned_media_files()
    RETURNS INTEGER AS
$$
DECLARE
    deleted_count INTEGER;
BEGIN
    -- Delete media files that are not referenced by any quiz question
    -- and are older than 24 hours (in case of temporary uploads)
    DELETE
    FROM media_files
    WHERE entity_type = 'quiz_question'
      AND id NOT IN (SELECT question_media_id
                     FROM quiz_questions
                     WHERE question_media_id IS NOT NULL)
      AND created_at < NOW() - INTERVAL '24 hours';

    GET DIAGNOSTICS deleted_count = ROW_COUNT;

    RETURN deleted_count;
END;
$$
    LANGUAGE plpgsql;

COMMENT ON FUNCTION cleanup_orphaned_media_files() IS 'Deletes orphaned quiz question media files older than 24 hours';

-- Get media statistics
CREATE OR REPLACE FUNCTION get_media_statistics()
    RETURNS TABLE
            (
                total_media_files      BIGINT,
                total_size_mb          NUMERIC,
                video_files            BIGINT,
                audio_files            BIGINT,
                image_files            BIGINT,
                questions_with_media   BIGINT,
                avg_media_file_size_mb NUMERIC
            )
AS
$$
BEGIN
    RETURN QUERY
        SELECT COUNT(*)                                                 AS total_media_files,
               ROUND(SUM(file_size)::NUMERIC / 1024 / 1024, 2)          AS total_size_mb,
               COUNT(*) FILTER (WHERE media_type = 'VIDEO')             AS video_files,
               COUNT(*) FILTER (WHERE media_type = 'AUDIO')             AS audio_files,
               COUNT(*) FILTER (WHERE media_type = 'IMAGE')             AS image_files,
               (SELECT COUNT(*) FROM quiz_questions WHERE question_media_id IS NOT NULL) AS questions_with_media,
               ROUND(AVG(file_size)::NUMERIC / 1024 / 1024, 2)          AS avg_media_file_size_mb
        FROM media_files
        WHERE media_category = 'QUIZ_QUESTION';
END;
$$
    LANGUAGE plpgsql;

COMMENT ON FUNCTION get_media_statistics() IS 'Returns statistics about media file usage';

-- Get user media quota usage
CREATE OR REPLACE FUNCTION get_user_media_quota(p_user_id BIGINT)
    RETURNS TABLE
            (
                total_files   BIGINT,
                total_size_mb NUMERIC,
                video_files   BIGINT,
                video_size_mb NUMERIC,
                audio_files   BIGINT,
                audio_size_mb NUMERIC,
                image_files   BIGINT,
                image_size_mb NUMERIC
            )
AS
$$
BEGIN
    RETURN QUERY
        SELECT COUNT(*)                                                              AS total_files,
               ROUND(SUM(file_size)::NUMERIC / 1024 / 1024, 2)                       AS total_size_mb,
               COUNT(*) FILTER (WHERE media_type = 'VIDEO')                          AS video_files,
               ROUND(SUM(file_size) FILTER (WHERE media_type = 'VIDEO')::NUMERIC / 1024 / 1024, 2) AS video_size_mb,
               COUNT(*) FILTER (WHERE media_type = 'AUDIO')                          AS audio_files,
               ROUND(SUM(file_size) FILTER (WHERE media_type = 'AUDIO')::NUMERIC / 1024 / 1024, 2) AS audio_size_mb,
               COUNT(*) FILTER (WHERE media_type = 'IMAGE')                          AS image_files,
               ROUND(SUM(file_size) FILTER (WHERE media_type = 'IMAGE')::NUMERIC / 1024 / 1024, 2) AS image_size_mb
        FROM media_files
        WHERE uploaded_by = p_user_id
          AND media_category = 'QUIZ_QUESTION';
END;
$$
    LANGUAGE plpgsql;

COMMENT ON FUNCTION get_user_media_quota(BIGINT) IS 'Returns media file quota usage for a user';

-- ============================================================================
-- QUIZ AND ACCESS CONTROL FUNCTIONS
-- ============================================================================

-- Update media usage statistics (trigger function)
CREATE OR REPLACE FUNCTION update_media_usage_stats()
    RETURNS TRIGGER AS
$$
BEGIN
    -- Update usage count when a question with media is used in a quiz round
    IF NEW.question_id IS NOT NULL THEN
        UPDATE quiz_questions
        SET usage_count = usage_count + 1
        WHERE id = NEW.question_id
          AND question_media_id IS NOT NULL;
    END IF;

    RETURN NEW;
END;
$$
    LANGUAGE plpgsql;

COMMENT ON FUNCTION update_media_usage_stats() IS 'Trigger function to update question usage statistics';

-- Check if user can access a question
CREATE OR REPLACE FUNCTION can_user_access_question(
    p_question_id BIGINT,
    p_user_id BIGINT
) RETURNS BOOLEAN AS
$$
DECLARE
    v_visibility       question_visibility;
    v_creator_id       BIGINT;
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
            SELECT EXISTS(SELECT 1
                          FROM user_relationships
                          WHERE ((user_id = v_creator_id AND related_user_id = p_user_id)
                              OR (user_id = p_user_id AND related_user_id = v_creator_id))
                            AND status = 'ACCEPTED'
                            AND relationship_type IN ('FRIEND', 'FAMILY'))
            INTO v_has_relationship;
            RETURN v_has_relationship;

        WHEN 'QUIZ_ONLY' THEN
            -- Check if user has access to the original quiz
            RETURN EXISTS(SELECT 1
                          FROM challenges c
                          WHERE c.id = v_original_quiz_id
                            AND (c.creator_id = p_user_id
                              OR c.is_public = TRUE
                              OR EXISTS(SELECT 1
                                        FROM user_quests uq
                                        WHERE uq.quest_id = v_original_quiz_id
                                          AND uq.user_id = p_user_id)));

        ELSE
            RETURN FALSE;
        END CASE;
END;
$$ LANGUAGE plpgsql STABLE;

COMMENT ON FUNCTION can_user_access_question(BIGINT, BIGINT) IS 'Determines if a user can access a specific question based on visibility and relationships';

-- ============================================================================
-- TRIGGERS FOR AUTOMATIC TIMESTAMP UPDATES
-- ============================================================================

DROP TRIGGER IF EXISTS update_users_updated_at ON users;
CREATE TRIGGER update_users_updated_at
    BEFORE UPDATE
    ON users
    FOR EACH ROW
EXECUTE FUNCTION update_updated_at_column();

DROP TRIGGER IF EXISTS update_groups_updated_at ON groups;
CREATE TRIGGER update_groups_updated_at
    BEFORE UPDATE
    ON groups
    FOR EACH ROW
EXECUTE FUNCTION update_updated_at_column();

DROP TRIGGER IF EXISTS update_quests_updated_at ON quests;
CREATE TRIGGER update_quests_updated_at
    BEFORE UPDATE
    ON quests
    FOR EACH ROW
EXECUTE FUNCTION update_updated_at_column();

DROP TRIGGER IF EXISTS update_tasks_updated_at ON tasks;
CREATE TRIGGER update_tasks_updated_at
    BEFORE UPDATE
    ON tasks
    FOR EACH ROW
EXECUTE FUNCTION update_updated_at_column();

DROP TRIGGER IF EXISTS update_challenges_updated_at ON challenges;
CREATE TRIGGER update_challenges_updated_at
    BEFORE UPDATE
    ON challenges
    FOR EACH ROW
EXECUTE FUNCTION update_updated_at_column();

DROP TRIGGER IF EXISTS update_challenge_progress_updated_at ON challenge_progress;
CREATE TRIGGER update_challenge_progress_updated_at
    BEFORE UPDATE
    ON challenge_progress
    FOR EACH ROW
EXECUTE FUNCTION update_updated_at_column();

DROP TRIGGER IF EXISTS update_user_connections_updated_at ON user_connections;
CREATE TRIGGER update_user_connections_updated_at
    BEFORE UPDATE
    ON user_connections
    FOR EACH ROW
EXECUTE FUNCTION update_updated_at_column();

DROP TRIGGER IF EXISTS update_quiz_questions_updated_at ON quiz_questions;
CREATE TRIGGER update_quiz_questions_updated_at
    BEFORE UPDATE
    ON quiz_questions
    FOR EACH ROW
EXECUTE FUNCTION update_updated_at_column();

DROP TRIGGER IF EXISTS update_quiz_sessions_updated_at ON quiz_sessions;
CREATE TRIGGER update_quiz_sessions_updated_at
    BEFORE UPDATE
    ON quiz_sessions
    FOR EACH ROW
EXECUTE FUNCTION update_updated_at_column();

DROP TRIGGER IF EXISTS update_media_files_updated_at ON media_files;
CREATE TRIGGER update_media_files_updated_at
    BEFORE UPDATE
    ON media_files
    FOR EACH ROW
EXECUTE FUNCTION update_updated_at_column();

DROP TRIGGER IF EXISTS update_payment_transactions_updated_at ON payment_transactions;
CREATE TRIGGER update_payment_transactions_updated_at
    BEFORE UPDATE
    ON payment_transactions
    FOR EACH ROW
EXECUTE FUNCTION update_updated_at_column();

-- ============================================================================
-- TRIGGERS FOR BUSINESS LOGIC
-- ============================================================================

DROP TRIGGER IF EXISTS trigger_update_media_usage ON quiz_rounds;
CREATE TRIGGER trigger_update_media_usage
    AFTER INSERT
    ON quiz_rounds
    FOR EACH ROW
EXECUTE FUNCTION update_media_usage_stats();

-- ============================================================================
-- VIEWS FOR COMMON QUERY PATTERNS
-- ============================================================================

-- View for completed media files
CREATE OR REPLACE VIEW completed_media_files AS
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

COMMENT ON VIEW completed_media_files IS 'Returns only successfully processed media files';

-- View for media files with dimensions
CREATE OR REPLACE VIEW media_files_with_dimensions AS
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

COMMENT ON VIEW media_files_with_dimensions IS 'Returns completed media files that have dimension data (images and videos)';

-- View for user profile pictures
CREATE OR REPLACE VIEW user_profile_pictures AS
SELECT DISTINCT ON (uploaded_by)
    id,
    original_filename,
    filename,
    s3_url,
    width,
    height,
    uploaded_by,
    uploaded_at
FROM media_files
WHERE media_category = 'AVATAR'
  AND processing_status = 'COMPLETED'
ORDER BY uploaded_by, uploaded_at DESC;

COMMENT ON VIEW user_profile_pictures IS 'Returns the most recent profile picture for each user';

-- View for challenge difficulty statistics
CREATE OR REPLACE VIEW challenge_difficulty_stats AS
SELECT difficulty,
       COUNT(*)                                                      AS total_challenges,
       COUNT(*) FILTER (WHERE status = 'ACTIVE')                     AS active_challenges,
       COUNT(*) FILTER (WHERE status = 'COMPLETED')                  AS completed_challenges,
       AVG(CASE WHEN status = 'COMPLETED' THEN 100.0 ELSE 0.0 END)   AS completion_rate
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

COMMENT ON VIEW challenge_difficulty_stats IS 'Statistics about challenges grouped by difficulty level';

-- View for multimedia quiz questions
CREATE OR REPLACE VIEW multimedia_quiz_questions AS
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
       u.username           AS creator_name,
       qq.usage_count,
       qq.created_at,
       qq.updated_at,
       mf.id                AS media_id,
       mf.original_filename AS media_filename,
       mf.content_type      AS media_mime_type,
       mf.file_size         AS media_size,
       mf.duration_seconds  AS media_duration,
       mf.width             AS media_width,
       mf.height            AS media_height,
       mf.processing_status AS media_processing_status,
       mf.thumbnail_path    AS media_thumbnail_url
FROM quiz_questions qq
         LEFT JOIN users u ON qq.creator_id = u.id
         LEFT JOIN media_files mf ON qq.question_media_id = mf.id;

COMMENT ON VIEW multimedia_quiz_questions IS 'Comprehensive view of quiz questions with their media and creator information';

-- View for challenge payment statistics
CREATE OR REPLACE VIEW challenge_payment_stats AS
SELECT c.id,
       c.title,
       c.payment_type,
       c.entry_fee_amount,
       c.entry_fee_currency,
       c.prize_pool,
       COUNT(DISTINCT cp.user_id)                                          AS participant_count,
       SUM(CASE WHEN pt.status = 'COMPLETED' THEN pt.amount ELSE 0 END)    AS total_collected
FROM challenges c
         LEFT JOIN challenge_progress cp ON c.id = cp.challenge_id
         LEFT JOIN payment_transactions pt ON c.id = pt.challenge_id
    AND pt.transaction_type = 'ENTRY_FEE'
GROUP BY c.id, c.title, c.payment_type, c.entry_fee_amount,
         c.entry_fee_currency, c.prize_pool;

COMMENT ON VIEW challenge_payment_stats IS 'Financial statistics for challenges including participant count and collected fees';

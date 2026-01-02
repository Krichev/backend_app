-- ============================================================================
-- Migration: Add Hierarchical Topics and Content Validation Workflow
-- Version: V15
-- Description: Adds parent-child relationships to topics, materialized path
--              for efficient tree queries, and validation status for
--              user-generated content moderation workflow.
-- Author: Challenger System
-- Date: 2025-12-03
-- ============================================================================

-- ============================================================================
-- STEP 1: Create validation_status ENUM type
-- ============================================================================
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'validation_status') THEN
        CREATE TYPE validation_status AS ENUM (
            'DRAFT',           -- Just created, not submitted for review
            'PENDING',         -- Submitted for validation, awaiting review
            'APPROVED',        -- Validated and approved for public use
            'REJECTED',        -- Rejected by moderator (with reason)
            'AUTO_APPROVED'    -- Approved by AI (future use)
        );
        RAISE NOTICE 'Created validation_status enum type';
    ELSE
        RAISE NOTICE 'validation_status enum type already exists';
    END IF;
END$$;

-- ============================================================================
-- STEP 2: Add PENDING_PUBLIC to question_visibility enum
-- Note: PostgreSQL allows adding values to enums but not removing/reordering
-- ============================================================================
DO $$
BEGIN
    -- Check if PENDING_PUBLIC already exists in the enum
    IF NOT EXISTS (
        SELECT 1 FROM pg_enum
        WHERE enumtypid = 'question_visibility'::regtype
        AND enumlabel = 'PENDING_PUBLIC'
    ) THEN
        -- Add PENDING_PUBLIC before PUBLIC
        ALTER TYPE question_visibility ADD VALUE 'PENDING_PUBLIC' BEFORE 'PUBLIC';
        RAISE NOTICE 'Added PENDING_PUBLIC to question_visibility enum';
    ELSE
        RAISE NOTICE 'PENDING_PUBLIC already exists in question_visibility enum';
    END IF;
END$$;

-- ============================================================================
-- STEP 3: Add hierarchical columns to topics table
-- ============================================================================

-- 3.1: Add parent_id for tree structure
ALTER TABLE topics
ADD COLUMN IF NOT EXISTS parent_id BIGINT;

-- 3.2: Add self-referencing foreign key
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.table_constraints
        WHERE constraint_name = 'fk_topic_parent'
        AND table_name = 'topics'
    ) THEN
        ALTER TABLE topics
        ADD CONSTRAINT fk_topic_parent
        FOREIGN KEY (parent_id) REFERENCES topics(id) ON DELETE SET NULL;
        RAISE NOTICE 'Added parent_id foreign key constraint';
    END IF;
END$$;

-- 3.3: Add materialized path for efficient subtree queries
-- Format: "/1/5/23/" where numbers are topic IDs
ALTER TABLE topics
ADD COLUMN IF NOT EXISTS path VARCHAR(500);

-- 3.4: Add depth level (0 = root, 1 = first level child, etc.)
ALTER TABLE topics
ADD COLUMN IF NOT EXISTS depth INTEGER DEFAULT 0;

-- 3.5: Add URL-friendly slug
ALTER TABLE topics
ADD COLUMN IF NOT EXISTS slug VARCHAR(200);

-- 3.6: Add system topic flag (true for app-imported topics)
ALTER TABLE topics
ADD COLUMN IF NOT EXISTS is_system_topic BOOLEAN DEFAULT FALSE;

-- 3.7: Add validation status
ALTER TABLE topics
ADD COLUMN IF NOT EXISTS validation_status validation_status DEFAULT 'DRAFT';

-- 3.8: Add validation tracking fields
ALTER TABLE topics
ADD COLUMN IF NOT EXISTS validated_by BIGINT;

ALTER TABLE topics
ADD COLUMN IF NOT EXISTS validated_at TIMESTAMP;

ALTER TABLE topics
ADD COLUMN IF NOT EXISTS rejection_reason VARCHAR(500);

-- 3.9: Add foreign key for validated_by
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.table_constraints
        WHERE constraint_name = 'fk_topic_validated_by'
        AND table_name = 'topics'
    ) THEN
        ALTER TABLE topics
        ADD CONSTRAINT fk_topic_validated_by
        FOREIGN KEY (validated_by) REFERENCES users(id) ON DELETE SET NULL;
        RAISE NOTICE 'Added validated_by foreign key constraint';
    END IF;
END$$;

-- ============================================================================
-- STEP 4: Add validation columns to quiz_questions table
-- ============================================================================

ALTER TABLE quiz_questions
ADD COLUMN IF NOT EXISTS validation_status validation_status DEFAULT 'DRAFT';

ALTER TABLE quiz_questions
ADD COLUMN IF NOT EXISTS validated_by BIGINT;

ALTER TABLE quiz_questions
ADD COLUMN IF NOT EXISTS validated_at TIMESTAMP;

ALTER TABLE quiz_questions
ADD COLUMN IF NOT EXISTS rejection_reason VARCHAR(500);

-- Add foreign key for validated_by
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.table_constraints
        WHERE constraint_name = 'fk_question_validated_by'
        AND table_name = 'quiz_questions'
    ) THEN
        ALTER TABLE quiz_questions
        ADD CONSTRAINT fk_question_validated_by
        FOREIGN KEY (validated_by) REFERENCES users(id) ON DELETE SET NULL;
        RAISE NOTICE 'Added validated_by foreign key to quiz_questions';
    END IF;
END$$;

-- ============================================================================
-- STEP 5: Create validation_history table for audit trail
-- ============================================================================

CREATE TABLE IF NOT EXISTS content_validation_history (
    id BIGSERIAL PRIMARY KEY,

    -- Polymorphic reference to content (topic or question)
    content_type VARCHAR(50) NOT NULL,  -- 'TOPIC' or 'QUESTION'
    content_id BIGINT NOT NULL,

    -- Validation action
    previous_status validation_status,
    new_status validation_status NOT NULL,

    -- Who performed the action
    validated_by BIGINT NOT NULL,

    -- Additional context
    reason VARCHAR(500),
    notes TEXT,

    -- Metadata
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,

    CONSTRAINT fk_validation_history_user
        FOREIGN KEY (validated_by) REFERENCES users(id) ON DELETE CASCADE
);

-- Add index for efficient lookups
CREATE INDEX IF NOT EXISTS idx_validation_history_content
ON content_validation_history(content_type, content_id);

CREATE INDEX IF NOT EXISTS idx_validation_history_validator
ON content_validation_history(validated_by);

CREATE INDEX IF NOT EXISTS idx_validation_history_created
ON content_validation_history(created_at);

-- ============================================================================
-- STEP 6: Create indexes for performance
-- ============================================================================

-- Topics hierarchy indexes
CREATE INDEX IF NOT EXISTS idx_topics_parent_id ON topics(parent_id);
CREATE INDEX IF NOT EXISTS idx_topics_path ON topics(path);
CREATE INDEX IF NOT EXISTS idx_topics_path_pattern ON topics(path varchar_pattern_ops);
CREATE INDEX IF NOT EXISTS idx_topics_depth ON topics(depth);
CREATE INDEX IF NOT EXISTS idx_topics_slug ON topics(slug);
CREATE INDEX IF NOT EXISTS idx_topics_validation_status ON topics(validation_status);
CREATE INDEX IF NOT EXISTS idx_topics_is_system ON topics(is_system_topic);

-- Composite index for finding selectable topics
CREATE INDEX IF NOT EXISTS idx_topics_selectable
ON topics(is_active, validation_status)
WHERE is_active = true;

-- Composite index for finding pending topics
CREATE INDEX IF NOT EXISTS idx_topics_pending
ON topics(validation_status, created_at)
WHERE validation_status = 'PENDING';

-- Quiz questions validation indexes
CREATE INDEX IF NOT EXISTS idx_questions_validation_status
ON quiz_questions(validation_status);

-- Composite index for pending questions
CREATE INDEX IF NOT EXISTS idx_questions_pending
ON quiz_questions(validation_status, created_at)
WHERE validation_status = 'PENDING';

-- Composite index for approved public questions
CREATE INDEX IF NOT EXISTS idx_questions_approved_public
ON quiz_questions(visibility, validation_status)
WHERE visibility = 'PUBLIC' AND validation_status = 'APPROVED';

-- ============================================================================
-- STEP 7: Create unique constraint for slug (only for active topics)
-- ============================================================================

-- Drop existing constraint if any
DROP INDEX IF EXISTS idx_topics_slug_unique;

-- Create partial unique index (slug must be unique among active topics)
CREATE UNIQUE INDEX IF NOT EXISTS idx_topics_slug_unique
ON topics(slug)
WHERE is_active = true AND slug IS NOT NULL;

-- ============================================================================
-- STEP 8: Migrate existing data
-- ============================================================================

-- 8.1: Mark existing topics without creator as system topics and approved
UPDATE topics
SET
    is_system_topic = TRUE,
    validation_status = 'APPROVED',
    validated_at = CURRENT_TIMESTAMP,
    depth = 0,
    path = '/' || id::text || '/'
WHERE creator_id IS NULL
AND is_system_topic IS NOT TRUE;

-- 8.2: Mark existing user-created topics as approved (grandfather clause)
UPDATE topics
SET
    is_system_topic = FALSE,
    validation_status = 'APPROVED',
    validated_at = CURRENT_TIMESTAMP,
    depth = 0,
    path = '/' || id::text || '/'
WHERE creator_id IS NOT NULL
AND validation_status = 'DRAFT';

-- 8.3: Generate slugs for existing topics
UPDATE topics
SET slug = LOWER(
    REGEXP_REPLACE(
        REGEXP_REPLACE(
            REGEXP_REPLACE(name, '[^a-zA-Z0-9\s-]', '', 'g'),  -- Remove special chars
            '\s+', '-', 'g'                                      -- Replace spaces with hyphens
        ),
        '-+', '-', 'g'                                          -- Remove duplicate hyphens
    )
)
WHERE slug IS NULL;

-- 8.4: Handle duplicate slugs by appending ID
UPDATE topics t1
SET slug = t1.slug || '-' || t1.id::text
WHERE EXISTS (
    SELECT 1 FROM topics t2
    WHERE t2.slug = t1.slug
    AND t2.id < t1.id
    AND t2.is_active = true
);

-- 8.5: Mark existing PUBLIC questions as approved
UPDATE quiz_questions
SET
    validation_status = 'APPROVED',
    validated_at = CURRENT_TIMESTAMP
WHERE visibility = 'PUBLIC'
AND validation_status = 'DRAFT';

-- 8.6: Mark all non-user-created questions as approved
UPDATE quiz_questions
SET
    validation_status = 'APPROVED',
    validated_at = CURRENT_TIMESTAMP
WHERE is_user_created = FALSE
AND validation_status = 'DRAFT';

-- 8.7: Mark user-created non-PUBLIC questions as approved (grandfather clause)
UPDATE quiz_questions
SET
    validation_status = 'APPROVED',
    validated_at = CURRENT_TIMESTAMP
WHERE is_user_created = TRUE
AND visibility != 'PUBLIC'
AND validation_status = 'DRAFT';

-- ============================================================================
-- STEP 9: Create helper functions for tree operations
-- ============================================================================

-- 9.1: Function to generate slug from name
CREATE OR REPLACE FUNCTION generate_topic_slug(
    p_name VARCHAR,
    p_parent_id BIGINT DEFAULT NULL
) RETURNS VARCHAR AS $$
DECLARE
    v_base_slug VARCHAR;
    v_slug VARCHAR;
    v_counter INTEGER := 0;
    v_parent_slug VARCHAR;
BEGIN
    -- Generate base slug from name
    v_base_slug := LOWER(
        REGEXP_REPLACE(
            REGEXP_REPLACE(
                REGEXP_REPLACE(p_name, '[^a-zA-Z0-9\s-]', '', 'g'),
                '\s+', '-', 'g'
            ),
            '-+', '-', 'g'
        )
    );

    -- Trim leading/trailing hyphens
    v_base_slug := TRIM(BOTH '-' FROM v_base_slug);

    -- If parent exists, prepend parent slug
    IF p_parent_id IS NOT NULL THEN
        SELECT slug INTO v_parent_slug FROM topics WHERE id = p_parent_id;
        IF v_parent_slug IS NOT NULL THEN
            v_base_slug := v_parent_slug || '-' || v_base_slug;
        END IF;
    END IF;

    -- Ensure uniqueness
    v_slug := v_base_slug;
    WHILE EXISTS (SELECT 1 FROM topics WHERE slug = v_slug AND is_active = true) LOOP
        v_counter := v_counter + 1;
        v_slug := v_base_slug || '-' || v_counter::text;
    END LOOP;

    RETURN v_slug;
END;
$$ LANGUAGE plpgsql;

-- 9.2: Function to build materialized path
CREATE OR REPLACE FUNCTION build_topic_path(p_topic_id BIGINT)
RETURNS VARCHAR AS $$
DECLARE
    v_path VARCHAR := '';
    v_current_id BIGINT := p_topic_id;
    v_parent_id BIGINT;
    v_ids BIGINT[] := ARRAY[]::BIGINT[];
BEGIN
    -- Walk up the tree collecting IDs
    WHILE v_current_id IS NOT NULL LOOP
        v_ids := array_prepend(v_current_id, v_ids);
        SELECT parent_id INTO v_parent_id FROM topics WHERE id = v_current_id;
        v_current_id := v_parent_id;
    END LOOP;

    -- Build path string
    v_path := '/';
    FOR i IN 1..array_length(v_ids, 1) LOOP
        v_path := v_path || v_ids[i]::text || '/';
    END LOOP;

    RETURN v_path;
END;
$$ LANGUAGE plpgsql;

-- 9.3: Function to calculate depth
CREATE OR REPLACE FUNCTION calculate_topic_depth(p_topic_id BIGINT)
RETURNS INTEGER AS $$
DECLARE
    v_depth INTEGER := 0;
    v_current_id BIGINT := p_topic_id;
    v_parent_id BIGINT;
BEGIN
    SELECT parent_id INTO v_parent_id FROM topics WHERE id = v_current_id;

    WHILE v_parent_id IS NOT NULL LOOP
        v_depth := v_depth + 1;
        SELECT parent_id INTO v_parent_id FROM topics WHERE id = v_parent_id;
    END LOOP;

    RETURN v_depth;
END;
$$ LANGUAGE plpgsql;

-- 9.4: Function to get full topic path as text (e.g., "Geography > Geology > Minerals")
CREATE OR REPLACE FUNCTION get_topic_path_text(p_topic_id BIGINT)
RETURNS VARCHAR AS $$
DECLARE
    v_result VARCHAR := '';
    v_name VARCHAR;
    rec RECORD;
BEGIN
    FOR rec IN
        WITH RECURSIVE topic_tree AS (
            SELECT id, name, parent_id, 0 as level
            FROM topics
            WHERE id = p_topic_id

            UNION ALL

            SELECT t.id, t.name, t.parent_id, tt.level + 1
            FROM topics t
            JOIN topic_tree tt ON t.id = tt.parent_id
        )
        SELECT name FROM topic_tree ORDER BY level DESC
    LOOP
        IF v_result != '' THEN
            v_result := v_result || ' > ';
        END IF;
        v_result := v_result || rec.name;
    END LOOP;

    RETURN v_result;
END;
$$ LANGUAGE plpgsql;

-- 9.5: Function to check if user can access question
CREATE OR REPLACE FUNCTION can_user_access_question_v2(
    p_question_id BIGINT,
    p_user_id BIGINT
) RETURNS BOOLEAN AS $$
DECLARE
    v_visibility question_visibility;
    v_validation_status validation_status;
    v_creator_id BIGINT;
    v_original_quiz_id BIGINT;
    v_has_relationship BOOLEAN;
BEGIN
    -- Get question details
    SELECT visibility, validation_status, creator_id, original_quiz_id
    INTO v_visibility, v_validation_status, v_creator_id, v_original_quiz_id
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
            -- Only approved public questions are accessible
            RETURN v_validation_status IN ('APPROVED', 'AUTO_APPROVED');

        WHEN 'PENDING_PUBLIC' THEN
            -- Only creator can see pending public
            RETURN FALSE;

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
            IF v_original_quiz_id IS NULL THEN
                RETURN FALSE;
            END IF;
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

-- ============================================================================
-- STEP 10: Create trigger to maintain path/depth on topic changes
-- ============================================================================

-- 10.1: Trigger function to update path and depth
CREATE OR REPLACE FUNCTION update_topic_hierarchy()
RETURNS TRIGGER AS $$
BEGIN
    -- Calculate and set depth
    NEW.depth := calculate_topic_depth(NEW.id);

    -- Build and set path
    IF NEW.parent_id IS NULL THEN
        NEW.path := '/' || NEW.id::text || '/';
    ELSE
        NEW.path := build_topic_path(NEW.id);
    END IF;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- 10.2: Create trigger for INSERT
DROP TRIGGER IF EXISTS trg_topic_hierarchy_insert ON topics;
CREATE TRIGGER trg_topic_hierarchy_insert
    BEFORE INSERT ON topics
    FOR EACH ROW
    EXECUTE FUNCTION update_topic_hierarchy();

-- 10.3: Create trigger for UPDATE (when parent changes)
CREATE OR REPLACE FUNCTION update_topic_hierarchy_on_parent_change()
RETURNS TRIGGER AS $$
BEGIN
    IF OLD.parent_id IS DISTINCT FROM NEW.parent_id THEN
        NEW.depth := calculate_topic_depth(NEW.id);
        NEW.path := build_topic_path(NEW.id);

        -- Also update all descendants
        UPDATE topics
        SET
            depth = calculate_topic_depth(id),
            path = build_topic_path(id)
        WHERE path LIKE OLD.path || '%'
        AND id != NEW.id;
    END IF;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_topic_hierarchy_update ON topics;
CREATE TRIGGER trg_topic_hierarchy_update
    AFTER UPDATE OF parent_id ON topics
    FOR EACH ROW
    EXECUTE FUNCTION update_topic_hierarchy_on_parent_change();

-- ============================================================================
-- STEP 11: Create views for common queries
-- ============================================================================

-- 11.1: View for topics with full path text
CREATE OR REPLACE VIEW topics_with_path AS
SELECT
    t.*,
    get_topic_path_text(t.id) as full_path_text,
    (SELECT COUNT(*) FROM topics WHERE parent_id = t.id AND is_active = true) as child_count
FROM topics t
WHERE t.is_active = true;

-- 11.2: View for pending content (for moderators)
CREATE OR REPLACE VIEW pending_content AS
SELECT
    'TOPIC' as content_type,
    t.id as content_id,
    t.name as title,
    NULL as question_text,
    t.creator_id,
    u.username as creator_username,
    t.validation_status,
    t.created_at
FROM topics t
LEFT JOIN users u ON t.creator_id = u.id
WHERE t.validation_status = 'PENDING' AND t.is_active = true

UNION ALL

SELECT
    'QUESTION' as content_type,
    q.id as content_id,
    NULL as title,
    q.question as question_text,
    q.creator_id,
    u.username as creator_username,
    q.validation_status,
    q.created_at
FROM quiz_questions q
LEFT JOIN users u ON q.creator_id = u.id
WHERE q.validation_status = 'PENDING' AND q.is_active = true

ORDER BY created_at ASC;

-- 11.3: View for topic statistics
CREATE OR REPLACE VIEW topic_statistics AS
SELECT
    t.id,
    t.name,
    t.slug,
    t.depth,
    t.validation_status,
    t.is_system_topic,
    t.question_count,
    (SELECT COUNT(*) FROM topics WHERE parent_id = t.id AND is_active = true) as direct_child_count,
    (SELECT COUNT(*) FROM topics WHERE path LIKE t.path || '%' AND id != t.id AND is_active = true) as total_descendant_count,
    (SELECT COUNT(*) FROM quiz_questions WHERE topic_id = t.id AND is_active = true) as actual_question_count
FROM topics t
WHERE t.is_active = true;

-- ============================================================================
-- STEP 12: Grant permissions (adjust role name as needed)
-- ============================================================================

-- ============================================================================
-- STEP 13: Add comments for documentation
-- ============================================================================

COMMENT ON TYPE validation_status IS 'Content validation workflow status: DRAFT (initial), PENDING (awaiting review), APPROVED, REJECTED, AUTO_APPROVED (AI validated)';

COMMENT ON COLUMN topics.parent_id IS 'Reference to parent topic for hierarchical structure. NULL means root topic.';
COMMENT ON COLUMN topics.path IS 'Materialized path for efficient subtree queries. Format: /1/5/23/ where numbers are topic IDs from root to current.';
COMMENT ON COLUMN topics.depth IS 'Depth level in hierarchy. 0 = root topic, 1 = first level child, etc.';
COMMENT ON COLUMN topics.slug IS 'URL-friendly unique identifier. Generated from topic name and parent path.';
COMMENT ON COLUMN topics.is_system_topic IS 'TRUE for app-imported topics, FALSE for user-created topics.';
COMMENT ON COLUMN topics.validation_status IS 'Moderation status for user-created topics.';
COMMENT ON COLUMN topics.validated_by IS 'User ID of moderator who validated this topic.';
COMMENT ON COLUMN topics.validated_at IS 'Timestamp when topic was validated.';
COMMENT ON COLUMN topics.rejection_reason IS 'Explanation provided when topic is rejected.';

COMMENT ON COLUMN quiz_questions.validation_status IS 'Moderation status for user-created questions.';
COMMENT ON COLUMN quiz_questions.validated_by IS 'User ID of moderator who validated this question.';
COMMENT ON COLUMN quiz_questions.validated_at IS 'Timestamp when question was validated.';
COMMENT ON COLUMN quiz_questions.rejection_reason IS 'Explanation provided when question is rejected.';

COMMENT ON TABLE content_validation_history IS 'Audit trail for all content validation actions.';

COMMENT ON FUNCTION generate_topic_slug IS 'Generates a unique URL-friendly slug from topic name, optionally prefixed with parent slug.';
COMMENT ON FUNCTION build_topic_path IS 'Builds materialized path string by walking up the parent chain.';
COMMENT ON FUNCTION calculate_topic_depth IS 'Calculates depth level by counting parents up to root.';
COMMENT ON FUNCTION get_topic_path_text IS 'Returns human-readable path like "Geography > Geology > Minerals".';
COMMENT ON FUNCTION can_user_access_question_v2 IS 'Checks if user can access question based on visibility and validation status.';

COMMENT ON VIEW topics_with_path IS 'Topics with computed full path text and child count.';
COMMENT ON VIEW pending_content IS 'Union of pending topics and questions for moderator review.';
COMMENT ON VIEW topic_statistics IS 'Topic statistics including descendant counts.';

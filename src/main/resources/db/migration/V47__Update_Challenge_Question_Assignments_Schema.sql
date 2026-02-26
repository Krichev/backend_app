-- ============================================================================
-- Update Challenge-Question Assignments Schema
-- 
-- Adds assignment_type and assigned_by columns to provide context on how
-- a question was linked to a challenge.
-- ============================================================================

-- Add assignment_type column if it doesn't exist
DO $$ 
BEGIN 
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='challenge_question_assignments' AND column_name='assignment_type') THEN
        ALTER TABLE challenge_question_assignments ADD COLUMN assignment_type VARCHAR(30) NOT NULL DEFAULT 'SELECTED';
    END IF;
END $$;

-- Add assigned_by column if it doesn't exist
DO $$ 
BEGIN 
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='challenge_question_assignments' AND column_name='assigned_by') THEN
        ALTER TABLE challenge_question_assignments ADD COLUMN assigned_by BIGINT;
    END IF;
END $$;

-- Add Foreign key for assigned_by
DO $$ 
BEGIN 
    IF NOT EXISTS (SELECT 1 FROM information_schema.table_constraints WHERE constraint_name='fk_cqa_assigned_by') THEN
        ALTER TABLE challenge_question_assignments
            ADD CONSTRAINT fk_cqa_assigned_by
                FOREIGN KEY (assigned_by) REFERENCES users(id) ON DELETE SET NULL;
    END IF;
END $$;

-- Add Check constraint for assignment_type values
DO $$ 
BEGIN 
    IF NOT EXISTS (SELECT 1 FROM information_schema.table_constraints WHERE constraint_name='chk_assignment_type') THEN
        ALTER TABLE challenge_question_assignments
            ADD CONSTRAINT chk_assignment_type
                CHECK (assignment_type IN ('SELECTED', 'CREATED_INLINE', 'AUTO_ASSIGNED'));
    END IF;
END $$;

-- Add missing index
CREATE INDEX IF NOT EXISTS idx_cqa_assignment_type ON challenge_question_assignments(assignment_type);
CREATE INDEX IF NOT EXISTS idx_cqa_challenge_order ON challenge_question_assignments(challenge_id, sort_order);

-- ============================================================================
-- Migrate existing data from quiz_questions.challenge_id → junction table
-- This preserves all existing challenge-question relationships that haven't
-- been migrated yet.
-- ============================================================================

INSERT INTO challenge_question_assignments (challenge_id, question_id, assignment_type, sort_order, assigned_at, assigned_by)
SELECT 
    qq.challenge_id,
    qq.id,
    CASE 
        WHEN qq.source LIKE 'USER_CREATED_FOR_CHALLENGE_%' THEN 'CREATED_INLINE'
        ELSE 'SELECTED'
    END,
    ROW_NUMBER() OVER (PARTITION BY qq.challenge_id ORDER BY qq.id) - 1,
    COALESCE(qq.created_at, CURRENT_TIMESTAMP),
    qq.creator_id
FROM quiz_questions qq
WHERE qq.challenge_id IS NOT NULL
ON CONFLICT (challenge_id, question_id) DO UPDATE SET
    assignment_type = EXCLUDED.assignment_type,
    assigned_by = COALESCE(challenge_question_assignments.assigned_by, EXCLUDED.assigned_by)
WHERE challenge_question_assignments.assignment_type = 'SELECTED'; -- Only update if it was default

-- Update comments
COMMENT ON COLUMN challenge_question_assignments.assignment_type IS 
    'How this question was linked: SELECTED (picked from pool), CREATED_INLINE (made during creation), AUTO_ASSIGNED (system fill)';
COMMENT ON COLUMN challenge_question_assignments.assigned_by IS 
    'User who made this assignment (NULL for AUTO_ASSIGNED)';

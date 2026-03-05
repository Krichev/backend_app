-- V51__Add_Position_To_Challenge_Question_Assignments.sql
-- Add position column if it doesn't exist, using sort_order as initial value

ALTER TABLE challenge_question_assignments 
    ADD COLUMN IF NOT EXISTS position INTEGER NOT NULL DEFAULT 0;

-- Sync position from sort_order if position is still 0 and sort_order is not
UPDATE challenge_question_assignments
SET position = sort_order
WHERE position = 0 AND sort_order <> 0;

COMMENT ON COLUMN challenge_question_assignments.position IS 'Explicit ordering of questions within a challenge';

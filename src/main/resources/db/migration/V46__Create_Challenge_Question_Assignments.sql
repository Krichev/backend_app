-- ============================================================================
-- Challenge-Question Assignments (many-to-many for selected existing questions)
-- ============================================================================

CREATE TABLE IF NOT EXISTS challenge_question_assignments (
    id BIGSERIAL PRIMARY KEY,
    challenge_id BIGINT NOT NULL,
    question_id BIGINT NOT NULL,
    sort_order INTEGER DEFAULT 0,
    assigned_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Foreign keys
ALTER TABLE challenge_question_assignments
    ADD CONSTRAINT fk_cqa_challenge
        FOREIGN KEY (challenge_id) REFERENCES challenges(id) ON DELETE CASCADE;

ALTER TABLE challenge_question_assignments
    ADD CONSTRAINT fk_cqa_question
        FOREIGN KEY (question_id) REFERENCES quiz_questions(id) ON DELETE CASCADE;

-- Unique constraint: no duplicate assignments
ALTER TABLE challenge_question_assignments
    ADD CONSTRAINT uq_challenge_question UNIQUE (challenge_id, question_id);

-- Indexes
CREATE INDEX idx_cqa_challenge_id ON challenge_question_assignments(challenge_id);
CREATE INDEX idx_cqa_question_id ON challenge_question_assignments(question_id);

COMMENT ON TABLE challenge_question_assignments IS 'Many-to-many: links existing questions to challenges/quests';

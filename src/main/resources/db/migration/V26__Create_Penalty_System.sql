-- ============================================================================
-- Flyway Migration V26: Create Penalty System
-- ============================================================================
-- Description: Creates the tables and enums for the Penalty/Punishment Engine.
--
-- Version: V26
-- Author: Gemini CLI
-- Date: 2026-02-01
-- ============================================================================

-- Create ENUM types for the penalty system
CREATE TYPE penalty_type AS ENUM (
    'SCREEN_TIME_LOCK',
    'SOCIAL_TASK',
    'POINT_DEDUCTION',
    'PROFILE_CHANGE',
    'CUSTOM_QUEST'
);
COMMENT ON TYPE penalty_type IS 'Type of penalty assigned to a user';

CREATE TYPE penalty_status AS ENUM (
    'PENDING',
    'IN_PROGRESS',
    'COMPLETED',
    'VERIFIED',
    'EXPIRED',
    'APPEALED',
    'WAIVED'
);
COMMENT ON TYPE penalty_status IS 'Lifecycle status of a penalty';

CREATE TYPE penalty_verification_method AS ENUM (
    'SELF_REPORT',
    'PEER_REVIEW',
    'AI_VERIFICATION',
    'PHOTO_PROOF'
);
COMMENT ON TYPE penalty_verification_method IS 'Method used to verify penalty completion';

-- ============================================================================
-- PENALTIES TABLE
-- ============================================================================

CREATE TABLE penalties (
    id BIGSERIAL PRIMARY KEY,
    wager_id BIGINT,
    challenge_id BIGINT,
    assigned_to_user_id BIGINT NOT NULL,
    assigned_by_user_id BIGINT NOT NULL,
    penalty_type penalty_type NOT NULL,
    description VARCHAR(1000),
    status penalty_status NOT NULL DEFAULT 'PENDING',
    due_date TIMESTAMP WITH TIME ZONE,
    completed_at TIMESTAMP WITH TIME ZONE,
    verification_method penalty_verification_method NOT NULL,
    verified_by_user_id BIGINT,
    verified_at TIMESTAMP WITH TIME ZONE,
    proof_media_id BIGINT,
    proof_description TEXT,
    screen_time_minutes INTEGER,
    point_amount BIGINT,
    appeal_reason TEXT,
    appealed_at TIMESTAMP WITH TIME ZONE,
    escalation_applied BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT fk_penalties_wager FOREIGN KEY (wager_id) REFERENCES wagers(id) ON DELETE SET NULL,
    CONSTRAINT fk_penalties_challenge FOREIGN KEY (challenge_id) REFERENCES challenges(id) ON DELETE SET NULL,
    CONSTRAINT fk_penalties_assigned_to FOREIGN KEY (assigned_to_user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_penalties_assigned_by FOREIGN KEY (assigned_by_user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_penalties_verified_by FOREIGN KEY (verified_by_user_id) REFERENCES users(id) ON DELETE SET NULL,
    CONSTRAINT fk_penalties_proof_media FOREIGN KEY (proof_media_id) REFERENCES media_files(id) ON DELETE SET NULL
);

COMMENT ON TABLE penalties IS 'Penalties assigned to users (e.g. from losing wagers)';
CREATE INDEX idx_penalties_assigned_to ON penalties(assigned_to_user_id);
CREATE INDEX idx_penalties_status ON penalties(status);
CREATE INDEX idx_penalties_wager_id ON penalties(wager_id);
CREATE INDEX idx_penalties_due_date ON penalties(due_date);

-- ============================================================================
-- PENALTY_PROOFS TABLE
-- ============================================================================

CREATE TABLE penalty_proofs (
    id BIGSERIAL PRIMARY KEY,
    penalty_id BIGINT NOT NULL,
    submitted_by_user_id BIGINT NOT NULL,
    media_file_id BIGINT,
    text_proof TEXT,
    submitted_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    reviewed_at TIMESTAMP WITH TIME ZONE,
    reviewed_by_user_id BIGINT,
    approved BOOLEAN,
    review_notes TEXT,
    
    CONSTRAINT fk_penalty_proofs_penalty FOREIGN KEY (penalty_id) REFERENCES penalties(id) ON DELETE CASCADE,
    CONSTRAINT fk_penalty_proofs_submitted_by FOREIGN KEY (submitted_by_user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_penalty_proofs_media_file FOREIGN KEY (media_file_id) REFERENCES media_files(id) ON DELETE SET NULL,
    CONSTRAINT fk_penalty_proofs_reviewed_by FOREIGN KEY (reviewed_by_user_id) REFERENCES users(id) ON DELETE SET NULL
);

COMMENT ON TABLE penalty_proofs IS 'Proof submissions for penalties';
CREATE INDEX idx_penalty_proofs_penalty_id ON penalty_proofs(penalty_id);

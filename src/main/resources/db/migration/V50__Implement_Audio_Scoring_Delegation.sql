-- Migration: Implement Audio Scoring Delegation Table Structure
-- Renaming and updating audio_challenge_submissions to audio_submissions as per doc.md

-- 1. Rename the table
ALTER TABLE audio_challenge_submissions RENAME TO audio_submissions;

-- 2. Rename existing columns to match doc.md
ALTER TABLE audio_submissions RENAME COLUMN submission_audio_path TO user_audio_s3_key;

-- 3. Add new columns required by doc.md
ALTER TABLE audio_submissions 
    ADD COLUMN user_audio_bucket VARCHAR(100),
    ADD COLUMN error_message VARCHAR(1000),
    ADD COLUMN passed BOOLEAN,
    ADD COLUMN minimum_score_required INTEGER,
    ADD COLUMN challenge_type VARCHAR(30),
    ADD COLUMN updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP;

-- 4. Update data for existing rows (if any)
UPDATE audio_submissions s
SET challenge_type = q.audio_challenge_type,
    minimum_score_required = q.minimum_score_percentage,
    user_audio_bucket = (SELECT bucket_name FROM media_files WHERE id = s.submission_media_id),
    passed = (s.overall_score >= q.minimum_score_percentage)
FROM quiz_questions q
WHERE s.question_id = q.id;

-- 5. Set constraints and defaults for new columns
ALTER TABLE audio_submissions ALTER COLUMN challenge_type SET NOT NULL;
ALTER TABLE audio_submissions ALTER COLUMN user_audio_bucket SET NOT NULL;
ALTER TABLE audio_submissions ALTER COLUMN updated_at SET NOT NULL;

-- 6. Update indexes
DROP INDEX IF EXISTS idx_audio_submissions_question;
DROP INDEX IF EXISTS idx_audio_submissions_user;
DROP INDEX IF EXISTS idx_audio_submissions_status;

CREATE INDEX idx_audio_submissions_question_user ON audio_submissions(question_id, user_id);
CREATE INDEX idx_audio_submissions_user ON audio_submissions(user_id);
CREATE INDEX idx_audio_submissions_status ON audio_submissions(processing_status);

-- 7. Add comments
COMMENT ON TABLE audio_submissions IS 'User submissions for audio-based challenges with scoring results';
COMMENT ON COLUMN audio_submissions.user_audio_s3_key IS 'S3 key for the user recording';
COMMENT ON COLUMN audio_submissions.user_audio_bucket IS 'S3 bucket for the user recording';
COMMENT ON COLUMN audio_submissions.challenge_type IS 'Denormalized challenge type for scoring context';
COMMENT ON COLUMN audio_submissions.passed IS 'Whether the submission met the minimum score requirement';

-- Add submission_media_id to audio_challenge_submissions
ALTER TABLE audio_challenge_submissions 
ADD COLUMN IF NOT EXISTS submission_media_id BIGINT;

-- Add foreign key constraint
ALTER TABLE audio_challenge_submissions
ADD CONSTRAINT fk_submission_media 
FOREIGN KEY (submission_media_id) REFERENCES media_files(id);

-- Add index for performance
CREATE INDEX IF NOT EXISTS idx_audio_submissions_media_id 
ON audio_challenge_submissions(submission_media_id);

COMMENT ON COLUMN audio_challenge_submissions.submission_media_id IS 
'Reference to the stored audio file in media_files table';
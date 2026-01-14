-- Add audio configuration fields to challenges table (mirrors quest audio config)

ALTER TABLE challenges 
    ADD COLUMN IF NOT EXISTS audio_media_id BIGINT REFERENCES media_files(id) ON DELETE SET NULL;

ALTER TABLE challenges 
    ADD COLUMN IF NOT EXISTS audio_start_time DOUBLE PRECISION DEFAULT 0.0;

ALTER TABLE challenges 
    ADD COLUMN IF NOT EXISTS audio_end_time DOUBLE PRECISION;

ALTER TABLE challenges 
    ADD COLUMN IF NOT EXISTS minimum_score_percentage INTEGER DEFAULT 0;

-- Create index for audio media lookups
CREATE INDEX IF NOT EXISTS idx_challenges_audio_media_id ON challenges(audio_media_id);

COMMENT ON COLUMN challenges.audio_media_id IS 'Reference to audio file for audio-based challenges';
COMMENT ON COLUMN challenges.audio_start_time IS 'Audio segment start time in seconds';
COMMENT ON COLUMN challenges.audio_end_time IS 'Audio segment end time in seconds (null = full duration)';
COMMENT ON COLUMN challenges.minimum_score_percentage IS 'Minimum score percentage required to pass (0-100)';
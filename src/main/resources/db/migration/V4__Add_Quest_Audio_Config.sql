-- Migration: Add Quest Audio Configuration
-- Description: Adds audio track support to quests with segment playback and minimum score requirement
-- Author: Claude Code
-- Date: 2025-12-16

-- Add audio configuration columns to quests table
ALTER TABLE quests
ADD COLUMN audio_media_id BIGINT,
ADD COLUMN audio_start_time DOUBLE PRECISION DEFAULT 0,
ADD COLUMN audio_end_time DOUBLE PRECISION,
ADD COLUMN minimum_score_percentage INTEGER DEFAULT 0;

-- Add foreign key constraint to media_files
ALTER TABLE quests
ADD CONSTRAINT fk_quests_audio_media
FOREIGN KEY (audio_media_id) REFERENCES media_files(id) ON DELETE SET NULL;

-- Add check constraint for score percentage (0-100)
ALTER TABLE quests
ADD CONSTRAINT check_minimum_score_percentage
CHECK (minimum_score_percentage >= 0 AND minimum_score_percentage <= 100);

-- Add partial index for audio media lookups (only index rows with audio)
CREATE INDEX idx_quests_audio_media_id ON quests(audio_media_id)
WHERE audio_media_id IS NOT NULL;

-- Add comment for documentation
COMMENT ON COLUMN quests.audio_media_id IS 'Reference to audio file in media_files table';
COMMENT ON COLUMN quests.audio_start_time IS 'Audio segment start time in seconds (default: 0)';
COMMENT ON COLUMN quests.audio_end_time IS 'Audio segment end time in seconds (NULL means full duration)';
COMMENT ON COLUMN quests.minimum_score_percentage IS 'Minimum score percentage (0-100) required to complete quest';

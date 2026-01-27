-- Create media_source_type enum
CREATE TYPE media_source_type AS ENUM (
    'UPLOADED',
    'EXTERNAL_URL', 
    'YOUTUBE',
    'VIMEO',
    'SOUNDCLOUD'
);

-- Add columns to quiz_questions table
ALTER TABLE quiz_questions 
ADD COLUMN IF NOT EXISTS media_source_type media_source_type DEFAULT 'UPLOADED',
ADD COLUMN IF NOT EXISTS external_media_url VARCHAR(1000),
ADD COLUMN IF NOT EXISTS external_media_id VARCHAR(100),
ADD COLUMN IF NOT EXISTS question_video_start_time DOUBLE PRECISION,
ADD COLUMN IF NOT EXISTS question_video_end_time DOUBLE PRECISION,
ADD COLUMN IF NOT EXISTS answer_media_url VARCHAR(1000),
ADD COLUMN IF NOT EXISTS answer_external_media_id VARCHAR(100),
ADD COLUMN IF NOT EXISTS answer_video_start_time DOUBLE PRECISION,
ADD COLUMN IF NOT EXISTS answer_video_end_time DOUBLE PRECISION,
ADD COLUMN IF NOT EXISTS answer_text_verification TEXT;

-- Add index for external media queries
CREATE INDEX IF NOT EXISTS idx_quiz_questions_media_source ON quiz_questions(media_source_type);

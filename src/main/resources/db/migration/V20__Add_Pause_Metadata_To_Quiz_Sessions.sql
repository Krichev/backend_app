-- V20__Add_Pause_Metadata_To_Quiz_Sessions.sql
-- Add metadata columns for pausing quiz sessions

ALTER TABLE quiz_sessions
ADD COLUMN paused_at TIMESTAMP WITH TIME ZONE,
ADD COLUMN paused_at_round INTEGER,
ADD COLUMN remaining_time_seconds INTEGER,
ADD COLUMN paused_answer TEXT,
ADD COLUMN paused_notes TEXT;
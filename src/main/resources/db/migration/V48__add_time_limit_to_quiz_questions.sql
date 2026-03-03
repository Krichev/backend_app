ALTER TABLE quiz_questions ADD COLUMN time_limit_seconds INTEGER;
COMMENT ON COLUMN quiz_questions.time_limit_seconds IS 'Per-question time limit in seconds. NULL means use session default roundTimeSeconds.';

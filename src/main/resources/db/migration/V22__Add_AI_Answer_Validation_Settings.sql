-- Add AI answer validation toggle to quiz_sessions
ALTER TABLE quiz_sessions 
ADD COLUMN IF NOT EXISTS enable_ai_answer_validation BOOLEAN DEFAULT FALSE;

COMMENT ON COLUMN quiz_sessions.enable_ai_answer_validation IS 
'When true, uses AI (DeepSeek) to accept semantically equivalent answers (synonyms, paraphrases)';

-- Add AI validation tracking to quiz_rounds
ALTER TABLE quiz_rounds
ADD COLUMN IF NOT EXISTS ai_validation_used BOOLEAN DEFAULT FALSE;

ALTER TABLE quiz_rounds
ADD COLUMN IF NOT EXISTS ai_accepted BOOLEAN DEFAULT FALSE;

ALTER TABLE quiz_rounds
ADD COLUMN IF NOT EXISTS ai_confidence DECIMAL(3,2);

ALTER TABLE quiz_rounds
ADD COLUMN IF NOT EXISTS ai_explanation VARCHAR(500);

COMMENT ON COLUMN quiz_rounds.ai_validation_used IS 'Whether AI was used for answer validation in this round';
COMMENT ON COLUMN quiz_rounds.ai_accepted IS 'Whether AI accepted the answer as semantically equivalent';
COMMENT ON COLUMN quiz_rounds.ai_confidence IS 'AI confidence score for the validation (0.00-1.00)';
COMMENT ON COLUMN quiz_rounds.ai_explanation IS 'AI explanation for why the answer was accepted/rejected';

-- Add default preference to user_app_settings
ALTER TABLE user_app_settings
ADD COLUMN IF NOT EXISTS enable_ai_answer_validation BOOLEAN DEFAULT FALSE;

COMMENT ON COLUMN user_app_settings.enable_ai_answer_validation IS 
'User default preference for AI-powered vague answer acceptance in quizzes';

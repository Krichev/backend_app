-- ============================================================================
-- Flyway Migration V27: Create Screen Time Budget System
-- ============================================================================
-- Description: Creates the screen_time_budgets table for tracking daily
--              screen time allowances, locks, and wager-based time transfers.
-- Version: V27
-- Date: 2024-02-04
-- ============================================================================

CREATE TABLE screen_time_budgets (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE,
    daily_budget_minutes INTEGER NOT NULL DEFAULT 180,
    available_minutes INTEGER NOT NULL DEFAULT 180,
    locked_minutes INTEGER NOT NULL DEFAULT 0,
    lost_today_minutes INTEGER NOT NULL DEFAULT 0,
    won_today_minutes INTEGER NOT NULL DEFAULT 0,
    total_lost_minutes BIGINT NOT NULL DEFAULT 0,
    total_won_minutes BIGINT NOT NULL DEFAULT 0,
    last_reset_date DATE NOT NULL DEFAULT CURRENT_DATE,
    last_activity_at TIMESTAMP WITH TIME ZONE,
    timezone VARCHAR(50) NOT NULL DEFAULT 'UTC',
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0,
    
    CONSTRAINT fk_screen_time_user FOREIGN KEY (user_id) 
        REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT chk_daily_budget_range 
        CHECK (daily_budget_minutes BETWEEN 30 AND 480),
    CONSTRAINT chk_available_not_negative 
        CHECK (available_minutes >= 0),
    CONSTRAINT chk_locked_not_negative 
        CHECK (locked_minutes >= 0)
);

COMMENT ON TABLE screen_time_budgets IS 'Tracks daily screen time budgets and usage for users';
COMMENT ON COLUMN screen_time_budgets.available_minutes IS 'Remaining usable minutes for today';
COMMENT ON COLUMN screen_time_budgets.locked_minutes IS 'Minutes locked due to active penalties';
COMMENT ON COLUMN screen_time_budgets.lost_today_minutes IS 'Minutes lost from wagers today (resets daily)';
COMMENT ON COLUMN screen_time_budgets.won_today_minutes IS 'Minutes won from wagers today (resets daily)';

CREATE INDEX idx_screen_time_user_id ON screen_time_budgets(user_id);
CREATE INDEX idx_screen_time_last_reset ON screen_time_budgets(last_reset_date);

-- Trigger for updated_at
DROP TRIGGER IF EXISTS update_screen_time_budgets_updated_at ON screen_time_budgets;
CREATE TRIGGER update_screen_time_budgets_updated_at
    BEFORE UPDATE ON screen_time_budgets
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

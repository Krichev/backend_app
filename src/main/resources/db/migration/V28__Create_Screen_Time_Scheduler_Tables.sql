-- ============================================================================
-- Flyway Migration V28: Screen Time Scheduler Support Tables
-- ============================================================================
-- Description: Creates tables for distributed locking and reset audit logs
-- ============================================================================

-- ShedLock table for distributed scheduling
CREATE TABLE IF NOT EXISTS shedlock (
    name VARCHAR(64) PRIMARY KEY,
    lock_until TIMESTAMP WITH TIME ZONE NOT NULL,
    locked_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    locked_by VARCHAR(255) NOT NULL
);

COMMENT ON TABLE shedlock IS 'Distributed lock table for ShedLock scheduler coordination';

-- Screen time reset audit log
CREATE TABLE screen_time_reset_logs (
    id BIGSERIAL PRIMARY KEY,
    reset_date DATE NOT NULL,
    timezone VARCHAR(50),
    users_processed INTEGER NOT NULL DEFAULT 0,
    users_reset INTEGER NOT NULL DEFAULT 0,
    users_skipped INTEGER NOT NULL DEFAULT 0,
    users_failed INTEGER NOT NULL DEFAULT 0,
    started_at TIMESTAMP WITH TIME ZONE NOT NULL,
    completed_at TIMESTAMP WITH TIME ZONE,
    duration_ms BIGINT,
    status VARCHAR(20) NOT NULL DEFAULT 'IN_PROGRESS',
    error_message TEXT,
    instance_id VARCHAR(100),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT chk_reset_log_status CHECK (status IN ('IN_PROGRESS', 'SUCCESS', 'PARTIAL', 'FAILED'))
);

COMMENT ON TABLE screen_time_reset_logs IS 'Audit log for daily screen time budget resets';

CREATE INDEX idx_reset_logs_date ON screen_time_reset_logs(reset_date);
CREATE INDEX idx_reset_logs_status ON screen_time_reset_logs(status);
CREATE INDEX idx_reset_logs_timezone ON screen_time_reset_logs(timezone);

-- Add index on screen_time_budgets for timezone queries
CREATE INDEX IF NOT EXISTS idx_screen_time_timezone 
    ON screen_time_budgets(timezone, last_reset_date);

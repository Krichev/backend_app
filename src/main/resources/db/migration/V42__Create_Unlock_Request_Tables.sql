-- Unlock request types
DO $$ BEGIN
    CREATE TYPE unlock_type AS ENUM ('PERMISSION_REQUEST', 'PENALTY_PAYMENT', 'EMERGENCY_BYPASS');
EXCEPTION
    WHEN duplicate_object THEN null;
END $$;

DO $$ BEGIN
    CREATE TYPE unlock_request_status AS ENUM ('PENDING', 'APPROVED', 'DENIED', 'EXPIRED', 'CANCELLED', 'AUTO_APPROVED');
EXCEPTION
    WHEN duplicate_object THEN null;
END $$;

CREATE TABLE unlock_requests (
    id BIGSERIAL PRIMARY KEY,
    requester_id BIGINT NOT NULL,
    approver_id BIGINT, -- NULL for emergency bypass, penalty assigner for wagers, parent for children
    penalty_id BIGINT, -- Links to the penalty that caused the lock
    unlock_type unlock_type NOT NULL,
    status unlock_request_status NOT NULL DEFAULT 'PENDING',
    
    -- For PENALTY_PAYMENT
    payment_type VARCHAR(30), -- 'POINTS', 'SCREEN_TIME', 'SUBSTITUTE_QUEST'
    payment_amount INTEGER, -- Points or minutes to deduct
    payment_fulfilled BOOLEAN DEFAULT FALSE,
    
    -- For EMERGENCY_BYPASS  
    bypass_number INTEGER, -- Which bypass this month (1, 2, 3...)
    
    -- Request details
    reason TEXT, -- User's reason for requesting unlock
    approver_message TEXT, -- Response message from approver
    
    -- Timestamps
    requested_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    responded_at TIMESTAMP WITH TIME ZONE,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL, -- Auto-expire after configurable time
    
    -- Audit
    device_info JSONB, -- Device model, OS version, lock duration at time of request
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT fk_unlock_requester FOREIGN KEY (requester_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_unlock_approver FOREIGN KEY (approver_id) REFERENCES users(id) ON DELETE SET NULL,
    CONSTRAINT fk_unlock_penalty FOREIGN KEY (penalty_id) REFERENCES penalties(id) ON DELETE SET NULL
);

CREATE INDEX idx_unlock_requester ON unlock_requests(requester_id, status);
CREATE INDEX idx_unlock_approver ON unlock_requests(approver_id, status);
CREATE INDEX idx_unlock_expires ON unlock_requests(expires_at) WHERE status = 'PENDING';
CREATE INDEX idx_unlock_penalty ON unlock_requests(penalty_id);

-- Account lock configuration
CREATE TABLE account_lock_configs (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE,
    configured_by BIGINT, -- NULL = self, user_id = parent/admin who set it
    
    allow_self_unlock BOOLEAN NOT NULL DEFAULT TRUE,
    allow_emergency_bypass BOOLEAN NOT NULL DEFAULT TRUE,
    max_emergency_bypasses_per_month INTEGER NOT NULL DEFAULT 3,
    unlock_penalty_multiplier NUMERIC(4,2) NOT NULL DEFAULT 2.00,
    require_approval_from BIGINT, -- Specific userId, NULL = penalty assigner
    escalation_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    escalation_after_attempts INTEGER NOT NULL DEFAULT 3,
    
    -- Monthly tracking
    emergency_bypasses_used_this_month INTEGER NOT NULL DEFAULT 0,
    bypass_month_reset_date DATE NOT NULL DEFAULT CURRENT_DATE,
    
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT fk_lock_config_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_lock_config_configured_by FOREIGN KEY (configured_by) REFERENCES users(id) ON DELETE SET NULL,
    CONSTRAINT fk_lock_config_approval FOREIGN KEY (require_approval_from) REFERENCES users(id) ON DELETE SET NULL,
    CONSTRAINT chk_bypass_limit CHECK (max_emergency_bypasses_per_month BETWEEN 0 AND 10),
    CONSTRAINT chk_multiplier CHECK (unlock_penalty_multiplier BETWEEN 1.0 AND 10.0),
    CONSTRAINT chk_escalation_attempts CHECK (escalation_after_attempts BETWEEN 1 AND 10)
);

CREATE INDEX idx_lock_config_user ON account_lock_configs(user_id);

-- Add update_updated_at_column if not exists (it should exist based on standard patterns)
-- But let's be safe and use a trigger if we know the function name.
-- V6 created it.

CREATE TRIGGER update_unlock_requests_updated_at
    BEFORE UPDATE ON unlock_requests
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_account_lock_configs_updated_at
    BEFORE UPDATE ON account_lock_configs
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- ============================================================================
-- Flyway Migration V29: Parental Controls System
-- ============================================================================

-- Parental links between parent and child accounts
CREATE TABLE parental_links (
    id BIGSERIAL PRIMARY KEY,
    parent_user_id BIGINT NOT NULL,
    child_user_id BIGINT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    verification_code VARCHAR(10),
    verified_at TIMESTAMP WITH TIME ZONE,
    permissions JSONB DEFAULT '{"canSetBudget": true, "canViewActivity": true, "canApproveWagers": true, "canGrantTime": true}',
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT fk_parental_parent FOREIGN KEY (parent_user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_parental_child FOREIGN KEY (child_user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT uk_parental_link UNIQUE (parent_user_id, child_user_id),
    CONSTRAINT chk_parental_status CHECK (status IN ('PENDING', 'ACTIVE', 'REVOKED')),
    CONSTRAINT chk_not_self_parent CHECK (parent_user_id != child_user_id)
);

CREATE INDEX idx_parental_parent ON parental_links(parent_user_id);
CREATE INDEX idx_parental_child ON parental_links(child_user_id);
CREATE INDEX idx_parental_status ON parental_links(status);

-- Child settings managed by parent
CREATE TABLE child_settings (
    id BIGSERIAL PRIMARY KEY,
    child_user_id BIGINT NOT NULL UNIQUE,
    managed_by_parent_id BIGINT NOT NULL,
    daily_budget_minutes INTEGER NOT NULL DEFAULT 180,
    max_wager_amount DECIMAL(10,2) DEFAULT 100.00,
    allow_money_wagers BOOLEAN DEFAULT FALSE,
    allow_screen_time_wagers BOOLEAN DEFAULT TRUE,
    allow_social_wagers BOOLEAN DEFAULT TRUE,
    max_extension_requests_per_day INTEGER DEFAULT 3,
    restricted_categories JSONB DEFAULT '[]',
    content_age_rating VARCHAR(10) DEFAULT 'E',
    notifications_to_parent JSONB DEFAULT '{"onLowTime": true, "onWager": true, "dailySummary": true, "onPenalty": true}',
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT fk_child_settings_child FOREIGN KEY (child_user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_child_settings_parent FOREIGN KEY (managed_by_parent_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT chk_budget_positive CHECK (daily_budget_minutes > 0),
    CONSTRAINT chk_max_wager_positive CHECK (max_wager_amount >= 0)
);

-- Parental approvals queue
CREATE TABLE parental_approvals (
    id BIGSERIAL PRIMARY KEY,
    parent_user_id BIGINT NOT NULL,
    child_user_id BIGINT NOT NULL,
    approval_type VARCHAR(30) NOT NULL,
    reference_id BIGINT,
    reference_type VARCHAR(30),
    request_details JSONB NOT NULL DEFAULT '{}',
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    parent_response JSONB,
    responded_at TIMESTAMP WITH TIME ZONE,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT fk_approval_parent FOREIGN KEY (parent_user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_approval_child FOREIGN KEY (child_user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT chk_approval_type CHECK (approval_type IN ('WAGER', 'MONEY_WAGER', 'HIGH_STAKES', 'FEATURE_ACCESS', 'CONTENT_ACCESS')),
    CONSTRAINT chk_approval_status CHECK (status IN ('PENDING', 'APPROVED', 'DENIED', 'EXPIRED'))
);

CREATE INDEX idx_approvals_parent ON parental_approvals(parent_user_id, status);
CREATE INDEX idx_approvals_child ON parental_approvals(child_user_id);
CREATE INDEX idx_approvals_expires ON parental_approvals(expires_at) WHERE status = 'PENDING';

-- Time extension requests
CREATE TABLE time_extension_requests (
    id BIGSERIAL PRIMARY KEY,
    child_user_id BIGINT NOT NULL,
    parent_user_id BIGINT NOT NULL,
    minutes_requested INTEGER NOT NULL,
    reason TEXT,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    minutes_granted INTEGER,
    parent_message TEXT,
    responded_at TIMESTAMP WITH TIME ZONE,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT fk_extension_child FOREIGN KEY (child_user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_extension_parent FOREIGN KEY (parent_user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT chk_extension_status CHECK (status IN ('PENDING', 'APPROVED', 'DENIED', 'EXPIRED')),
    CONSTRAINT chk_minutes_positive CHECK (minutes_requested > 0)
);

CREATE INDEX idx_extensions_child ON time_extension_requests(child_user_id);
CREATE INDEX idx_extensions_parent ON time_extension_requests(parent_user_id, status);

-- Add is_child flag to users table
ALTER TABLE users ADD COLUMN IF NOT EXISTS is_child_account BOOLEAN DEFAULT FALSE;
ALTER TABLE users ADD COLUMN IF NOT EXISTS birth_date DATE;

COMMENT ON TABLE parental_links IS 'Links between parent and child accounts for parental controls';
COMMENT ON TABLE child_settings IS 'Parent-managed settings for child accounts';
COMMENT ON TABLE parental_approvals IS 'Queue of items requiring parental approval';
COMMENT ON TABLE time_extension_requests IS 'Child requests for additional screen time';

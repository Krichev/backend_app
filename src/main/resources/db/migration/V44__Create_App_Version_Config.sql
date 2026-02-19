-- ============================================================================
-- Flyway Migration V44: Create App Version Configuration Table
-- ============================================================================
-- Description: Stores app version configuration for in-app update checking.
--              Admins can configure minimum supported version and force-update thresholds.
-- ============================================================================

CREATE TABLE IF NOT EXISTS app_version_config (
    id                      BIGSERIAL PRIMARY KEY,
    platform                VARCHAR(20)  NOT NULL DEFAULT 'android',
    min_supported_version   VARCHAR(50)  NOT NULL DEFAULT '1.0.0.1',
    force_update_below      VARCHAR(50),
    github_owner            VARCHAR(100) NOT NULL,
    github_repo             VARCHAR(100) NOT NULL,
    cache_ttl_minutes       INTEGER      NOT NULL DEFAULT 15,
    enabled                 BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at              TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at              TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_app_version_platform UNIQUE (platform)
);

COMMENT ON TABLE app_version_config IS 'Configuration for in-app update version checking';
COMMENT ON COLUMN app_version_config.min_supported_version IS 'Versions below this are forced to update';
COMMENT ON COLUMN app_version_config.force_update_below IS 'Optional: force update for versions below this (overrides min_supported)';
COMMENT ON COLUMN app_version_config.github_owner IS 'GitHub repository owner for release checking';
COMMENT ON COLUMN app_version_config.github_repo IS 'GitHub repository name for release checking';
COMMENT ON COLUMN app_version_config.cache_ttl_minutes IS 'How long to cache GitHub API response (default 15min)';

-- Insert default Android config — REPLACE {OWNER} and {REPO} with actual values
INSERT INTO app_version_config (platform, min_supported_version, github_owner, github_repo)
VALUES ('android', '1.0.0.1', 'Krichev', 'front_app');

-- ============================================================================
-- Flyway Migration V12: Add Enhanced Relationships
-- ============================================================================
-- Description: Expands user relationship features with more categories,
--              privacy settings, and custom groups.
-- ============================================================================

-- Expand relationship_type enum (creating a new one as per doc.txt)
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'relationship_type_v2') THEN
        CREATE TYPE relationship_type_v2 AS ENUM (
            'COLLEAGUE',
            'CLASSMATE', 
            'FRIEND',
            'CLOSE_FRIEND',
            'FAMILY_PARENT',
            'FAMILY_SIBLING',
            'FAMILY_EXTENDED',
            'PARTNER',
            'ACQUAINTANCE',
            'BLOCKED'
        );
    END IF;
END$$;

-- Add new columns to user_relationships
ALTER TABLE user_relationships 
    ADD COLUMN IF NOT EXISTS nickname VARCHAR(100),
    ADD COLUMN IF NOT EXISTS notes TEXT,
    ADD COLUMN IF NOT EXISTS is_favorite BOOLEAN DEFAULT FALSE;

-- Create user_privacy_settings table
CREATE TABLE IF NOT EXISTS user_privacy_settings (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE,
    allow_requests_from VARCHAR(50) DEFAULT 'ANYONE',
    show_connections BOOLEAN DEFAULT TRUE,
    show_mutual_connections BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_privacy_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Create contact_groups table for custom grouping
CREATE TABLE IF NOT EXISTS contact_groups (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    name VARCHAR(100) NOT NULL,
    color VARCHAR(7),
    icon VARCHAR(50),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_contact_groups_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT uk_contact_group_name UNIQUE (user_id, name)
);

-- Create contact_group_members junction table
CREATE TABLE IF NOT EXISTS contact_group_members (
    contact_group_id BIGINT NOT NULL,
    relationship_id BIGINT NOT NULL,
    added_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (contact_group_id, relationship_id),
    CONSTRAINT fk_cgm_group FOREIGN KEY (contact_group_id) REFERENCES contact_groups(id) ON DELETE CASCADE,
    CONSTRAINT fk_cgm_relationship FOREIGN KEY (relationship_id) REFERENCES user_relationships(id) ON DELETE CASCADE
);

-- Indexes
CREATE INDEX IF NOT EXISTS idx_user_relationships_favorite ON user_relationships(user_id, is_favorite) WHERE is_favorite = TRUE;
CREATE INDEX IF NOT EXISTS idx_contact_groups_user ON contact_groups(user_id);

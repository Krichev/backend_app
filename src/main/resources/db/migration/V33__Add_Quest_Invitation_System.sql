-- Create ENUMs
CREATE TYPE gender_type AS ENUM ('MALE', 'FEMALE', 'OTHER', 'PREFER_NOT_TO_SAY');
CREATE TYPE invitation_preference AS ENUM ('ANYONE', 'FRIENDS_ONLY', 'FAMILY_ONLY', 'FRIENDS_AND_FAMILY', 'NOBODY');
CREATE TYPE gender_preference AS ENUM ('ANY_GENDER', 'MALE_ONLY', 'FEMALE_ONLY');
CREATE TYPE quest_invitation_status AS ENUM ('PENDING', 'ACCEPTED', 'DECLINED', 'EXPIRED', 'NEGOTIATING', 'CANCELLED');
CREATE TYPE negotiation_status AS ENUM ('PROPOSED', 'ACCEPTED', 'REJECTED', 'EXPIRED');

-- Add gender to users table
ALTER TABLE users ADD COLUMN gender gender_type;

-- Add preferences to user_privacy_settings
ALTER TABLE user_privacy_settings ADD COLUMN quest_invitation_preference invitation_preference DEFAULT 'ANYONE';
ALTER TABLE user_privacy_settings ADD COLUMN gender_preference_for_invites gender_preference DEFAULT 'ANY_GENDER';

-- Create quest_invitations table
CREATE TABLE quest_invitations (
    id BIGSERIAL PRIMARY KEY,
    quest_id BIGINT NOT NULL,
    inviter_id BIGINT NOT NULL,
    invitee_id BIGINT NOT NULL,
    
    -- Stake details
    proposed_stake_type VARCHAR(50),
    proposed_stake_amount DECIMAL(19, 2),
    proposed_stake_currency VARCHAR(10),
    proposed_screen_time_minutes INTEGER,
    proposed_social_penalty_description TEXT,
    
    status quest_invitation_status NOT NULL DEFAULT 'PENDING',
    message TEXT,
    expires_at TIMESTAMP,
    
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    responded_at TIMESTAMP,

    CONSTRAINT fk_invitation_quest FOREIGN KEY (quest_id) REFERENCES quests(id) ON DELETE CASCADE,
    CONSTRAINT fk_invitation_inviter FOREIGN KEY (inviter_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_invitation_invitee FOREIGN KEY (invitee_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX idx_quest_invitations_invitee_status ON quest_invitations(invitee_id, status);
CREATE INDEX idx_quest_invitations_inviter_status ON quest_invitations(inviter_id, status);
CREATE INDEX idx_quest_invitations_quest ON quest_invitations(quest_id);
CREATE INDEX idx_quest_invitations_expires_at ON quest_invitations(expires_at);

-- Create invitation_negotiations table
CREATE TABLE invitation_negotiations (
    id BIGSERIAL PRIMARY KEY,
    invitation_id BIGINT NOT NULL,
    proposer_id BIGINT NOT NULL,
    
    -- Counter-stake details
    counter_stake_type VARCHAR(50),
    counter_stake_amount DECIMAL(19, 2),
    counter_stake_currency VARCHAR(10),
    counter_screen_time_minutes INTEGER,
    counter_social_penalty_description TEXT,
    
    status negotiation_status NOT NULL DEFAULT 'PROPOSED',
    message TEXT,
    
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    responded_at TIMESTAMP,
    
    CONSTRAINT fk_negotiation_invitation FOREIGN KEY (invitation_id) REFERENCES quest_invitations(id) ON DELETE CASCADE,
    CONSTRAINT fk_negotiation_proposer FOREIGN KEY (proposer_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX idx_invitation_negotiations_invitation ON invitation_negotiations(invitation_id);

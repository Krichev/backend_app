-- V52__create_tv_displays_table.sql
-- New table: tv_displays
CREATE TABLE tv_displays (
    id              BIGSERIAL PRIMARY KEY,
    pairing_code    VARCHAR(6) NOT NULL UNIQUE,
    status          VARCHAR(20) NOT NULL DEFAULT 'WAITING',  -- WAITING, CLAIMED, EXPIRED
    room_code       VARCHAR(6),
    tv_user_id      BIGINT REFERENCES users(id),
    claimed_by_user_id BIGINT REFERENCES users(id),
    token           TEXT,
    created_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    claimed_at      TIMESTAMP,
    expires_at      TIMESTAMP NOT NULL
);

CREATE INDEX idx_tv_displays_pairing_code ON tv_displays(pairing_code) WHERE status = 'WAITING';
CREATE INDEX idx_tv_displays_status ON tv_displays(status);

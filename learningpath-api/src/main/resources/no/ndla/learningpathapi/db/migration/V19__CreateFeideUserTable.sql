CREATE TABLE feide_users (
    id BIGSERIAL PRIMARY KEY,
    feide_id TEXT UNIQUE,
    document JSONB
);
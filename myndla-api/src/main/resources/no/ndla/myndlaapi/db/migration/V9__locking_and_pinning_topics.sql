ALTER TABLE topics
    ADD COLUMN locked BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE topics
    ADD COLUMN pinned BOOLEAN NOT NULL DEFAULT FALSE;


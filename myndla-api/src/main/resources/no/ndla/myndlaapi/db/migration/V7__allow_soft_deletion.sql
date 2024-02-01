ALTER TABLE topic_follows
    DROP CONSTRAINT topic_follows_user_id_fkey,
    ADD CONSTRAINT topic_follows_user_id_fkey
        FOREIGN KEY (user_id)
            REFERENCES my_ndla_users (id)
            ON DELETE CASCADE;

ALTER TABLE category_follows
    DROP CONSTRAINT category_follows_user_id_fkey,
    ADD CONSTRAINT category_follows_user_id_fkey
        FOREIGN KEY (user_id)
            REFERENCES my_ndla_users (id)
            ON DELETE CASCADE;

ALTER TABLE flags DROP CONSTRAINT flags_user_id_fkey;
ALTER TABLE posts DROP CONSTRAINT posts_owner_id_fkey;
ALTER TABLE topics DROP CONSTRAINT topics_owner_id_fkey;

ALTER TABLE flags ALTER COLUMN user_id DROP NOT NULL;
ALTER TABLE posts ALTER COLUMN owner_id DROP NOT NULL;
ALTER TABLE topics ALTER COLUMN owner_id DROP NOT NULL;

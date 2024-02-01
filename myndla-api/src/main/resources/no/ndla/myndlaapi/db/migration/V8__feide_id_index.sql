ALTER TABLE my_ndla_users
    ALTER COLUMN feide_id SET NOT NULL;

ALTER TABLE my_ndla_users
    ADD UNIQUE (feide_id);

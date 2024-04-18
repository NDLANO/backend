ALTER TABLE folder_resources
    ADD COLUMN favorited_date timestamp NOT NULL DEFAULT now()

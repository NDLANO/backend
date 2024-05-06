CREATE TABLE saved_shared_folder(
    folder_id uuid NOT NULL,
    feide_id text NOT NULL,
    CONSTRAINT folder_users_pkey PRIMARY KEY (folder_id, feide_id),
    CONSTRAINT folder_users_folder_id_fkey FOREIGN KEY (folder_id) REFERENCES folders(id) ON DELETE CASCADE
);
ALTER TABLE folder_resources
DROP CONSTRAINT folder_resource_pkey;

ALTER TABLE folder_resources
ALTER COLUMN folder_id DROP NOT NULL;

ALTER TABLE folder_resources
ADD COLUMN connection_id bigint GENERATED ALWAYS AS IDENTITY;

ALTER TABLE folder_resources
ADD CONSTRAINT folder_resources_pkey PRIMARY KEY (connection_id);

ALTER TABLE folder_resources
ADD CONSTRAINT folder_resources_folder_resource_uniq
UNIQUE (folder_id, resource_id);

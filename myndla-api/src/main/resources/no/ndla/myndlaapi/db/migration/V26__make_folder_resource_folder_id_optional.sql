ALTER TABLE folder_resources RENAME TO resource_connections;
ALTER TABLE resource_connections
DROP CONSTRAINT folder_resource_pkey;

ALTER TABLE resource_connections
ALTER COLUMN folder_id DROP NOT NULL;

ALTER TABLE resource_connections
ADD COLUMN connection_id bigint GENERATED ALWAYS AS IDENTITY;

ALTER TABLE resource_connections
ADD CONSTRAINT resource_connections_pkey PRIMARY KEY (connection_id);

ALTER TABLE resource_connections
ADD CONSTRAINT resource_connections_folder_resource_uniq 
UNIQUE NULLS NOT DISTINCT (folder_id, resource_id);

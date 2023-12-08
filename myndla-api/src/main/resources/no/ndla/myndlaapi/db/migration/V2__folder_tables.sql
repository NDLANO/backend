CREATE TABLE resources (
	id uuid NOT NULL DEFAULT gen_random_uuid(),
	feide_id text NULL,
	"path" text NULL,
	resource_type text NULL,
	created timestamp NULL,
	"document" jsonb NULL,
	CONSTRAINT resources_pkey PRIMARY KEY (id)
);

CREATE TABLE folders (
	id uuid NOT NULL DEFAULT gen_random_uuid(),
	parent_id uuid NULL,
	feide_id text NULL,
	"rank" int4 NULL,
	"name" text NOT NULL,
	status text NOT NULL,
	created timestamp NOT NULL DEFAULT now(),
	updated timestamp NOT NULL DEFAULT now(),
	shared timestamp NULL,
	description text NULL,
	CONSTRAINT folders_pkey PRIMARY KEY (id),
	CONSTRAINT fk_parent_id FOREIGN KEY (parent_id) REFERENCES folders(id)
);

CREATE INDEX folders_feide_id_idx ON folders USING btree (feide_id);
CREATE INDEX folders_parent_id_idx ON folders USING btree (parent_id);

CREATE TABLE folder_resources (
	folder_id uuid NOT NULL,
	resource_id uuid NOT NULL,
	"rank" int4 NULL,
	CONSTRAINT folder_resource_pkey PRIMARY KEY (folder_id, resource_id),
	CONSTRAINT folder_resources_folder_id_fkey FOREIGN KEY (folder_id) REFERENCES folders(id) ON DELETE CASCADE,
	CONSTRAINT folder_resources_resource_id_fkey FOREIGN KEY (resource_id) REFERENCES resources(id) ON DELETE CASCADE
);

CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

CREATE TABLE folders (
    id UUID DEFAULT uuid_generate_v4 () PRIMARY KEY,
    parent_id UUID NULL,
    feide_id TEXT,
    document JSONB,
    CONSTRAINT fk_parent_id
      FOREIGN KEY(parent_id)
	  REFERENCES folders(id)
);

CREATE TABLE resources (
    id UUID DEFAULT uuid_generate_v4 () PRIMARY KEY,
    feide_id TEXT,
    created TIMESTAMP,
    document JSONB
);

CREATE TABLE folder_resources (
    folder_id UUID REFERENCES folders(id) ON DELETE CASCADE,
    resource_id UUID REFERENCES resources(id) ON DELETE CASCADE,
    CONSTRAINT folder_resource_pkey PRIMARY KEY (folder_id, resource_id)
);
CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE TABLE folders (
    id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    parent_id UUID NULL,
    feide_id TEXT,
    document JSONB,
    CONSTRAINT fk_parent_id
      FOREIGN KEY(parent_id)
	  REFERENCES folders(id)
);

CREATE TABLE resources (
    id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    feide_id TEXT,
    path TEXT,
    resource_type TEXT,
    created TIMESTAMP,
    document JSONB
);

CREATE TABLE folder_resources (
    folder_id UUID REFERENCES folders(id) ON DELETE CASCADE,
    resource_id UUID REFERENCES resources(id) ON DELETE CASCADE,
    CONSTRAINT folder_resource_pkey PRIMARY KEY (folder_id, resource_id)
);
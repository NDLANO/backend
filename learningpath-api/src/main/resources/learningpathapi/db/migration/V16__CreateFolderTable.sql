CREATE TABLE folders (
    id BIGSERIAL PRIMARY KEY,
    parent_id BIGINT,
    feide_id TEXT,
    document JSONB
);

CREATE TABLE resources (
    id BIGSERIAL PRIMARY KEY,
    feide_id TEXT,
    created TIMESTAMP,
    document JSONB
);

CREATE TABLE folder_resources (
    folder_id BIGSERIAL REFERENCES folders(id) ON DELETE CASCADE,
    resource_id BIGSERIAL REFERENCES resources(id) ON DELETE CASCADE,
    CONSTRAINT folder_resource_pkey PRIMARY KEY (folder_id, resource_id)
);
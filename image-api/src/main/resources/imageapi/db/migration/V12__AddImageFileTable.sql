CREATE TABLE imagefiledata (
    id BIGSERIAL PRIMARY KEY,
    file_name TEXT,
    metadata JSONB,
    image_meta_id BIGSERIAL REFERENCES imagemetadata(id) ON DELETE CASCADE
);

CREATE INDEX ON imagefiledata(file_name);
CREATE INDEX ON imagefiledata(image_meta_id);

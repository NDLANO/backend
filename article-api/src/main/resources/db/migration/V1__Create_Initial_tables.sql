CREATE TABLE contentdata (
  id BIGSERIAL PRIMARY KEY,
  external_id TEXT,
  document JSONB,
  external_subject_id TEXT[]
);

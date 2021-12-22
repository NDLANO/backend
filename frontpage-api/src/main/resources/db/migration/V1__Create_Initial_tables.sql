CREATE TABLE subjectpage (
  id BIGSERIAL PRIMARY KEY,
  external_id TEXT,
  document JSONB
);

CREATE TABLE mainfrontpage (
  id BIGSERIAL PRIMARY KEY,
  document JSONB
);
